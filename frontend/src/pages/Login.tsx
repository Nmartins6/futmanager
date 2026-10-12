import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { LogIn, KeyRound, AlertCircle, CheckCircle2, Sparkles, X, Eye, EyeOff } from 'lucide-react';

export const Login: React.FC = () => {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Modal de recuperação / redefinição de senha
  const [showRecoveryModal, setShowRecoveryModal] = useState(false);
  const [recoveryEmail, setRecoveryEmail] = useState('');
  const [recoveryNewPassword, setRecoveryNewPassword] = useState('');
  const [recoveryLoading, setRecoveryLoading] = useState(false);
  const [recoverySuccess, setRecoverySuccess] = useState<string | null>(null);
  const [recoveryError, setRecoveryError] = useState<string | null>(null);

  // Modal de Onboarding SaaS (Criar Nova Escolinha)
  const [showOnboardModal, setShowOnboardModal] = useState(false);
  const [onboardLoading, setOnboardLoading] = useState(false);
  const [onboardError, setOnboardError] = useState<string | null>(null);
  const [onboardData, setOnboardData] = useState({
    schoolName: '',
    adminFullName: '',
    adminEmail: '',
    adminPassword: '',
    phone: '',
    cnpj: '',
    address: '',
    plan: 'PRO',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) return;

    setLoading(true);
    setError(null);

    try {
      await login(email, password);
    } catch (err: any) {
      setError(err.message || 'Falha ao autenticar. Verifique seu e-mail e senha.');
    } finally {
      setLoading(false);
    }
  };

  const handleRecoverPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recoveryEmail || !recoveryNewPassword) return;

    setRecoveryLoading(true);
    setRecoveryError(null);
    try {
      const res = await api.resetPassword(recoveryEmail.trim().toLowerCase(), recoveryNewPassword.trim());
      setRecoverySuccess(res.message);
    } catch (err: any) {
      setRecoveryError(err.message || 'Falha ao redefinir senha.');
    } finally {
      setRecoveryLoading(false);
    }
  };

  const handleOnboardSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!onboardData.schoolName || !onboardData.adminFullName || !onboardData.adminEmail) {
      setOnboardError('Preencha os campos obrigatórios.');
      return;
    }

    setOnboardLoading(true);
    setOnboardError(null);

    try {
      const res = await api.onboardSchool({
        schoolName: onboardData.schoolName.trim(),
        adminFullName: onboardData.adminFullName.trim(),
        adminEmail: onboardData.adminEmail.trim().toLowerCase(),
        adminPassword: onboardData.adminPassword.trim() || undefined,
        phone: onboardData.phone.trim() || undefined,
        cnpj: onboardData.cnpj.trim() || undefined,
        address: onboardData.address.trim() || undefined,
        plan: onboardData.plan as any,
      });

      // Salva token e usuário e redireciona imediatamente
      localStorage.setItem('futmanager_token', res.token);
      localStorage.setItem('futmanager_user', JSON.stringify({
        id: res.adminUser.id,
        email: res.adminUser.email,
        fullName: res.adminUser.fullName,
        role: res.adminUser.role,
        status: res.adminUser.status,
        tenantId: res.tenant.id,
        tenantName: res.tenant.name,
        tenantSlug: res.tenant.slug,
        createdAt: new Date().toISOString(),
      }));

      window.location.reload();
    } catch (err: any) {
      setOnboardError(err.message || 'Falha ao cadastrar escolinha.');
    } finally {
      setOnboardLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen px-4 py-12 bg-slate-900 sm:px-6 lg:px-8">
      <div className="w-full max-w-md space-y-8 bg-white p-8 sm:p-10 rounded-2xl shadow-2xl">
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-pitch-600 text-white text-3xl shadow-lg mb-3">
            ⚽
          </div>
          <h2 className="text-2xl font-extrabold text-slate-900 tracking-tight sm:text-3xl">
            Fut<span className="text-pitch-600">Manager</span>
          </h2>
          <p className="mt-1 text-sm text-slate-500 font-medium">
            Gestão de Chamadas & Frequência
          </p>
        </div>

        {error && (
          <div className="p-3.5 text-sm text-rose-800 bg-rose-50 border border-rose-200 rounded-xl flex items-start gap-2.5">
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="email" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              E-mail de Acesso *
            </label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="exemplo@futmanager.com"
              className="w-full px-4 py-3 text-slate-900 bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white text-sm transition-all"
            />
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label htmlFor="password" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Senha de Acesso *
              </label>
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="text-xs text-slate-500 hover:text-slate-700 flex items-center gap-1"
              >
                {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                <span>{showPassword ? 'Ocultar' : 'Mostrar'}</span>
              </button>
            </div>
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full px-4 py-3 text-slate-900 bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white text-sm transition-all"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="flex items-center justify-center w-full px-4 py-3 text-sm font-semibold text-white bg-brand-600 rounded-xl hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500 transition-all shadow-md hover:shadow-lg disabled:opacity-50"
          >
            {loading ? (
              <span className="flex items-center gap-2">
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                Acessando...
              </span>
            ) : (
              <span className="flex items-center gap-2">
                <LogIn className="w-4 h-4" /> Entrar no Sistema
              </span>
            )}
          </button>
        </form>

        {/* Botão de Criação de Nova Escolinha (SaaS Onboarding) */}
        <div className="pt-2">
          <button
            type="button"
            onClick={() => setShowOnboardModal(true)}
            className="w-full flex items-center justify-center gap-2 px-4 py-3 text-sm font-bold text-pitch-900 bg-pitch-50 border-2 border-pitch-300 rounded-xl hover:bg-pitch-100 hover:border-pitch-400 transition-all shadow-sm"
          >
            <Sparkles className="w-4 h-4 text-pitch-700" />
            <span>Cadastrar Nova Escolinha (SaaS)</span>
          </button>
        </div>

        <div className="text-center pt-2">
          <button
            type="button"
            onClick={() => setShowRecoveryModal(true)}
            className="text-xs font-medium text-brand-600 hover:text-brand-800 hover:underline"
          >
            Esqueceu sua senha ou primeiro acesso?
          </button>
        </div>
      </div>

      {/* Modal de Recuperação de Senha */}
      {showRecoveryModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-sm bg-white p-6 rounded-2xl shadow-xl space-y-4">
            <div className="flex items-center gap-2 text-slate-900 font-bold text-lg">
              <KeyRound className="w-5 h-5 text-brand-600" />
              <span>Redefinição de Senha</span>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed">
              Informe seu e-mail cadastrado e defina sua nova senha de acesso.
            </p>

            {recoverySuccess ? (
              <div className="p-3 text-xs text-emerald-800 bg-emerald-50 border border-emerald-200 rounded-xl flex items-start gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <span>{recoverySuccess}</span>
              </div>
            ) : (
              <form onSubmit={handleRecoverPassword} className="space-y-3">
                {recoveryError && (
                  <div className="p-2.5 text-xs text-rose-800 bg-rose-50 border border-rose-200 rounded-xl flex items-start gap-2">
                    <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                    <span>{recoveryError}</span>
                  </div>
                )}
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">E-mail Cadastrado *</label>
                  <input
                    type="email"
                    required
                    value={recoveryEmail}
                    onChange={(e) => setRecoveryEmail(e.target.value)}
                    placeholder="Seu e-mail cadastrado"
                    className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Nova Senha (mínimo 6 caracteres) *</label>
                  <input
                    type="password"
                    required
                    minLength={6}
                    value={recoveryNewPassword}
                    onChange={(e) => setRecoveryNewPassword(e.target.value)}
                    placeholder="Digite sua nova senha"
                    className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                </div>
                <button
                  type="submit"
                  disabled={recoveryLoading}
                  className="w-full py-2.5 text-xs font-semibold text-white bg-brand-600 rounded-xl hover:bg-brand-700 disabled:opacity-50"
                >
                  {recoveryLoading ? 'Redefinindo...' : 'Definir Nova Senha'}
                </button>
              </form>
            )}

            <button
              type="button"
              onClick={() => {
                setShowRecoveryModal(false);
                setRecoverySuccess(null);
                setRecoveryError(null);
                setRecoveryEmail('');
                setRecoveryNewPassword('');
              }}
              className="w-full py-2 text-xs font-semibold text-slate-600 bg-slate-100 rounded-xl hover:bg-slate-200"
            >
              Fechar
            </button>
          </div>
        </div>
      )}

      {/* Modal de Onboarding SaaS (Criar Nova Escolinha) */}
      {showOnboardModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-sm overflow-y-auto">
          <div className="w-full max-w-lg bg-white p-6 sm:p-8 rounded-2xl shadow-2xl space-y-5 my-8">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-pitch-600 text-white font-bold">
                  ⚽
                </div>
                <div>
                  <h3 className="text-lg font-bold text-slate-900">Cadastrar Nova Escolinha</h3>
                  <p className="text-xs text-slate-500">Crie sua conta SaaS e comece a gerenciar suas chamadas</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => {
                  setShowOnboardModal(false);
                  setOnboardError(null);
                }}
                className="p-1 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {onboardError && (
              <div className="p-3 text-xs text-rose-800 bg-rose-50 border border-rose-200 rounded-xl flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{onboardError}</span>
              </div>
            )}

            <form onSubmit={handleOnboardSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                  Nome da Escolinha *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ex: Escolinha Estrela de Ouro"
                  value={onboardData.schoolName}
                  onChange={(e) => setOnboardData({ ...onboardData, schoolName: e.target.value })}
                  className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Nome do Administrador *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: Carlos Silva"
                    value={onboardData.adminFullName}
                    onChange={(e) => setOnboardData({ ...onboardData, adminFullName: e.target.value })}
                    className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    E-mail do Administrador *
                  </label>
                  <input
                    type="email"
                    required
                    placeholder="carlos@escolinha.com"
                    value={onboardData.adminEmail}
                    onChange={(e) => setOnboardData({ ...onboardData, adminEmail: e.target.value })}
                    className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                </div>
                <div className="sm:col-span-2">
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Senha de Acesso do Administrador *
                  </label>
                  <input
                    type="password"
                    required
                    minLength={6}
                    placeholder="Defina uma senha (mínimo 6 caracteres)"
                    value={onboardData.adminPassword}
                    onChange={(e) => setOnboardData({ ...onboardData, adminPassword: e.target.value })}
                    className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Telefone / WhatsApp
                  </label>
                  <input
                    type="tel"
                    placeholder="(11) 99999-8888"
                    value={onboardData.phone}
                    onChange={(e) => setOnboardData({ ...onboardData, phone: e.target.value })}
                    className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Plano Escolhido
                  </label>
                  <select
                    value={onboardData.plan}
                    onChange={(e) => setOnboardData({ ...onboardData, plan: e.target.value })}
                    className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                  >
                    <option value="STARTER">Starter (até 50 alunos)</option>
                    <option value="PRO">Pro (até 150 alunos)</option>
                    <option value="ENTERPRISE">Enterprise (ilimitado)</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    CNPJ (Opcional)
                  </label>
                  <input
                    type="text"
                    placeholder="00.000.000/0001-00"
                    value={onboardData.cnpj}
                    onChange={(e) => setOnboardData({ ...onboardData, cnpj: e.target.value })}
                    className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Endereço / Sede (Opcional)
                  </label>
                  <input
                    type="text"
                    placeholder="Centro Esportivo, Cidade - UF"
                    value={onboardData.address}
                    onChange={(e) => setOnboardData({ ...onboardData, address: e.target.value })}
                    className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                </div>
              </div>

              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-600">
                💡 <span className="font-semibold text-slate-800">Isolamento Seguro:</span> Ao cadastrar, um banco de dados exclusivo para sua escolinha é configurado, com parâmetros próprios de chamadas, turmas e professores.
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowOnboardModal(false)}
                  className="px-4 py-2.5 text-xs font-semibold text-slate-600 bg-slate-100 rounded-xl hover:bg-slate-200"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={onboardLoading}
                  className="px-5 py-2.5 text-xs font-bold text-white bg-pitch-600 hover:bg-pitch-700 rounded-xl shadow-md disabled:opacity-50 flex items-center gap-2"
                >
                  {onboardLoading ? (
                    <>
                      <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      Criando Escolinha...
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-4 h-4" /> Criar Escolinha & Acessar
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
