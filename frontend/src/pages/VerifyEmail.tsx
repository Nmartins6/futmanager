import React, { useState, useEffect } from 'react';
import { api, isValidEmail } from '../services/api';
import {
  MailCheck,
  AlertCircle,
  CheckCircle2,
  ArrowLeft,
  RefreshCw,
  LogIn
} from 'lucide-react';

interface VerifyEmailProps {
  token: string;
  onContinue: () => void;
}

export const VerifyEmail: React.FC<VerifyEmailProps> = ({ token, onContinue }) => {
  const [verifying, setVerifying] = useState(true);
  const [verifiedSuccess, setVerifiedSuccess] = useState<string | null>(null);
  const [verifyError, setVerifyError] = useState<string | null>(null);

  const [resendEmail, setResendEmail] = useState('');
  const [resendLoading, setResendLoading] = useState(false);
  const [resendSuccess, setResendSuccess] = useState<string | null>(null);
  const [resendError, setResendError] = useState<string | null>(null);

  useEffect(() => {
    const confirmEmail = async () => {
      if (!token || !token.trim()) {
        setVerifyError('Token de confirmação ausente na URL. Solicite um novo link abaixo.');
        setVerifying(false);
        return;
      }

      setVerifying(true);
      setVerifyError(null);
      try {
        const res = await api.verifyEmail(token.trim());
        setVerifiedSuccess(res.message || 'Endereço de e-mail confirmado com sucesso!');

        // Atualiza cache local caso o usuário esteja autenticado
        const savedUser = localStorage.getItem('futmanager_user');
        if (savedUser) {
          try {
            const parsed = JSON.parse(savedUser);
            parsed.emailVerified = true;
            localStorage.setItem('futmanager_user', JSON.stringify(parsed));
          } catch {
            // ignore
          }
        }
      } catch (err: any) {
        setVerifyError(
          err.message || 'Não foi possível confirmar seu e-mail. O link pode ter expirado ou já ter sido utilizado.'
        );
      } finally {
        setVerifying(false);
      }
    };

    confirmEmail();
  }, [token]);

  const handleResendVerification = async (e: React.FormEvent) => {
    e.preventDefault();
    setResendError(null);
    setResendSuccess(null);

    if (!isValidEmail(resendEmail)) {
      setResendError('Informe um endereço de e-mail válido.');
      return;
    }

    setResendLoading(true);
    try {
      const res = await api.resendVerification(resendEmail.trim().toLowerCase());
      setResendSuccess(
        res.message ||
          'Se o endereço informado estiver pendente de confirmação, um novo link de verificação foi enviado.'
      );
    } catch (err: any) {
      setResendError(err.message || 'Falha ao reenviar confirmação.');
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
            Confirmação de E-mail
          </h2>
          <p className="mt-1 text-xs sm:text-sm text-slate-500 font-medium">
            FutManager — Verificação de Conta
          </p>
        </div>

        {verifying ? (
          <div className="py-10 text-center space-y-3">
            <div className="w-10 h-10 border-3 border-emerald-600 border-t-transparent rounded-full animate-spin mx-auto" />
            <p className="text-sm font-semibold text-slate-700">
              Confirmando seu endereço de e-mail...
            </p>
          </div>
        ) : verifiedSuccess ? (
          <div className="space-y-5">
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-start gap-3 text-emerald-900">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
              <div className="text-xs sm:text-sm space-y-1">
                <p className="font-bold">E-mail Verificado!</p>
                <p>{verifiedSuccess}</p>
              </div>
            </div>

            <button
              type="button"
              onClick={onContinue}
              className="w-full flex items-center justify-center gap-2 px-4 py-3 text-sm font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl shadow-md transition-all"
            >
              <LogIn className="w-4 h-4" />
              <span>Continuar para o FutManager</span>
            </button>
          </div>
        ) : (
          <div className="space-y-5">
            <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-start gap-3 text-rose-900">
              <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
              <div className="text-xs sm:text-sm space-y-1">
                <p className="font-bold">Falha na Verificação</p>
                <p>{verifyError}</p>
              </div>
            </div>

            <div className="border-t border-slate-200 pt-4 space-y-3">
              <p className="text-xs font-semibold text-slate-700">
                Solicitar um novo link de confirmação:
              </p>

              {resendSuccess ? (
                <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 flex items-start gap-2">
                  <MailCheck className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                  <span>{resendSuccess}</span>
                </div>
              ) : (
                <form onSubmit={handleResendVerification} className="space-y-3">
                  {resendError && (
                    <div className="p-2.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-start gap-2">
                      <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                      <span>{resendError}</span>
                    </div>
                  )}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Seu E-mail Cadastrado *
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
                    <span>{resendLoading ? 'Enviando...' : 'Reenviar Confirmação de E-mail'}</span>
                  </button>
                </form>
              )}
            </div>

            <button
              type="button"
              onClick={onContinue}
              className="w-full flex items-center justify-center gap-1.5 py-2 text-xs font-semibold text-slate-600 hover:text-slate-900"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Voltar ao Sistema</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
