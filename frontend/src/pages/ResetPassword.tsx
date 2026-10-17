import React, { useState, useEffect } from 'react';
import { api, isValidEmail } from '../services/api';
import {
  KeyRound,
  AlertCircle,
  CheckCircle2,
  Eye,
  EyeOff,
  ArrowLeft,
  ShieldCheck,
  RefreshCw,
  Mail
} from 'lucide-react';

interface ResetPasswordProps {
  token: string;
  onBackToLogin: () => void;
}

export const ResetPassword: React.FC<ResetPasswordProps> = ({ token, onBackToLogin }) => {
  const [validatingToken, setValidatingToken] = useState(true);
  const [tokenValid, setTokenValid] = useState(false);
  const [maskedEmail, setMaskedEmail] = useState<string>('');
  const [tokenError, setTokenError] = useState<string | null>(null);

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [resetSuccess, setResetSuccess] = useState<string | null>(null);

  // Estado para solicitar novo link caso o token esteja expirado ou inválido
  const [resendEmail, setResendEmail] = useState('');
  const [resendLoading, setResendLoading] = useState(false);
  const [resendSuccess, setResendSuccess] = useState<string | null>(null);
  const [resendError, setResendError] = useState<string | null>(null);

  useEffect(() => {
    const checkToken = async () => {
      if (!token || !token.trim()) {
        setTokenError('Link de redefinição incompleto ou inválido. Solicite um novo link.');
        setValidatingToken(false);
        return;
      }

      setValidatingToken(true);
      setTokenError(null);
      try {
        const res = await api.validateResetToken(token.trim());
        if (res.valid) {
          setTokenValid(true);
          setMaskedEmail(res.email || '');
        } else {
          setTokenValid(false);
          setTokenError(res.message || 'Este link expirou ou já foi utilizado.');
        }
      } catch (err: any) {
        setTokenValid(false);
        setTokenError(
          err.message || 'Este link de recuperação é inválido, expirou ou já foi utilizado.'
        );
      } finally {
        setValidatingToken(false);
      }
    };

    checkToken();
  }, [token]);

  const handleResetSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const trimmedPass = newPassword.trim();
    const trimmedConfirm = confirmPassword.trim();

    if (trimmedPass.length < 6) {
      setFormError('A nova senha deve ter no mínimo 6 caracteres.');
      return;
    }

    if (trimmedPass !== trimmedConfirm) {
      setFormError('A confirmação da senha não confere com a nova senha.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await api.resetPasswordWithToken(token.trim(), trimmedPass);
      // Limpa qualquer sessão anterior local sem autenticar automaticamente
      localStorage.removeItem('futmanager_token');
      localStorage.removeItem('futmanager_user');
      setResetSuccess(
        res.message || 'Sua senha foi redefinida com sucesso! Faça login com sua nova senha.'
      );
    } catch (err: any) {
      setFormError(err.message || 'Não foi possível redefinir a senha. Solicite um novo link.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRequestNewLink = async (e: React.FormEvent) => {
    e.preventDefault();
    setResendError(null);
    setResendSuccess(null);

    if (!isValidEmail(resendEmail)) {
      setResendError('Informe um endereço de e-mail válido.');
      return;
    }

    setResendLoading(true);
    try {
      const res = await api.forgotPassword(resendEmail.trim().toLowerCase());
      setResendSuccess(
        res.message ||
          'Se o endereço informado estiver associado a uma conta, você receberá um e-mail com as instruções para redefinir sua senha.'
      );
    } catch (err: any) {
      setResendError(err.message || 'Erro ao solicitar novo link.');
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen px-4 py-12 bg-slate-900 sm:px-6 lg:px-8">
      <div className="w-full max-w-md space-y-6 bg-white p-8 sm:p-10 rounded-2xl shadow-2xl">
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-pitch-600 text-white text-3xl shadow-lg mb-3">
            ⚽
          </div>
          <h2 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Redefinir Senha
          </h2>
          <p className="mt-1 text-xs sm:text-sm text-slate-500 font-medium">
            FutManager — Segurança da Conta
          </p>
        </div>

        {validatingToken ? (
          <div className="py-10 text-center space-y-3">
            <div className="w-10 h-10 border-3 border-brand-600 border-t-transparent rounded-full animate-spin mx-auto" />
            <p className="text-sm font-semibold text-slate-700">
              Validando seu link de segurança...
            </p>
            <p className="text-xs text-slate-400">
              Verificando autenticidade e validade do token
            </p>
          </div>
        ) : resetSuccess ? (
          <div className="space-y-5">
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-start gap-3 text-emerald-900">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
              <div className="text-xs sm:text-sm space-y-1">
                <p className="font-bold">Senha Atualizada!</p>
                <p>{resetSuccess}</p>
              </div>
            </div>

            <button
              type="button"
              onClick={onBackToLogin}
              className="w-full flex items-center justify-center gap-2 px-4 py-3 text-sm font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-md transition-all"
            >
              Ir para a Tela de Login
            </button>
          </div>
        ) : !tokenValid ? (
          <div className="space-y-5">
            <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-start gap-3 text-rose-900">
              <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
              <div className="text-xs sm:text-sm space-y-1">
                <p className="font-bold">Link Expirado ou Inválido</p>
                <p>{tokenError}</p>
              </div>
            </div>

            <div className="border-t border-slate-200 pt-4 space-y-3">
              <p className="text-xs font-semibold text-slate-700">
                Deseja receber um novo link de recuperação?
              </p>

              {resendSuccess ? (
                <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                  <span>{resendSuccess}</span>
                </div>
              ) : (
                <form onSubmit={handleRequestNewLink} className="space-y-3">
                  {resendError && (
                    <div className="p-2.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-start gap-2">
                      <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                      <span>{resendError}</span>
                    </div>
                  )}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Seu E-mail de Acesso *
                    </label>
                    <input
                      type="email"
                      required
                      value={resendEmail}
                      onChange={(e) => setResendEmail(e.target.value)}
                      placeholder="exemplo@dominio.com"
                      className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={resendLoading}
                    className="w-full flex items-center justify-center gap-2 py-2.5 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow disabled:opacity-50"
                  >
                    <RefreshCw className={`w-3.5 h-3.5 ${resendLoading ? 'animate-spin' : ''}`} />
                    <span>{resendLoading ? 'Enviando...' : 'Solicitar Novo Link'}</span>
                  </button>
                </form>
              )}
            </div>

            <button
              type="button"
              onClick={onBackToLogin}
              className="w-full flex items-center justify-center gap-1.5 py-2 text-xs font-semibold text-slate-600 hover:text-slate-900"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Voltar para o Login</span>
            </button>
          </div>
        ) : (
          <form onSubmit={handleResetSubmit} className="space-y-4">
            {maskedEmail && (
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>
                  Conta vinculada: <strong className="font-mono">{maskedEmail}</strong>
                </span>
              </div>
            )}

            {formError && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{formError}</span>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Nova Senha (mínimo 6 caracteres) *
              </label>
              <div className="relative">
                <input
                  type={showNewPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-4 pr-10 py-3 text-slate-900 bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500 text-sm"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  className="absolute right-3 top-3 text-slate-400 hover:text-slate-600"
                >
                  {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Confirmar Nova Senha *
              </label>
              <div className="relative">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-4 pr-10 py-3 text-slate-900 bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500 text-sm"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-3 text-slate-400 hover:text-slate-600"
                >
                  {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full flex items-center justify-center gap-2 px-4 py-3 text-sm font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-md disabled:opacity-50 transition-all"
            >
              {submitting ? (
                <>
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>Atualizando Senha...</span>
                </>
              ) : (
                <>
                  <KeyRound className="w-4 h-4" />
                  <span>Salvar Nova Senha</span>
                </>
              )}
            </button>

            <button
              type="button"
              onClick={onBackToLogin}
              className="w-full flex items-center justify-center gap-1.5 py-2 text-xs font-semibold text-slate-500 hover:text-slate-800"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Cancelar e voltar ao login</span>
            </button>
          </form>
        )}
      </div>
    </div>
  );
};
