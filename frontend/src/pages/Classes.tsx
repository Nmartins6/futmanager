import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { SchoolClass, ClassSchedule, TeacherSummary, User, ClassStatus, EnrollmentStatus } from '../types';
import { useAuth } from '../context/AuthContext';
import { GeneralStatusBadge } from '../components/StatusBadge';
import {
  Users,
  Plus,
  Clock,
  MapPin,
  UserCheck,
  Calendar,
  AlertCircle,
  Eye,
  Edit2,
  Trash2,
  CheckCircle2
} from 'lucide-react';

export const Classes: React.FC = () => {
  const { isAdmin } = useAuth();
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [teachers, setTeachers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  // Modal Turma
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formName, setFormName] = useState('');
  const [formCategory, setFormCategory] = useState('');
  const [formLocation, setFormLocation] = useState('');
  const [formCapacity, setFormCapacity] = useState<number | undefined>(undefined);
  const [formStatus, setFormStatus] = useState<ClassStatus>('ATIVO');
  const [formStartDate, setFormStartDate] = useState('');
  const [formEndDate, setFormEndDate] = useState('');

  // Professores selecionados
  const [selectedTeachers, setSelectedTeachers] = useState<{ userId: string; substitute: boolean }[]>([]);

  // Horários recorrentes da turma
  const [schedules, setSchedules] = useState<ClassSchedule[]>([
    { dayOfWeek: 'MONDAY', startTime: '08:30:00', endTime: '10:00:00' },
  ]);

  const [formError, setFormError] = useState<string | null>(null);
  const [formSaving, setFormSaving] = useState(false);

  // Modal Alunos Matriculados na Turma
  const [viewingClassEnrollments, setViewingClassEnrollments] = useState<any[] | null>(null);
  const [viewingClassName, setViewingClassName] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [cls, tchs] = await Promise.all([
        api.getAllActiveClasses(),
        api.getActiveTeachers(),
      ]);
      setClasses(cls);
      setTeachers(tchs);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenCreate = () => {
    setEditingId(null);
    setFormName('');
    setFormCategory('Sub-9');
    setFormLocation('Campo 1');
    setFormCapacity(20);
    setFormStatus('ATIVO');
    setFormStartDate(new Date().toISOString().substring(0, 10));
    setFormEndDate('');
    setSelectedTeachers(teachers.length > 0 ? [{ userId: teachers[0].id, substitute: false }] : []);
    setSchedules([
      { dayOfWeek: 'MONDAY', startTime: '08:30', endTime: '10:00' },
      { dayOfWeek: 'WEDNESDAY', startTime: '08:30', endTime: '10:00' },
    ]);
    setFormError(null);
    setShowModal(true);
  };

  const handleOpenEdit = async (id: string) => {
    setEditingId(id);
    setFormError(null);
    try {
      const c = await api.getClassById(id);
      setFormName(c.name);
      setFormCategory(c.category);
      setFormLocation(c.location || '');
      setFormCapacity(c.maxCapacity);
      setFormStatus(c.status);
      setFormStartDate(c.startDate || '');
      setFormEndDate(c.endDate || '');
      setSelectedTeachers(
        c.teachers.map((t: TeacherSummary) => ({ userId: t.userId, substitute: t.substitute }))
      );
      setSchedules(
        c.schedules.map((s: ClassSchedule) => ({
          dayOfWeek: s.dayOfWeek,
          startTime: s.startTime.substring(0, 5),
          endTime: s.endTime.substring(0, 5),
        }))
      );
      setShowModal(true);
    } catch (err: any) {
      alert(err.message || 'Erro ao carregar turma.');
    }
  };

  const handleAddSchedule = () => {
    setSchedules([...schedules, { dayOfWeek: 'TUESDAY', startTime: '14:30', endTime: '16:00' }]);
  };

  const handleRemoveSchedule = (index: number) => {
    setSchedules(schedules.filter((_, i) => i !== index));
  };

  const handleViewEnrollments = async (c: SchoolClass) => {
    try {
      const list = await api.getClassEnrollments(c.id);
      setViewingClassEnrollments(list);
      setViewingClassName(c.name);
    } catch (err: any) {
      alert(err.message || 'Erro ao listar matrículas da turma.');
    }
  };

  const handleSaveClass = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormSaving(true);
    setFormError(null);

    // Formatar horários com segundos caso necessário
    const formattedSchedules = schedules.map((s) => ({
      dayOfWeek: s.dayOfWeek,
      startTime: s.startTime.length === 5 ? `${s.startTime}:00` : s.startTime,
      endTime: s.endTime.length === 5 ? `${s.endTime}:00` : s.endTime,
    }));

    const payload: any = {
      name: formName.trim(),
      category: formCategory.trim(),
      location: formLocation.trim() || null,
      maxCapacity: formCapacity || null,
      status: formStatus,
      startDate: formStartDate || null,
      endDate: formEndDate || null,
      teachers: selectedTeachers,
      schedules: formattedSchedules,
    };

    try {
      if (editingId) {
        await api.updateClass(editingId, payload);
      } else {
        await api.createClass(payload);
      }
      setShowModal(false);
      loadData();
    } catch (err: any) {
      setFormError(err.message || 'Erro ao salvar turma.');
    } finally {
      setFormSaving(false);
    }
  };

  const dayOfWeekPtBr: Record<string, string> = {
    MONDAY: 'Segunda-feira',
    TUESDAY: 'Terça-feira',
    WEDNESDAY: 'Quarta-feira',
    THURSDAY: 'Quinta-feira',
    FRIDAY: 'Sexta-feira',
    SATURDAY: 'Sábado',
    SUNDAY: 'Domingo',
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
            Turmas & Horários
          </h2>
          <p className="text-xs sm:text-sm text-slate-500">
            Categorias, dias da semana, horários recorrentes e professores autorizados
          </p>
        </div>
        {isAdmin && (
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-4 py-2.5 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-md transition-all self-start sm:self-auto"
          >
            <Plus className="w-4 h-4" /> Nova Turma
          </button>
        )}
      </div>

      {loading ? (
        <div className="p-12 text-center text-slate-500">
          <div className="w-8 h-8 border-3 border-brand-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs">Carregando turmas...</p>
        </div>
      ) : classes.length === 0 ? (
        <div className="bg-white p-12 text-center border border-slate-200 rounded-2xl">
          <Users className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-700">Nenhuma turma cadastrada</h3>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {classes.map((c) => (
            <div
              key={c.id}
              className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col justify-between hover:border-slate-300 transition-all"
            >
              <div>
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div>
                    <h3 className="font-extrabold text-slate-900 text-base">{c.name}</h3>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="text-xs font-bold px-2 py-0.5 rounded bg-brand-50 text-brand-700">
                        {c.category}
                      </span>
                      {c.location && (
                        <span className="text-xs text-slate-500 flex items-center gap-1">
                          <MapPin className="w-3.5 h-3.5 text-slate-400" /> {c.location}
                        </span>
                      )}
                    </div>
                  </div>
                  <GeneralStatusBadge status={c.status} />
                </div>

                {/* Professores Vinculados */}
                <div className="mb-3 p-3 bg-slate-50 border border-slate-100 rounded-xl text-xs space-y-1">
                  <span className="font-bold text-slate-700 block">Professores Autorizados:</span>
                  {c.teachers && c.teachers.length > 0 ? (
                    <div className="flex flex-wrap gap-1.5">
                      {c.teachers.map((t) => (
                        <span
                          key={t.userId}
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md font-semibold text-[11px] ${
                            t.substitute
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-emerald-100 text-emerald-800'
                          }`}
                        >
                          <UserCheck className="w-3 h-3" /> {t.fullName}
                          {t.substitute && ' (Substituto)'}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <span className="text-slate-400 italic">Nenhum professor vinculado</span>
                  )}
                </div>

                {/* Horários Semanais */}
                <div className="space-y-1.5 text-xs text-slate-600 mb-4">
                  <span className="font-bold text-slate-700 block">Horários de Treino:</span>
                  {c.schedules && c.schedules.length > 0 ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
                      {c.schedules.map((s, idx) => (
                        <div key={idx} className="flex items-center gap-1.5 bg-slate-50 px-2.5 py-1.5 rounded-lg border border-slate-100">
                          <Clock className="w-3.5 h-3.5 text-brand-600 shrink-0" />
                          <span className="font-semibold text-slate-800">
                            {dayOfWeekPtBr[s.dayOfWeek] || s.dayOfWeek}:
                          </span>
                          <span>{s.startTime.substring(0, 5)} - {s.endTime.substring(0, 5)}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <span className="text-slate-400 italic">Nenhum horário definido</span>
                  )}
                </div>
              </div>

              <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs">
                <span className="font-semibold text-slate-600">
                  {c.activeStudentsCount} alunos matriculados
                  {c.maxCapacity ? ` / máx ${c.maxCapacity}` : ''}
                </span>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => handleViewEnrollments(c)}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold"
                  >
                    <Eye className="w-3.5 h-3.5" /> Alunos
                  </button>
                  {isAdmin && (
                    <button
                      type="button"
                      onClick={() => handleOpenEdit(c.id)}
                      className="p-1.5 text-slate-600 hover:text-brand-600 hover:bg-slate-100 rounded-lg"
                      title="Editar Turma"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal Alunos Matriculados na Turma */}
      {viewingClassEnrollments && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-lg bg-white p-6 rounded-2xl shadow-xl space-y-4 max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-2 border-b border-slate-200">
              <h3 className="font-bold text-base text-slate-900">
                Alunos Matriculados - {viewingClassName}
              </h3>
              <button
                onClick={() => setViewingClassEnrollments(null)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-2 pr-1">
              {viewingClassEnrollments.length === 0 ? (
                <p className="text-xs text-slate-500 text-center py-6">Nenhum aluno matriculado nesta turma.</p>
              ) : (
                viewingClassEnrollments.map((e) => (
                  <div key={e.id} className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs flex items-center justify-between">
                    <div>
                      <p className="font-bold text-slate-900">{e.studentName}</p>
                      <p className="text-slate-500 text-[11px] mt-0.5">
                        {e.studentAge} anos • Matrícula: {new Date(e.entryDate + 'T12:00:00').toLocaleDateString('pt-BR')}
                      </p>
                    </div>
                    <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-slate-200 text-slate-700">
                      {e.status}
                    </span>
                  </div>
                ))
              )}
            </div>

            <button
              onClick={() => setViewingClassEnrollments(null)}
              className="w-full py-2.5 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl"
            >
              Fechar
            </button>
          </div>
        </div>
      )}

      {/* Modal Cadastrar/Editar Turma */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-xl bg-white p-6 rounded-2xl shadow-2xl max-h-[90vh] overflow-y-auto space-y-4">
            <h3 className="text-base font-bold text-slate-900">
              {editingId ? 'Editar Turma' : 'Nova Turma'}
            </h3>

            {formError && (
              <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleSaveClass} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <div className="sm:col-span-2">
                  <label className="block font-bold text-slate-700 mb-1">Nome da Turma *</label>
                  <input
                    type="text"
                    required
                    value={formName}
                    onChange={(e) => setFormName(e.target.value)}
                    placeholder="Ex: Sub-11 Manhã (Fundamentos)"
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Categoria / Faixa Etária *</label>
                  <input
                    type="text"
                    required
                    value={formCategory}
                    onChange={(e) => setFormCategory(e.target.value)}
                    placeholder="Ex: Sub-11"
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Local de Treino</label>
                  <input
                    type="text"
                    value={formLocation}
                    onChange={(e) => setFormLocation(e.target.value)}
                    placeholder="Ex: Campo Sintético 1"
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Capacidade Máxima</label>
                  <input
                    type="number"
                    value={formCapacity || ''}
                    onChange={(e) => setFormCapacity(e.target.value ? parseInt(e.target.value) : undefined)}
                    placeholder="Ex: 25"
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Situação *</label>
                  <select
                    value={formStatus}
                    onChange={(e) => setFormStatus(e.target.value as ClassStatus)}
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                  >
                    <option value="ATIVO">Ativa</option>
                    <option value="INATIVO">Inativa</option>
                  </select>
                </div>
              </div>

              {/* Professores Autorizados */}
              <div className="pt-3 border-t border-slate-200">
                <label className="block text-xs font-bold text-slate-900 mb-1.5">
                  Professores Autorizados (Titulares e Substitutos)
                </label>
                <div className="space-y-2">
                  {teachers.map((t) => {
                    const isSelected = selectedTeachers.some((st) => st.userId === t.id);
                    const isSub = selectedTeachers.find((st) => st.userId === t.id)?.substitute || false;

                    return (
                      <div
                        key={t.id}
                        className="flex items-center justify-between p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs"
                      >
                        <label className="flex items-center gap-2 cursor-pointer font-semibold text-slate-800">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={(e) => {
                              if (e.target.checked) {
                                setSelectedTeachers([...selectedTeachers, { userId: t.id, substitute: false }]);
                              } else {
                                setSelectedTeachers(selectedTeachers.filter((st) => st.userId !== t.id));
                              }
                            }}
                            className="rounded text-brand-600 focus:ring-brand-500"
                          />
                          <span>{t.fullName}</span>
                        </label>

                        {isSelected && (
                          <label className="flex items-center gap-1.5 cursor-pointer text-slate-600 font-medium text-[11px]">
                            <input
                              type="checkbox"
                              checked={isSub}
                              onChange={(e) => {
                                setSelectedTeachers(
                                  selectedTeachers.map((st) =>
                                    st.userId === t.id ? { ...st, substitute: e.target.checked } : st
                                  )
                                );
                              }}
                              className="rounded text-amber-600 focus:ring-amber-500"
                            />
                            <span>Substituto</span>
                          </label>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Horários Recorrentes */}
              <div className="pt-3 border-t border-slate-200">
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-bold text-slate-900">
                    Horários Semanais de Treino
                  </label>
                  <button
                    type="button"
                    onClick={handleAddSchedule}
                    className="flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-brand-700 bg-brand-50 rounded-lg hover:bg-brand-100"
                  >
                    <Plus className="w-3.5 h-3.5" /> Adicionar Horário
                  </button>
                </div>

                <div className="space-y-2">
                  {schedules.map((s, index) => (
                    <div key={index} className="flex items-center gap-2 bg-slate-50 p-2 rounded-xl border border-slate-200 text-xs">
                      <select
                        value={s.dayOfWeek}
                        onChange={(e) => {
                          const updated = [...schedules];
                          updated[index].dayOfWeek = e.target.value;
                          setSchedules(updated);
                        }}
                        className="p-1.5 bg-white border border-slate-300 rounded-lg flex-1"
                      >
                        <option value="MONDAY">Segunda-feira</option>
                        <option value="TUESDAY">Terça-feira</option>
                        <option value="WEDNESDAY">Quarta-feira</option>
                        <option value="THURSDAY">Quinta-feira</option>
                        <option value="FRIDAY">Sexta-feira</option>
                        <option value="SATURDAY">Sábado</option>
                        <option value="SUNDAY">Domingo</option>
                      </select>

                      <input
                        type="time"
                        required
                        value={s.startTime}
                        onChange={(e) => {
                          const updated = [...schedules];
                          updated[index].startTime = e.target.value;
                          setSchedules(updated);
                        }}
                        className="p-1.5 bg-white border border-slate-300 rounded-lg"
                      />
                      <span>até</span>
                      <input
                        type="time"
                        required
                        value={s.endTime}
                        onChange={(e) => {
                          const updated = [...schedules];
                          updated[index].endTime = e.target.value;
                          setSchedules(updated);
                        }}
                        className="p-1.5 bg-white border border-slate-300 rounded-lg"
                      />

                      {schedules.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleRemoveSchedule(index)}
                          className="p-1 text-slate-400 hover:text-rose-600"
                          title="Remover horário"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={formSaving}
                  className="px-5 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow disabled:opacity-50"
                >
                  {formSaving ? 'Gravando...' : 'Salvar Turma'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
