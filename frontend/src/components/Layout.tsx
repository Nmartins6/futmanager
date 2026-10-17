import React, { useState } from 'react';
import { Navbar } from './Navbar';
import { Sidebar } from './Sidebar';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { MailWarning, CheckCircle2, RefreshCw } from 'lucide-react';

interface LayoutProps {
  children: React.ReactNode;
  activePage: string;
  pageTitle: string;
  onNavigate: (page: string) => void;
}

export const Layout: React.FC<LayoutProps> = ({
  children,
  activePage,
  pageTitle,
  onNavigate,
}) => {
  const { user } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [resending, setResending] = useState(false);
  const [resendFeedback, setResendFeedback] = useState<string | null>(null);
  const [resendError, setResendError] = useState<string | null>(null);

  const handleResendVerification = async () => {
    if (!user?.email || resending) return;
    setResending(true);
    setResendFeedback(null);
    setResendError(null);
    try {
      const res = await api.resendVerification(user.email);
      setResendFeedback(res.message || 'Novo link de confirmação enviado para o seu e-mail!');
    } catch (err: any) {
      setResendError(err.message || 'Aguarde alguns instantes antes de reenviar.');
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        activePage={activePage}
        onNavigate={onNavigate}
      />

      <div className="flex flex-col flex-1 min-w-0">
        <Navbar
          onToggleSidebar={() => setSidebarOpen(!sidebarOpen)}
          pageTitle={pageTitle}
        />

        {user && user.emailVerified === false && (
          <div className="bg-amber-50 border-b border-amber-200 px-4 py-2.5 sm:px-6">
            <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs text-amber-900">
              <div className="flex items-center gap-2">
                <MailWarning className="w-4 h-4 text-amber-600 shrink-0" />
                <span>
                  Seu endereço de e-mail (<strong>{user.email}</strong>) ainda não foi confirmado. Verifique sua caixa de entrada.
                </span>
              </div>
              <div className="flex items-center gap-2 self-end sm:self-auto">
                {resendFeedback ? (
                  <span className="inline-flex items-center gap-1 text-emerald-700 font-semibold">
                    <CheckCircle2 className="w-3.5 h-3.5" /> E-mail enviado!
                  </span>
                ) : (
                  <>
                    {resendError && (
                      <span className="text-rose-700 font-medium">{resendError}</span>
                    )}
                    <button
                      type="button"
                      onClick={handleResendVerification}
                      disabled={resending}
                      className="inline-flex items-center gap-1.5 px-3 py-1 font-bold text-amber-900 bg-amber-100 hover:bg-amber-200 border border-amber-300 rounded-lg transition-colors disabled:opacity-50"
                    >
                      <RefreshCw className={`w-3 h-3 ${resending ? 'animate-spin' : ''}`} />
                      <span>{resending ? 'Enviando...' : 'Reenviar confirmação'}</span>
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>
        )}

        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          {children}
        </main>
      </div>
    </div>
  );
};
