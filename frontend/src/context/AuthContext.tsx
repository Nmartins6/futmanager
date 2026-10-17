import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Role } from '../types';
import { api } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  isAdmin: boolean;
  isProfessor: boolean;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('futmanager_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem('futmanager_token');
  });
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const checkAuth = async () => {
      const storedToken = localStorage.getItem('futmanager_token');
      if (storedToken) {
        try {
          const profile = await api.getMe();
          setUser({
            id: profile.id,
            email: profile.email,
            fullName: profile.fullName,
            role: profile.role,
            status: profile.status,
            tenantId: profile.tenantId,
            tenantName: profile.tenantName,
            tenantSlug: profile.tenantSlug,
            emailVerified: profile.emailVerified,
            createdAt: new Date().toISOString(),
          });
        } catch (e) {
          console.warn('Sessão expirada ou inválida:', e);
          logout();
        }
      }
      setLoading(false);
    };

    checkAuth();

    const handleUnauthorized = () => logout();
    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const login = async (email: string, password: string) => {
    const res = await api.login(email, password);
    const loggedUser: User = {
      id: res.id,
      email: res.email,
      fullName: res.fullName,
      role: res.role,
      status: res.status,
      tenantId: res.tenantId,
      tenantName: res.tenantName,
      tenantSlug: res.tenantSlug,
      emailVerified: res.emailVerified,
      createdAt: new Date().toISOString(),
    };
    setUser(loggedUser);
    setToken(res.token);
    localStorage.setItem('futmanager_token', res.token);
    localStorage.setItem('futmanager_user', JSON.stringify(loggedUser));
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('futmanager_token');
    localStorage.removeItem('futmanager_user');
  };

  const isAdmin = user?.role === 'ADMINISTRADOR';
  const isProfessor = user?.role === 'PROFESSOR';

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAdmin, isProfessor, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider');
  }
  return context;
};
