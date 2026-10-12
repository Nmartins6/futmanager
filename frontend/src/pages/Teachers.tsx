import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { User, Role, UserStatus } from '../types';
import { useAuth } from '../context/AuthContext';
import { GeneralStatusBadge } from '../components/StatusBadge';
import {
  UserCog,
  Plus,
  Mail,
  Phone,
  Power,
  PowerOff,
  Shield,
  UserCheck,
  AlertCircle
} from 'lucide-react';

export const Teachers: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  // Modal Convite/Provisionamento
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteName, setInviteName] = useState('');
  const [inviteRole, setInviteRole] = useState<Role>('PROFESSOR');
  const [invitePhone, setInvitePhone] = useState('');
  const [invitePassword, setInvitePassword] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const res = await api.getUsers('size=50');
      setUsers(res.content || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setFormError(null);

    try {
      await api.inviteUser({
        email: inviteEmail.trim().toLowerCase(),
        fullName: inviteName.trim(),
        role: inviteRole,
        phone: invitePhone.trim() || null,
        password: invitePassword.trim() || undefined,
      });
      setShowInviteModal(false);
      setInviteEmail('');
      setInviteName('');
      setInvitePhone('');
      setInvitePassword('');
      loadUsers();
    } catch (err: any) {
      setFormError(err.message || 'Erro ao provisionar usuário.');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleStatus = async (user: User) => {
    if (user.id === currentUser?.id) {
      alert('Você não pode desativar seu próprio usuário administrador.');
      return;
    }

    if (!window.confirm(`Deseja alterar a situação de ${user.fullName} para ${user.status === 'ATIVO' ? 'INATIVO' : 'ATIVO'}?`)) {
      return;
    }

    try {
      await api.toggleUserStatus(user.id);
      loadUsers();
    } catch (err: any) {
      alert(err.message || 'Erro ao alterar situação.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
            Equipe & Professores
          </h2>
          <p className="text-xs sm:text-sm text-slate-500">
            Provisionamento seguro, gestão de permissões e controle de acesso
          </p>
        </div>

        <button
          onClick={() => setShowInviteModal(true)}
          className="flex items-center gap-2 px-4 py-2.5 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-md transition-all self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" /> Convidar / Criar Usuário
        </button>
      </div>

      {loading ? (
        <div className="p-12 text-center text-slate-500">
          <div className="w-8 h-8 border-3 border-brand-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs">Carregando equipe...</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {users.map((u) => {
            const isSelf = u.id === currentUser?.id;
            const isAdminRole = u.role === 'ADMINISTRADOR';

            return (
              <div
                key={u.id}
                className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col justify-between hover:border-slate-300 transition-all"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <div className="flex items-center gap-2.5">
                      <div className={`p-2 rounded-xl text-white font-bold ${
                        isAdminRole ? 'bg-blue-600' : 'bg-emerald-600'
                      }`}>
                        {isAdminRole ? <Shield className="w-4 h-4" /> : <UserCheck className="w-4 h-4" />}
                      </div>
                      <div>
                        <h4 className="font-bold text-slate-900 text-sm">
                          {u.fullName} {isSelf && '(Você)'}
                        </h4>
                        <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full ${
                          isAdminRole ? 'bg-blue-100 text-blue-800' : 'bg-emerald-100 text-emerald-800'
                        }`}>
                          {u.role}
                        </span>
                      </div>
                    </div>
                    <GeneralStatusBadge status={u.status} />
                  </div>

                  <div className="space-y-1.5 text-xs text-slate-600 pt-2 border-t border-slate-100">
                    <p className="flex items-center gap-2 truncate">
                      <Mail className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span>{u.email}</span>
                    </p>
                    {u.phone && (
                      <p className="flex items-center gap-2">
                        <Phone className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                        <span>{u.phone}</span>
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex items-center justify-between pt-3 border-t border-slate-100 mt-4 text-xs">
                  <span className="text-[11px] text-slate-400">
                    Cadastro: {new Date(u.createdAt).toLocaleDateString('pt-BR')}
                  </span>

                  {!isSelf && (
                    <button
                      type="button"
                      onClick={() => handleToggleStatus(u)}
                      className={`flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded-lg transition-colors ${
                        u.status === 'ATIVO'
                          ? 'text-rose-700 bg-rose-50 hover:bg-rose-100'
                          : 'text-emerald-700 bg-emerald-50 hover:bg-emerald-100'
                      }`}
                    >
                      {u.status === 'ATIVO' ? (
                        <>
                          <PowerOff className="w-3.5 h-3.5" /> Desativar
                        </>
                      ) : (
                        <>
                          <Power className="w-3.5 h-3.5" /> Ativar
                        </>
                      )}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal Convidar/Provisionar Usuário */}
      {showInviteModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-md bg-white p-6 rounded-2xl shadow-xl space-y-4">
            <h3 className="font-bold text-base text-slate-900">
              Provisionar / Convidar Usuário
            </h3>

            {formError && (
              <p className="text-xs text-rose-600 bg-rose-50 p-2.5 rounded-lg border border-rose-200">
                {formError}
              </p>
            )}

            <form onSubmit={handleInvite} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Nome Completo *</label>
                <input
                  type="text"
                  required
                  value={inviteName}
                  onChange={(e) => setInviteName(e.target.value)}
                  placeholder="Ex: Carlos Eduardo de Oliveira"
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">E-mail Corporativo *</label>
                <input
                  type="email"
                  required
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  placeholder="professor@futmanager.com"
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Perfil de Acesso *</label>
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value as Role)}
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                >
                  <option value="PROFESSOR">Professor (acesso às turmas autorizadas)</option>
                  <option value="ADMINISTRADOR">Administrador (acesso irrestrito)</option>
                </select>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Telefone / WhatsApp</label>
                <input
                  type="text"
                  value={invitePhone}
                  onChange={(e) => setInvitePhone(e.target.value)}
                  placeholder="(11) 98765-4321"
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Senha Inicial de Acesso (opcional)</label>
                <input
                  type="text"
                  value={invitePassword}
                  onChange={(e) => setInvitePassword(e.target.value)}
                  placeholder="Padrão: admin123"
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowInviteModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-5 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow"
                >
                  {saving ? 'Gravando...' : 'Criar Usuário'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
