import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Lesson, LessonStatus, SchoolClass } from '../types';
import { useAuth } from '../context/AuthContext';
import { LessonStatusBadge } from '../components/StatusBadge';
import {
  CalendarDays,
  Plus,
  Clock,
  ClipboardCheck,
  Ban,
  CalendarCheck,
  CalendarClock,
  Sparkles,
  AlertCircle,
  Filter
} from 'lucide-react';

interface LessonsProps {
  onNavigate: (page: string, params?: any) => void;
}

export const Lessons: React.FC<LessonsProps> = ({ onNavigate }) => {
  const { isAdmin } = useAuth();
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [loading, setLoading] = useState(true);

  // Filtros
  const [selectedClass, setSelectedClass] = useState<string>('');
  const [selectedStatus, setSelectedStatus] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');

  // Modais de Ação
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createClassId, setCreateClassId] = useState('');
  const [createDate, setCreateDate] = useState(new Date().toISOString().substring(0, 10));
  const [createStartTime, setCreateStartTime] = useState('08:30');
  const [createEndTime, setCreateEndTime] = useState('10:00');
  const [createType, setCreateType] = useState('EXTRA');
  const [createNotes, setCreateNotes] = useState('');

  const [cancelModal, setCancelModal] = useState<{ lessonId: string; className: string } | null>(null);
  const [cancelReason, setCancelReason] = useState('');

  const [rescheduleModal, setRescheduleModal] = useState<{ lessonId: string; className: string } | null>(null);
  const [rescheduleDate, setRescheduleDate] = useState('');
  const [rescheduleStartTime, setRescheduleStartTime] = useState('');
  const [rescheduleEndTime, setRescheduleEndTime] = useState('');
  const [rescheduleReason, setRescheduleReason] = useState('');

  const [generatingHorizon, setGeneratingHorizon] = useState(false);
  const [horizonMsg, setHorizonMsg] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    loadClasses();
    loadLessons();
  }, [selectedClass, selectedStatus, startDate, endDate]);

  const loadClasses = async () => {
    try {
      const cls = await api.getAllActiveClasses();
      setClasses(cls);
      if (cls.length > 0 && !createClassId) {
        setCreateClassId(cls[0].id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const loadLessons = async () => {
    setLoading(true);
    try {
      const q = new URLSearchParams();
      if (selectedClass) q.append('classId', selectedClass);
      if (selectedStatus) q.append('status', selectedStatus);
      if (startDate) q.append('startDate', startDate);
      if (endDate) q.append('endDate', endDate);
      q.append('page', '0');
      q.append('size', '50');

      const res = await api.getLessons(q.toString());
      setLessons(res.content || []);
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateHorizon = async () => {
    if (!window.confirm('Deseja executar a geração idempotente de treinos para o horizonte configurado?')) return;
    setGeneratingHorizon(true);
    setHorizonMsg(null);
    try {
      const res = await api.generateHorizonLessons();
      setHorizonMsg(res.message);
      loadLessons();
    } catch (err: any) {
      alert(err.message || 'Erro ao gerar aulas futuras.');
    } finally {
      setGeneratingHorizon(false);
    }
  };

  const handleCreateLesson = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionError(null);
    try {
      await api.createLesson({
        classId: createClassId,
        date: createDate,
        startTime: `${createStartTime}:00`,
        endTime: `${createEndTime}:00`,
        type: createType,
        notes: createNotes || null,
      });
      setShowCreateModal(false);
      loadLessons();
    } catch (err: any) {
      setActionError(err.message || 'Erro ao criar aula.');
    }
  };

  const handleCancelLesson = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!cancelModal) return;
    try {
      await api.cancelLesson(cancelModal.lessonId, cancelReason);
      setCancelModal(null);
      setCancelReason('');
      loadLessons();
    } catch (err: any) {
      alert(err.message || 'Erro ao cancelar treino.');
    }
  };

  const handleRescheduleLesson = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rescheduleModal) return;
    try {
      await api.rescheduleLesson(rescheduleModal.lessonId, {
        newDate: rescheduleDate,
        newStartTime: `${rescheduleStartTime}:00`,
        newEndTime: `${rescheduleEndTime}:00`,
        reason: rescheduleReason,
      });
      setRescheduleModal(null);
      loadLessons();
    } catch (err: any) {
      alert(err.message || 'Erro ao remarcar treino.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Cabeçalho */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
            Agenda & Aulas
          </h2>
          <p className="text-xs sm:text-sm text-slate-500">
            Aulas regulares geradas por horizonte, treinos extras, cancelamentos e chamadas
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 self-start sm:self-auto">
          {isAdmin && (
            <button
              onClick={handleGenerateHorizon}
              disabled={generatingHorizon}
              className="flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold text-brand-700 bg-brand-50 hover:bg-brand-100 border border-brand-200 rounded-xl transition-colors shadow-xs"
              title="Gera aulas idempotentes para as próximas semanas"
            >
              <Sparkles className="w-3.5 h-3.5 text-brand-600" />
              <span>{generatingHorizon ? 'Gerando...' : 'Gerar Futuras'}</span>
            </button>
          )}

          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-md transition-all"
          >
            <Plus className="w-4 h-4" /> Nova Aula Extra
          </button>
        </div>
      </div>

      {horizonMsg && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs font-semibold flex items-center justify-between">
          <span>{horizonMsg}</span>
          <button onClick={() => setHorizonMsg(null)} className="text-emerald-700">✕</button>
        </div>
      )}

      {/* Barra de Filtros */}
      <div className="bg-white p-4 border border-slate-200 rounded-2xl shadow-sm">
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 text-xs">
          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">Filtrar por Turma</label>
            <select
              value={selectedClass}
              onChange={(e) => setSelectedClass(e.target.value)}
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            >
              <option value="">Todas as turmas</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">Situação da Aula</label>
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            >
              <option value="">Todas as situações</option>
              <option value="AGENDADA">Agendada</option>
              <option value="REALIZADA">Realizada</option>
              <option value="CANCELADA">Cancelada</option>
            </select>
          </div>

          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">A partir de</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            />
          </div>

          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">Até a data</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            />
          </div>
        </div>
      </div>

      {/* Lista de Aulas */}
      {loading ? (
        <div className="p-12 text-center text-slate-500">
          <div className="w-8 h-8 border-3 border-brand-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs">Carregando aulas...</p>
        </div>
      ) : lessons.length === 0 ? (
        <div className="bg-white p-12 text-center border border-slate-200 rounded-2xl">
          <CalendarDays className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-700">Nenhuma aula encontrada</h3>
          <p className="text-xs text-slate-500 mt-1">Ajuste os filtros ou gere novas aulas pelo horizonte configurado.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {lessons.map((lesson) => {
            const isToday = lesson.date === new Date().toISOString().substring(0, 10);
            const isFinalized = lesson.attendanceStatus === 'FINALIZADA';
            const isCancelled = lesson.status === 'CANCELADA';

            return (
              <div
                key={lesson.id}
                className={`p-4 bg-white border rounded-2xl shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-all ${
                  isToday ? 'border-brand-300 ring-1 ring-brand-200' : 'border-slate-200'
                }`}
              >
                <div className="flex items-start gap-3.5">
                  <div className={`flex flex-col items-center justify-center w-14 h-14 rounded-xl border shrink-0 ${
                    isToday ? 'bg-brand-50 border-brand-200 text-brand-700' : 'bg-slate-50 border-slate-200 text-slate-700'
                  }`}>
                    <span className="text-[10px] font-bold uppercase">
                      {new Date(lesson.date + 'T12:00:00').toLocaleDateString('pt-BR', { weekday: 'short' })}
                    </span>
                    <span className="text-xs font-black">
                      {new Date(lesson.date + 'T12:00:00').toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })}
                    </span>
                  </div>

                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-extrabold text-slate-900 text-sm sm:text-base">
                        {lesson.className}
                      </span>
                      <span className="text-xs font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                        {lesson.category}
                      </span>
                      {lesson.type !== 'REGULAR' && (
                        <span className="text-[11px] font-bold px-2 py-0.5 rounded bg-purple-100 text-purple-800">
                          {lesson.type}
                        </span>
                      )}
                    </div>

                    <p className="text-xs text-slate-500 mt-0.5 flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5" />
                      {lesson.startTime.substring(0, 5)} às {lesson.endTime.substring(0, 5)} • {lesson.eligibleStudentsCount} alunos matriculados
                    </p>

                    {lesson.notes && (
                      <p className="text-xs text-slate-600 italic mt-1 bg-slate-50 px-2 py-0.5 rounded border border-slate-100">
                        {lesson.notes}
                      </p>
                    )}

                    <div className="mt-2 flex items-center gap-2">
                      <LessonStatusBadge status={lesson.status} />
                      {isFinalized && (
                        <span className="text-xs font-bold text-emerald-700 bg-emerald-100 px-2.5 py-0.5 rounded-full">
                          ✓ Chamada Finalizada
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Ações da Aula */}
                <div className="flex items-center gap-2 self-end sm:self-center">
                  {!isCancelled && (
                    <button
                      onClick={() => onNavigate('attendance', { lessonId: lesson.id })}
                      className={`flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold rounded-xl transition-all shadow-sm ${
                        isFinalized
                          ? 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                          : 'bg-emerald-600 text-white hover:bg-emerald-700'
                      }`}
                    >
                      <ClipboardCheck className="w-4 h-4" />
                      <span>{isFinalized ? 'Ver / Editar' : 'Fazer Chamada'}</span>
                    </button>
                  )}

                  {!isFinalized && !isCancelled && (
                    <>
                      <button
                        onClick={() => {
                          setRescheduleModal({ lessonId: lesson.id, className: lesson.className });
                          setRescheduleDate(lesson.date);
                          setRescheduleStartTime(lesson.startTime.substring(0, 5));
                          setRescheduleEndTime(lesson.endTime.substring(0, 5));
                        }}
                        className="p-2 text-slate-500 hover:text-brand-600 hover:bg-slate-100 rounded-lg"
                        title="Remarcar treino"
                      >
                        <CalendarClock className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => setCancelModal({ lessonId: lesson.id, className: lesson.className })}
                        className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg"
                        title="Cancelar treino"
                      >
                        <Ban className="w-4 h-4" />
                      </button>
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal Criar Aula Extra */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-md bg-white p-6 rounded-2xl shadow-xl space-y-4">
            <h3 className="font-bold text-base text-slate-900">Agendar Aula Extra / Avulsa</h3>
            {actionError && (
              <p className="text-xs text-rose-600 bg-rose-50 p-2.5 rounded-lg border border-rose-200">
                {actionError}
              </p>
            )}
            <form onSubmit={handleCreateLesson} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Turma *</label>
                <select
                  required
                  value={createClassId}
                  onChange={(e) => setCreateClassId(e.target.value)}
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                >
                  {classes.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Data da Aula *</label>
                <input
                  type="date"
                  required
                  value={createDate}
                  onChange={(e) => setCreateDate(e.target.value)}
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Início *</label>
                  <input
                    type="time"
                    required
                    value={createStartTime}
                    onChange={(e) => setCreateStartTime(e.target.value)}
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                  />
                </div>
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Término *</label>
                  <input
                    type="time"
                    required
                    value={createEndTime}
                    onChange={(e) => setCreateEndTime(e.target.value)}
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Tipo de Aula</label>
                <select
                  value={createType}
                  onChange={(e) => setCreateType(e.target.value)}
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                >
                  <option value="EXTRA">Aula Extra</option>
                  <option value="REGULAR">Aula Regular</option>
                </select>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Observações</label>
                <input
                  type="text"
                  value={createNotes}
                  onChange={(e) => setCreateNotes(e.target.value)}
                  placeholder="Ex: Treino tático preparatório para campeonato"
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow"
                >
                  Agendar Aula
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Cancelar Aula */}
      {cancelModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-sm bg-white p-6 rounded-2xl shadow-xl space-y-4">
            <h3 className="font-bold text-base text-rose-700 flex items-center gap-2">
              <Ban className="w-5 h-5" /> Cancelar Treino
            </h3>
            <p className="text-xs text-slate-600">
              O cancelamento da aula não gerará faltas para os alunos matriculados na turma <strong>{cancelModal.className}</strong>.
            </p>
            <form onSubmit={handleCancelLesson} className="space-y-3">
              <input
                type="text"
                required
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="Motivo (ex: Chuva forte, reforma no campo...)"
                className="w-full p-2.5 text-xs bg-slate-50 border border-slate-300 rounded-xl"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setCancelModal(null)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Voltar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-xl shadow"
                >
                  Confirmar Cancelamento
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Remarcar Aula */}
      {rescheduleModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-md bg-white p-6 rounded-2xl shadow-xl space-y-4">
            <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
              <CalendarClock className="w-5 h-5 text-brand-600" /> Remarcar Treino - {rescheduleModal.className}
            </h3>
            <form onSubmit={handleRescheduleLesson} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Nova Data *</label>
                <input
                  type="date"
                  required
                  value={rescheduleDate}
                  onChange={(e) => setRescheduleDate(e.target.value)}
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Novo Horário Início *</label>
                  <input
                    type="time"
                    required
                    value={rescheduleStartTime}
                    onChange={(e) => setRescheduleStartTime(e.target.value)}
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                  />
                </div>
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Novo Horário Fim *</label>
                  <input
                    type="time"
                    required
                    value={rescheduleEndTime}
                    onChange={(e) => setRescheduleEndTime(e.target.value)}
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Motivo da Remarcação</label>
                <input
                  type="text"
                  value={rescheduleReason}
                  onChange={(e) => setRescheduleReason(e.target.value)}
                  placeholder="Ex: Feriado municipal, reposição de jogo..."
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setRescheduleModal(null)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Voltar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow"
                >
                  Confirmar Remarcação
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
