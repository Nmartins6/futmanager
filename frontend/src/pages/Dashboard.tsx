import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { DashboardOverview, Lesson } from '../types';
import { useAuth } from '../context/AuthContext';
import { LessonStatusBadge } from '../components/StatusBadge';
import {
  Users,
  CalendarCheck,
  AlertTriangle,
  Clock,
  ClipboardCheck,
  ChevronRight,
  TrendingDown,
  ArrowRight
} from 'lucide-react';

interface DashboardProps {
  onNavigate: (page: string, params?: any) => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onNavigate }) => {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.getDashboardOverview();
      setData(res);
    } catch (err: any) {
      setError(err.message || 'Falha ao carregar painel.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-3 text-slate-500">
          <div className="w-8 h-8 border-3 border-brand-600 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm font-medium">Carregando painel da escolinha...</p>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="p-6 text-center bg-white border border-rose-200 rounded-2xl shadow-sm">
        <p className="text-rose-600 font-medium mb-3">{error || 'Dados indisponíveis'}</p>
        <button
          onClick={loadDashboard}
          className="px-4 py-2 text-xs font-semibold text-white bg-brand-600 rounded-xl"
        >
          Tentar Novamente
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Saudação */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-gradient-to-r from-brand-900 via-brand-800 to-slate-900 p-6 rounded-2xl text-white shadow-lg">
        <div>
          <h2 className="text-xl sm:text-2xl font-black tracking-tight">
            Olá, {user?.fullName?.split(' ')[0]}! ⚽
          </h2>
          <p className="text-xs sm:text-sm text-slate-300 mt-1">
            Pronto para os treinos de hoje? Acompanhe as chamadas e o desempenho dos alunos.
          </p>
        </div>
        <button
          onClick={() => onNavigate('lessons')}
          className="inline-flex items-center gap-2 px-4 py-2 text-xs font-bold text-slate-900 bg-emerald-400 hover:bg-emerald-300 rounded-xl transition-all self-start sm:self-auto shadow"
        >
          <CalendarCheck className="w-4 h-4" /> Ver Agenda Completa
        </button>
      </div>

      {/* Cards de Métricas Principais */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-4 sm:p-5 bg-white border border-slate-200 rounded-2xl shadow-sm flex items-center gap-3.5">
          <div className="p-3 rounded-xl bg-blue-50 text-blue-600 shrink-0">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500">Alunos Cadastrados</p>
            <p className="text-xl sm:text-2xl font-black text-slate-900">{data.totalActiveStudents}</p>
          </div>
        </div>

        <div className="p-4 sm:p-5 bg-white border border-slate-200 rounded-2xl shadow-sm flex items-center gap-3.5">
          <div className="p-3 rounded-xl bg-emerald-50 text-emerald-600 shrink-0">
            <CalendarCheck className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500">Turmas Ativas</p>
            <p className="text-xl sm:text-2xl font-black text-slate-900">{data.totalClasses}</p>
          </div>
        </div>

        <div
          onClick={() => onNavigate('statistics')}
          className="p-4 sm:p-5 bg-white border border-slate-200 rounded-2xl shadow-sm flex items-center gap-3.5 cursor-pointer hover:border-amber-300 transition-all"
        >
          <div className="p-3 rounded-xl bg-amber-50 text-amber-600 shrink-0">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500">Chamadas Pendentes</p>
            <p className="text-xl sm:text-2xl font-black text-amber-600">{data.pendingLessonsCount}</p>
          </div>
        </div>

        <div
          onClick={() => onNavigate('statistics')}
          className="p-4 sm:p-5 bg-white border border-slate-200 rounded-2xl shadow-sm flex items-center gap-3.5 cursor-pointer hover:border-rose-300 transition-all"
        >
          <div className="p-3 rounded-xl bg-rose-50 text-rose-600 shrink-0">
            <TrendingDown className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500">Alunos em Alerta</p>
            <p className="text-xl sm:text-2xl font-black text-rose-600">{data.lowFrequencyStudentsCount}</p>
          </div>
        </div>
      </div>

      {/* Seção Central: Aulas de Hoje (DESTAQUE PARA O PROFESSOR) */}
      <div className="bg-white border border-slate-200 rounded-2xl p-5 sm:p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse" />
            <h3 className="text-base sm:text-lg font-bold text-slate-900">
              Aulas de Hoje ({new Date().toLocaleDateString('pt-BR')})
            </h3>
          </div>
          <span className="text-xs font-semibold px-2.5 py-1 bg-slate-100 text-slate-600 rounded-lg">
            {data.todayLessons.length} {data.todayLessons.length === 1 ? 'aula' : 'aulas'}
          </span>
        </div>

        {data.todayLessons.length === 0 ? (
          <div className="text-center py-8 text-slate-400">
            <CalendarCheck className="w-10 h-10 mx-auto mb-2 opacity-50" />
            <p className="text-sm">Nenhum treino agendado para o dia de hoje.</p>
            <button
              onClick={() => onNavigate('lessons')}
              className="mt-3 text-xs font-bold text-brand-600 hover:underline"
            >
              Consultar calendário de treinos
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            {data.todayLessons.map((lesson) => {
              const isFinalized = lesson.attendanceStatus === 'FINALIZADA';
              const isDraft = lesson.attendanceStatus === 'RASCUNHO';

              return (
                <div
                  key={lesson.id}
                  className="flex flex-col sm:flex-row sm:items-center justify-between p-4 bg-slate-50 hover:bg-slate-100/80 border border-slate-200 rounded-xl gap-3 transition-colors"
                >
                  <div className="flex items-start gap-3">
                    <div className="flex flex-col items-center justify-center w-14 h-14 bg-white border border-slate-200 rounded-xl shrink-0 shadow-2xl">
                      <span className="text-[11px] font-bold text-slate-500 uppercase">Horário</span>
                      <span className="text-xs font-black text-slate-800">
                        {lesson.startTime?.substring(0, 5)}
                      </span>
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-slate-900 text-sm sm:text-base">
                          {lesson.className}
                        </span>
                        <span className="text-xs font-semibold px-2 py-0.5 rounded bg-brand-50 text-brand-700">
                          {lesson.category}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 mt-0.5">
                        {lesson.startTime?.substring(0, 5)} às {lesson.endTime?.substring(0, 5)} • {lesson.eligibleStudentsCount} alunos matriculados
                      </p>
                      <div className="mt-1 flex items-center gap-2">
                        <LessonStatusBadge status={lesson.status} />
                        {isFinalized && (
                          <span className="text-xs font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                            ✓ Chamada Finalizada
                          </span>
                        )}
                        {isDraft && (
                          <span className="text-xs font-semibold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-full">
                            ● Rascunho Salvo
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Botão de Destaque da Chamada */}
                  <div className="flex items-center gap-2 sm:self-center">
                    <button
                      onClick={() => onNavigate('attendance', { lessonId: lesson.id })}
                      className={`flex items-center justify-center gap-1.5 w-full sm:w-auto px-4 py-2.5 text-xs font-bold rounded-xl shadow-sm transition-all ${
                        isFinalized
                          ? 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'
                          : 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-600/20 shadow-md'
                      }`}
                    >
                      <ClipboardCheck className="w-4 h-4" />
                      <span>{isFinalized ? 'Ver / Corrigir Chamada' : 'Fazer Chamada'}</span>
                      <ArrowRight className="w-3.5 h-3.5 ml-0.5" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Alunos com Baixa Frequência (< 75%) */}
      {data.lowFrequencyAlerts.length > 0 && (
        <div className="bg-white border border-rose-200 rounded-2xl p-5 sm:p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2 text-rose-700">
              <AlertTriangle className="w-5 h-5" />
              <h3 className="text-base font-bold text-slate-900">
                Alunos com Baixa Frequência (&lt; 75%)
              </h3>
            </div>
            <button
              onClick={() => onNavigate('statistics')}
              className="text-xs font-bold text-brand-600 hover:underline"
            >
              Ver relatório completo
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {data.lowFrequencyAlerts.slice(0, 6).map((student) => (
              <div
                key={student.studentId}
                onClick={() => onNavigate('students', { studentId: student.studentId })}
                className="p-3.5 bg-rose-50/50 hover:bg-rose-50 border border-rose-200 rounded-xl cursor-pointer flex items-center justify-between transition-colors"
              >
                <div>
                  <p className="text-sm font-bold text-slate-900">{student.studentName}</p>
                  <p className="text-xs text-slate-500">
                    {student.presences} presenças em {student.totalLessons} aulas ({student.unexcusedAbsences} faltas)
                  </p>
                </div>
                <span className="text-sm font-black text-rose-600 bg-white px-2.5 py-1 rounded-lg border border-rose-200 shadow-xs">
                  {student.attendancePercentage != null ? `${student.attendancePercentage}%` : 'S/D'}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
