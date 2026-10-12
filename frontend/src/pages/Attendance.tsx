import React, { useState, useEffect, useRef } from 'react';
import { api, ApiError } from '../services/api';
import { AttendanceDetail, AttendanceStatus, AttendanceStudentItem } from '../types';
import { AttendanceBadge } from '../components/StatusBadge';
import {
  Check,
  X,
  AlertCircle,
  Save,
  CheckCircle2,
  Clock,
  History,
  MessageSquare,
  ArrowLeft,
  Users,
  ShieldAlert,
  AlertTriangle,
  RotateCcw
} from 'lucide-react';

interface AttendancePageProps {
  lessonId: string;
  onBack: () => void;
}

export const Attendance: React.FC<AttendancePageProps> = ({ lessonId, onBack }) => {
  const [data, setData] = useState<AttendanceDetail | null>(null);
  const [items, setItems] = useState<AttendanceStudentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saveSuccessMessage, setSaveSuccessMessage] = useState<string | null>(null);
  const [isDirty, setIsDirty] = useState(false);

  // Modal de justificativa/observação
  const [justificationModal, setJustificationModal] = useState<{
    studentId: string;
    studentName: string;
    notes: string;
  } | null>(null);

  // Modal de motivo para alteração de chamada já finalizada
  const [finalizeConfirmModal, setFinalizeConfirmModal] = useState(false);
  const [changeReason, setChangeReason] = useState('');
  const [showHistoryModal, setShowHistoryModal] = useState(false);

  useEffect(() => {
    loadAttendance();
  }, [lessonId]);

  // Alerta de saída sem salvar
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (isDirty) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [isDirty]);

  const loadAttendance = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.getLessonAttendance(lessonId);
      setData(res);
      setItems(res.students);
      setIsDirty(false);
    } catch (err: any) {
      setError(err.message || 'Falha ao carregar a chamada.');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = (studentId: string, newStatus: AttendanceStatus) => {
    setItems((prev) =>
      prev.map((s) => (s.studentId === studentId ? { ...s, status: newStatus } : s))
    );
    setIsDirty(true);
    setSaveSuccessMessage(null);
  };

  const handleMarkAllPresent = () => {
    setItems((prev) => prev.map((s) => ({ ...s, status: 'PRESENTE' as AttendanceStatus })));
    setIsDirty(true);
    setSaveSuccessMessage(null);
  };

  const handleSaveDraft = async () => {
    if (!data) return;
    setSaving(true);
    setError(null);
    setSaveSuccessMessage(null);

    const payload = {
      version: data.version,
      finalizeAttendance: false,
      changeReason: data.status === 'FINALIZADA' ? (changeReason || 'Ajuste de rascunho') : undefined,
      items: items.map((s) => ({
        studentId: s.studentId,
        status: s.status || null,
        notes: s.notes || null,
      })),
    };

    try {
      const res = await api.saveLessonAttendance(lessonId, payload);
      setData(res);
      setItems(res.students);
      setIsDirty(false);
      setSaveSuccessMessage('Rascunho da chamada salvo com sucesso no servidor.');
    } catch (err: any) {
      if (err instanceof ApiError && err.status === 409) {
        setError('Conflito de concorrência: Outro usuário alterou a chamada. Recarregue para atualizar.');
      } else {
        setError(err.message || 'Erro ao salvar rascunho.');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleOpenFinalizeModal = () => {
    // Validar se todos foram classificados
    const unclassified = items.filter((s) => !s.status);
    if (unclassified.length > 0) {
      setError(
        `Atenção: Não é possível finalizar a chamada com alunos pendentes (${unclassified.length} sem classificação). Classifique todos antes de finalizar.`
      );
      return;
    }
    setError(null);
    setFinalizeConfirmModal(true);
  };

  const handleFinalizeAttendance = async () => {
    if (!data) return;
    setSaving(true);
    setError(null);

    const isAlreadyFinalized = data.status === 'FINALIZADA';
    if (isAlreadyFinalized && (!changeReason || !changeReason.trim())) {
      setError('É obrigatório informar o motivo da alteração de uma chamada já finalizada.');
      setSaving(false);
      return;
    }

    const payload = {
      version: data.version,
      finalizeAttendance: true,
      changeReason: isAlreadyFinalized ? changeReason.trim() : 'Chamada finalizada normalmente',
      items: items.map((s) => ({
        studentId: s.studentId,
        status: s.status,
        notes: s.notes || null,
      })),
    };

    try {
      const res = await api.saveLessonAttendance(lessonId, payload);
      setData(res);
      setItems(res.students);
      setIsDirty(false);
      setFinalizeConfirmModal(false);
      setChangeReason('');
      setSaveSuccessMessage('Chamada finalizada com sucesso! Presenças registradas.');
    } catch (err: any) {
      if (err instanceof ApiError && err.status === 409) {
        setError('Conflito de concorrência: A chamada foi alterada por outro professor simultaneamente.');
      } else {
        setError(err.message || 'Erro ao finalizar chamada.');
      }
    } finally {
      setSaving(false);
    }
  };

  const saveJustification = () => {
    if (!justificationModal) return;
    setItems((prev) =>
      prev.map((s) =>
        s.studentId === justificationModal.studentId
          ? { ...s, notes: justificationModal.notes }
          : s
      )
    );
    setIsDirty(true);
    setJustificationModal(null);
  };

  // Contadores dinâmicos
  const presentCount = items.filter((s) => s.status === 'PRESENTE').length;
  const absentCount = items.filter((s) => s.status === 'AUSENTE').length;
  const excusedCount = items.filter((s) => s.status === 'FALTA_JUSTIFICADA').length;
  const unclassifiedCount = items.filter((s) => !s.status).length;

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-3 text-slate-500">
          <div className="w-8 h-8 border-3 border-brand-600 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm font-medium">Carregando lista de chamada...</p>
        </div>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="space-y-4 pb-20">
      {/* Barra superior de navegação e status */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-4 sm:p-5 border border-slate-200 rounded-2xl shadow-sm">
        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              if (isDirty && !window.confirm('Existem alterações não salvas. Deseja realmente sair?')) {
                return;
              }
              onBack();
            }}
            className="p-2 text-slate-600 hover:bg-slate-100 rounded-xl transition-colors"
            title="Voltar"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base sm:text-lg font-black text-slate-900">{data.className}</h2>
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-brand-50 text-brand-700">
                {data.category}
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Data: {new Date(data.lessonDate + 'T12:00:00').toLocaleDateString('pt-BR')} • {data.startTime.substring(0, 5)} às {data.endTime.substring(0, 5)}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {data.status === 'FINALIZADA' ? (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-bold rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              Finalizada {data.finalizedByUserName ? `por ${data.finalizedByUserName.split(' ')[0]}` : ''}
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-semibold rounded-full bg-amber-100 text-amber-800 border border-amber-200">
              <Clock className="w-4 h-4 text-amber-600" />
              Rascunho em edição
            </span>
          )}

          <button
            onClick={() => setShowHistoryModal(true)}
            title="Ver histórico de auditoria"
            className="p-2 text-slate-600 hover:bg-slate-100 rounded-xl transition-colors"
          >
            <History className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Alertas e Mensagens de Feedback */}
      {saveSuccessMessage && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl flex items-center justify-between text-xs font-semibold shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{saveSuccessMessage}</span>
          </div>
          <button onClick={() => setSaveSuccessMessage(null)} className="text-emerald-700 hover:underline">
            ✕
          </button>
        </div>
      )}

      {error && (
        <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl flex items-start gap-2.5 text-xs font-medium shadow-xs">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
          <div className="flex-1">
            <span>{error}</span>
          </div>
          <button onClick={() => setError(null)} className="text-rose-700 hover:underline">
            ✕
          </button>
        </div>
      )}

      {isDirty && (
        <div className="p-2.5 bg-amber-50 border border-amber-200 text-amber-800 rounded-xl flex items-center justify-between text-xs font-semibold">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
            <span>Você possui alterações não salvas nesta chamada.</span>
          </div>
          <button
            onClick={handleSaveDraft}
            disabled={saving}
            className="px-2.5 py-1 text-xs font-bold bg-amber-600 text-white rounded-lg hover:bg-amber-700"
          >
            Salvar Rascunho
          </button>
        </div>
      )}

      {/* Resumo de Contagem Rápida e Ação em Massa */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 bg-white p-3 sm:p-4 border border-slate-200 rounded-2xl shadow-sm items-center">
        <div className="flex items-center justify-between p-2 rounded-xl bg-slate-50 border border-slate-200">
          <span className="text-xs font-medium text-slate-500">Total:</span>
          <span className="text-sm font-black text-slate-800">{items.length}</span>
        </div>
        <div className="flex items-center justify-between p-2 rounded-xl bg-emerald-50 border border-emerald-200">
          <span className="text-xs font-semibold text-emerald-700">Presentes:</span>
          <span className="text-sm font-black text-emerald-700">{presentCount}</span>
        </div>
        <div className="flex items-center justify-between p-2 rounded-xl bg-rose-50 border border-rose-200">
          <span className="text-xs font-semibold text-rose-700">Ausentes:</span>
          <span className="text-sm font-black text-rose-700">{absentCount}</span>
        </div>
        <div className="flex items-center justify-between p-2 rounded-xl bg-amber-50 border border-amber-200">
          <span className="text-xs font-semibold text-amber-700">Justificados:</span>
          <span className="text-sm font-black text-amber-700">{excusedCount}</span>
        </div>
        <div className="col-span-2 sm:col-span-1">
          <button
            type="button"
            onClick={handleMarkAllPresent}
            className="w-full py-2 px-3 text-xs font-bold rounded-xl bg-slate-100 hover:bg-emerald-50 hover:text-emerald-800 hover:border-emerald-300 border border-slate-300 text-slate-700 transition-colors shadow-xs"
          >
            ✓ Todos Presentes
          </button>
        </div>
      </div>

      {/* Lista de Alunos Elegíveis com Botões Grandes para Celular */}
      <div className="space-y-3">
        {items.map((student) => {
          const currentStatus = student.status;

          return (
            <div
              key={student.studentId}
              className={`p-4 bg-white border rounded-2xl shadow-sm transition-all ${
                currentStatus === 'PRESENTE'
                  ? 'border-emerald-300 ring-1 ring-emerald-200/50 bg-emerald-50/20'
                  : currentStatus === 'AUSENTE'
                  ? 'border-rose-300 ring-1 ring-rose-200/50 bg-rose-50/20'
                  : currentStatus === 'FALTA_JUSTIFICADA'
                  ? 'border-amber-300 ring-1 ring-amber-200/50 bg-amber-50/20'
                  : 'border-slate-200 hover:border-slate-300'
              }`}
            >
              <div className="flex items-center justify-between gap-3 mb-3">
                <div className="flex items-center gap-3 min-w-0">
                  {/* Foto ou Avatar alternativo */}
                  {student.photoUrl ? (
                    <img
                      src={student.photoUrl}
                      alt={student.studentName}
                      className="w-12 h-12 rounded-xl object-cover border border-slate-200 shadow-sm shrink-0"
                    />
                  ) : (
                    <div className="flex items-center justify-center w-12 h-12 rounded-xl bg-slate-200 text-slate-700 font-extrabold text-base shrink-0 shadow-xs">
                      {student.studentName.substring(0, 2).toUpperCase()}
                    </div>
                  )}

                  <div className="min-w-0">
                    <p className="font-bold text-slate-900 text-sm sm:text-base truncate">
                      {student.studentName}
                    </p>
                    <p className="text-xs text-slate-500">
                      {student.ageOnLessonDate} anos na data da aula
                    </p>
                  </div>
                </div>

                {/* Botão de Observação/Justificativa */}
                <button
                  type="button"
                  onClick={() =>
                    setJustificationModal({
                      studentId: student.studentId,
                      studentName: student.studentName,
                      notes: student.notes || '',
                    })
                  }
                  className={`flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold rounded-lg border transition-colors ${
                    student.notes
                      ? 'bg-amber-100 border-amber-300 text-amber-900'
                      : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'
                  }`}
                  title="Adicionar ou editar observação"
                >
                  <MessageSquare className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">
                    {student.notes ? 'Ver Obs.' : 'Obs.'}
                  </span>
                </button>
              </div>

              {student.notes && (
                <div className="mb-3 px-3 py-1.5 bg-amber-50 border border-amber-200/80 rounded-lg text-xs text-amber-900 italic">
                  "{student.notes}"
                </div>
              )}

              {/* Botões Grandes e Acessíveis (Texto + Ícone + Cor) para toque no celular */}
              <div className="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  onClick={() => handleStatusChange(student.studentId, 'PRESENTE')}
                  className={`flex flex-col sm:flex-row items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl font-bold text-xs sm:text-sm transition-all ${
                    currentStatus === 'PRESENTE'
                      ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/30 ring-2 ring-emerald-600 ring-offset-1'
                      : 'bg-slate-100 text-slate-700 hover:bg-emerald-50 hover:text-emerald-700'
                  }`}
                >
                  <Check className="w-4 h-4 shrink-0" />
                  <span>Presente</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleStatusChange(student.studentId, 'AUSENTE')}
                  className={`flex flex-col sm:flex-row items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl font-bold text-xs sm:text-sm transition-all ${
                    currentStatus === 'AUSENTE'
                      ? 'bg-rose-600 text-white shadow-md shadow-rose-600/30 ring-2 ring-rose-600 ring-offset-1'
                      : 'bg-slate-100 text-slate-700 hover:bg-rose-50 hover:text-rose-700'
                  }`}
                >
                  <X className="w-4 h-4 shrink-0" />
                  <span>Ausente</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleStatusChange(student.studentId, 'FALTA_JUSTIFICADA')}
                  className={`flex flex-col sm:flex-row items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl font-bold text-xs sm:text-sm transition-all ${
                    currentStatus === 'FALTA_JUSTIFICADA'
                      ? 'bg-amber-600 text-white shadow-md shadow-amber-600/30 ring-2 ring-amber-600 ring-offset-1'
                      : 'bg-slate-100 text-slate-700 hover:bg-amber-50 hover:text-amber-700'
                  }`}
                >
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>Justificada</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Barra de Ações Fixa no Rodapé (Ideal para uso móvel em pé no campo) */}
      <div className="fixed bottom-0 left-0 right-0 z-30 bg-white/95 backdrop-blur-md border-t border-slate-200 p-3 sm:p-4 shadow-lg">
        <div className="max-w-4xl mx-auto flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={handleSaveDraft}
            disabled={saving}
            className="flex items-center justify-center gap-2 px-4 py-2.5 text-xs sm:text-sm font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            <span>Salvar Rascunho</span>
          </button>

          <button
            type="button"
            onClick={handleOpenFinalizeModal}
            disabled={saving}
            className="flex items-center justify-center gap-2 px-5 py-2.5 text-xs sm:text-sm font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl shadow-md hover:shadow-lg transition-all disabled:opacity-50"
          >
            <CheckCircle2 className="w-4 h-4" />
            <span>{data.status === 'FINALIZADA' ? 'Salvar Alterações' : 'Finalizar Chamada'}</span>
          </button>
        </div>
      </div>

      {/* Modal de Justificativa / Observação */}
      {justificationModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-md bg-white p-6 rounded-2xl shadow-xl space-y-4">
            <h3 className="font-bold text-base text-slate-900">
              Observação / Justificativa - {justificationModal.studentName}
            </h3>
            <textarea
              rows={4}
              value={justificationModal.notes}
              onChange={(e) =>
                setJustificationModal({ ...justificationModal, notes: e.target.value })
              }
              placeholder="Ex: Atestado médico apresentado pela mãe; Consulta ao dentista; Chegou 20 min atrasado..."
              className="w-full p-3 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setJustificationModal(null)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={saveJustification}
                className="px-4 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow"
              >
                Salvar Observação
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Confirmação de Finalização / Motivo da Alteração */}
      {finalizeConfirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-md bg-white p-6 rounded-2xl shadow-xl space-y-4">
            <div className="flex items-center gap-2 text-emerald-700 font-bold text-base">
              <CheckCircle2 className="w-5 h-5" />
              <span>{data.status === 'FINALIZADA' ? 'Registrar Alteração da Chamada' : 'Confirmar Finalização da Chamada'}</span>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              {data.status === 'FINALIZADA'
                ? 'Como esta chamada já havia sido finalizada anteriormente, é obrigatório registrar o motivo da retificação para fins de auditoria.'
                : `Você está prestes a finalizar a chamada com ${presentCount} presentes, ${absentCount} ausentes e ${excusedCount} justificadas. A aula será marcada como Realizada.`}
            </p>

            {data.status === 'FINALIZADA' && (
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Motivo da Alteração *
                </label>
                <input
                  type="text"
                  required
                  value={changeReason}
                  onChange={(e) => setChangeReason(e.target.value)}
                  placeholder="Ex: Aluno Lucas apresentou atestado posterior; Erro de digitação..."
                  className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>
            )}

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setFinalizeConfirmModal(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Voltar
              </button>
              <button
                type="button"
                onClick={handleFinalizeAttendance}
                disabled={saving}
                className="px-5 py-2 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl shadow"
              >
                {saving ? 'Processando...' : 'Confirmar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Histórico de Auditoria */}
      {showHistoryModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-lg bg-white p-6 rounded-2xl shadow-xl space-y-4 max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-2 border-b border-slate-200">
              <div className="flex items-center gap-2 font-bold text-base text-slate-900">
                <History className="w-5 h-5 text-brand-600" />
                <span>Histórico de Auditoria da Chamada</span>
              </div>
              <button
                onClick={() => setShowHistoryModal(false)}
                className="text-slate-400 hover:text-slate-600 text-lg font-bold"
              >
                ✕
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-3 pr-1">
              {data.auditLogs && data.auditLogs.length > 0 ? (
                data.auditLogs.map((log) => (
                  <div key={log.id} className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs space-y-1">
                    <div className="flex items-center justify-between font-bold text-slate-800">
                      <span>{log.action}</span>
                      <span className="text-[11px] text-slate-500 font-normal">
                        {new Date(log.createdAt).toLocaleString('pt-BR')}
                      </span>
                    </div>
                    <p className="text-slate-600">
                      Por: <strong className="text-slate-800">{log.changedByUserName}</strong>
                    </p>
                    {log.reason && (
                      <p className="text-slate-600">
                        Motivo: <span className="italic">{log.reason}</span>
                      </p>
                    )}
                    {log.details && (
                      <p className="text-slate-500 font-mono text-[11px] bg-white p-2 rounded border border-slate-200 mt-1">
                        {log.details}
                      </p>
                    )}
                  </div>
                ))
              ) : (
                <p className="text-xs text-slate-500 text-center py-6">Nenhum log gravado ainda.</p>
              )}
            </div>

            <button
              onClick={() => setShowHistoryModal(false)}
              className="w-full py-2.5 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl"
            >
              Fechar
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
