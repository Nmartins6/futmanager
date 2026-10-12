import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import {
  ClassComparison,
  MonthlyEvolution,
  Lesson,
  SchoolClass,
  StudentSummary
} from '../types';
import {
  BarChart3,
  TrendingDown,
  Clock,
  Calendar,
  AlertTriangle,
  ArrowRight,
  ClipboardCheck,
  CheckCircle2,
  XCircle,
  HelpCircle
} from 'lucide-react';

interface StatisticsProps {
  onNavigate: (page: string, params?: any) => void;
}

export const Statistics: React.FC<StatisticsProps> = ({ onNavigate }) => {
  const [activeTab, setActiveTab] = useState<'comparison' | 'evolution' | 'pending' | 'student'>('comparison');
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [students, setStudents] = useState<StudentSummary[]>([]);

  // Dados das abas
  const [classComparison, setClassComparison] = useState<ClassComparison[]>([]);
  const [monthlyEvolution, setMonthlyEvolution] = useState<MonthlyEvolution[]>([]);
  const [pendingLessons, setPendingLessons] = useState<Lesson[]>([]);

  // Pesquisa individual de aluno
  const [selectedStudentId, setSelectedStudentId] = useState<string>('');
  const [studentStats, setStudentStats] = useState<any | null>(null);
  const [studentHistory, setStudentHistory] = useState<any[]>([]);

  const [loading, setLoading] = useState(true);

  // Filtros
  const [filterStartDate, setFilterStartDate] = useState('');
  const [filterEndDate, setFilterEndDate] = useState('');
  const [filterClassId, setFilterClassId] = useState('');

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    loadTabData();
  }, [activeTab, filterStartDate, filterEndDate, filterClassId]);

  const loadInitialData = async () => {
    try {
      const [cls, stus] = await Promise.all([
        api.getAllActiveClasses(),
        api.getStudents('size=100'),
      ]);
      setClasses(cls);
      setStudents(stus.content || []);
      if (stus.content && stus.content.length > 0) {
        setSelectedStudentId(stus.content[0].id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const loadTabData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'comparison') {
        const res = await api.getClassesComparison(filterStartDate, filterEndDate);
        setClassComparison(res);
      } else if (activeTab === 'evolution') {
        const res = await api.getMonthlyEvolution(filterClassId || undefined);
        setMonthlyEvolution(res);
      } else if (activeTab === 'pending') {
        const res = await api.getPendingLessons(filterClassId || undefined, filterStartDate, filterEndDate);
        setPendingLessons(res);
      } else if (activeTab === 'student' && selectedStudentId) {
        const [stats, hist] = await Promise.all([
          api.getStudentFrequency(selectedStudentId, filterClassId || undefined, filterStartDate, filterEndDate),
          api.getStudentHistory(selectedStudentId, filterClassId || undefined, filterStartDate, filterEndDate),
        ]);
        setStudentStats(stats);
        setStudentHistory(hist);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleStudentSelect = async (studentId: string) => {
    setSelectedStudentId(studentId);
    setLoading(true);
    try {
      const [stats, hist] = await Promise.all([
        api.getStudentFrequency(studentId, filterClassId || undefined, filterStartDate, filterEndDate),
        api.getStudentHistory(studentId, filterClassId || undefined, filterStartDate, filterEndDate),
      ]);
      setStudentStats(stats);
      setStudentHistory(hist);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Cabeçalho */}
      <div>
        <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
          Estatísticas & Frequência
        </h2>
        <p className="text-xs sm:text-sm text-slate-500">
          Métricas consolidadas, evolução mensal, comparativo entre turmas e pendências de chamada
        </p>
      </div>

      {/* Navegação entre Abas */}
      <div className="flex flex-wrap gap-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setActiveTab('comparison')}
          className={`px-4 py-2 text-xs font-bold rounded-xl transition-all ${
            activeTab === 'comparison'
              ? 'bg-brand-600 text-white shadow-sm'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          Comparativo de Turmas
        </button>

        <button
          onClick={() => setActiveTab('evolution')}
          className={`px-4 py-2 text-xs font-bold rounded-xl transition-all ${
            activeTab === 'evolution'
              ? 'bg-brand-600 text-white shadow-sm'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          Evolução Mensal
        </button>

        <button
          onClick={() => setActiveTab('student')}
          className={`px-4 py-2 text-xs font-bold rounded-xl transition-all ${
            activeTab === 'student'
              ? 'bg-brand-600 text-white shadow-sm'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          Histórico por Aluno
        </button>

        <button
          onClick={() => setActiveTab('pending')}
          className={`px-4 py-2 text-xs font-bold rounded-xl transition-all flex items-center gap-1.5 ${
            activeTab === 'pending'
              ? 'bg-amber-600 text-white shadow-sm'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          <Clock className="w-3.5 h-3.5" />
          <span>Chamadas Pendentes</span>
        </button>
      </div>

      {/* Filtros de Período e Turma */}
      <div className="bg-white p-4 border border-slate-200 rounded-2xl shadow-sm">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">Filtrar por Turma</label>
            <select
              value={filterClassId}
              onChange={(e) => setFilterClassId(e.target.value)}
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            >
              <option value="">Todas as turmas</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">A partir de</label>
            <input
              type="date"
              value={filterStartDate}
              onChange={(e) => setFilterStartDate(e.target.value)}
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            />
          </div>

          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">Até a data</label>
            <input
              type="date"
              value={filterEndDate}
              onChange={(e) => setFilterEndDate(e.target.value)}
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            />
          </div>
        </div>
      </div>

      {/* Conteúdo da Aba 1: Comparativo de Turmas */}
      {activeTab === 'comparison' && (
        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-base text-slate-900">
              Desempenho e Presença Média por Turma
            </h3>
            <span className="text-xs text-slate-500">
              Regra: Presenças ÷ Total de Aulas Elegíveis Realizadas × 100
            </span>
          </div>

          {loading ? (
            <div className="p-8 text-center text-slate-400">Carregando comparativo...</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left">
                <thead className="bg-slate-50 text-slate-700 uppercase text-[10px] font-bold border-b border-slate-200">
                  <tr>
                    <th className="p-3">Turma</th>
                    <th className="p-3">Categoria</th>
                    <th className="p-3 text-center">Alunos Ativos</th>
                    <th className="p-3 text-center">Aulas Realizadas</th>
                    <th className="p-3 text-center">Presenças</th>
                    <th className="p-3 text-center">Faltas Sem Just.</th>
                    <th className="p-3 text-center">Faltas Just.</th>
                    <th className="p-3 text-right">Frequência Média</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {classComparison.map((item) => (
                    <tr key={item.classId} className="hover:bg-slate-50/80">
                      <td className="p-3 font-bold text-slate-900">{item.className}</td>
                      <td className="p-3 text-slate-600">{item.category}</td>
                      <td className="p-3 text-center font-semibold text-slate-800">{item.activeStudentsCount}</td>
                      <td className="p-3 text-center font-semibold text-slate-800">{item.totalLessonsCounted}</td>
                      <td className="p-3 text-center text-emerald-700 font-bold">{item.totalPresences}</td>
                      <td className="p-3 text-center text-rose-700 font-bold">{item.totalAbsences}</td>
                      <td className="p-3 text-center text-amber-700 font-bold">{item.totalExcused}</td>
                      <td className="p-3 text-right">
                        {item.averageAttendancePercentage != null ? (
                          <span className={`inline-block font-extrabold px-2.5 py-1 rounded-lg ${
                            item.averageAttendancePercentage >= 75
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-rose-100 text-rose-800'
                          }`}>
                            {item.averageAttendancePercentage}%
                          </span>
                        ) : (
                          <span className="text-slate-400 italic">Sem dados</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Conteúdo da Aba 2: Evolução Mensal */}
      {activeTab === 'evolution' && (
        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4">
          <h3 className="font-bold text-base text-slate-900">
            Evolução Histórica Mês a Mês
          </h3>

          {loading ? (
            <div className="p-8 text-center text-slate-400">Carregando evolução mensal...</div>
          ) : monthlyEvolution.length === 0 ? (
            <p className="text-xs text-slate-500 py-6 text-center">Nenhuma aula finalizada no período selecionado.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left">
                <thead className="bg-slate-50 text-slate-700 uppercase text-[10px] font-bold border-b border-slate-200">
                  <tr>
                    <th className="p-3">Mês / Ano</th>
                    <th className="p-3 text-center">Aulas Realizadas</th>
                    <th className="p-3 text-center">Total Presenças</th>
                    <th className="p-3 text-center">Faltas Sem Justificativa</th>
                    <th className="p-3 text-center">Faltas Justificadas</th>
                    <th className="p-3 text-right">Taxa de Presença (%)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {monthlyEvolution.map((m) => (
                    <tr key={m.monthYear} className="hover:bg-slate-50">
                      <td className="p-3 font-bold text-slate-900">{m.monthYear}</td>
                      <td className="p-3 text-center text-slate-700 font-semibold">{m.totalLessons}</td>
                      <td className="p-3 text-center text-emerald-700 font-bold">{m.presences}</td>
                      <td className="p-3 text-center text-rose-700 font-bold">{m.absences}</td>
                      <td className="p-3 text-center text-amber-700 font-bold">{m.excusedAbsences}</td>
                      <td className="p-3 text-right font-black">
                        {m.attendancePercentage != null ? `${m.attendancePercentage}%` : 'Sem dados'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Conteúdo da Aba 3: Histórico Individual por Aluno */}
      {activeTab === 'student' && (
        <div className="space-y-4">
          <div className="bg-white p-4 border border-slate-200 rounded-2xl shadow-sm">
            <label className="block text-xs font-bold text-slate-700 mb-1">
              Selecione o Aluno para Auditoria de Frequência:
            </label>
            <select
              value={selectedStudentId}
              onChange={(e) => handleStudentSelect(e.target.value)}
              className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-semibold"
            >
              {students.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.fullName} ({s.age} anos)
                </option>
              ))}
            </select>
          </div>

          {studentStats && (
            <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-200">
                <div>
                  <h3 className="text-base font-extrabold text-slate-900">{studentStats.studentName}</h3>
                  <p className="text-xs text-slate-500">
                    Idade Atual: {studentStats.currentAge} anos • Total de Aulas Computadas: {studentStats.totalLessons}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className={`text-base font-black px-3 py-1 rounded-xl ${
                    studentStats.belowThreshold
                      ? 'bg-rose-100 text-rose-800 border border-rose-200'
                      : 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                  }`}>
                    {studentStats.attendancePercentage != null ? `${studentStats.attendancePercentage}% Frequência` : 'Sem dados'}
                  </span>
                </div>
              </div>

              {/* Quebra por turma para alunos com várias turmas (Regra Consolidada) */}
              {studentStats.classBreakdowns && studentStats.classBreakdowns.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold text-slate-700 uppercase mb-2">Desempenho por Turma</h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {studentStats.classBreakdowns.map((cb: any) => (
                      <div key={cb.classId} className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs flex justify-between items-center">
                        <div>
                          <span className="font-bold text-slate-900">{cb.className}</span>
                          <p className="text-[11px] text-slate-500 mt-0.5">
                            {cb.presences} presenças em {cb.totalLessons} aulas ({cb.unexcusedAbsences} faltas)
                          </p>
                        </div>
                        <span className="font-extrabold text-brand-700 bg-white px-2.5 py-1 rounded-lg border border-slate-200 shadow-xs">
                          {cb.attendancePercentage != null ? `${cb.attendancePercentage}%` : 'S/D'}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Histórico aula por aula */}
              <div>
                <h4 className="text-xs font-bold text-slate-700 uppercase mb-2">
                  Histórico Aula a Aula (Idade calculada na data do treino)
                </h4>
                <div className="overflow-x-auto">
                  <table className="w-full text-xs text-left">
                    <thead className="bg-slate-50 text-slate-700 uppercase text-[10px] font-bold border-b border-slate-200">
                      <tr>
                        <th className="p-2.5">Data do Treino</th>
                        <th className="p-2.5">Turma</th>
                        <th className="p-2.5 text-center">Idade na Data</th>
                        <th className="p-2.5 text-center">Situação</th>
                        <th className="p-2.5">Observação / Justificativa</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {studentHistory.map((h, idx) => (
                        <tr key={idx} className="hover:bg-slate-50">
                          <td className="p-2.5 font-bold text-slate-900">
                            {new Date(h.lessonDate + 'T12:00:00').toLocaleDateString('pt-BR')}
                          </td>
                          <td className="p-2.5 text-slate-700">{h.className}</td>
                          <td className="p-2.5 text-center text-slate-600">{h.ageOnLessonDate} anos</td>
                          <td className="p-2.5 text-center">
                            <span className={`inline-block px-2 py-0.5 rounded-full text-[11px] font-bold ${
                              h.status === 'PRESENTE'
                                ? 'bg-emerald-100 text-emerald-800'
                                : h.status === 'AUSENTE'
                                ? 'bg-rose-100 text-rose-800'
                                : 'bg-amber-100 text-amber-800'
                            }`}>
                              {h.status === 'PRESENTE' ? 'Presente' : h.status === 'AUSENTE' ? 'Ausente' : 'Falta Justificada'}
                            </span>
                          </td>
                          <td className="p-2.5 text-slate-500 italic">
                            {h.notes || '-'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Conteúdo da Aba 4: Chamadas Pendentes */}
      {activeTab === 'pending' && (
        <div className="bg-white border border-amber-200 rounded-2xl p-5 shadow-sm space-y-4">
          <div className="flex items-center gap-2 text-amber-800">
            <AlertTriangle className="w-5 h-5 text-amber-600" />
            <h3 className="font-bold text-base">
              Aulas Realizadas sem Chamada Finalizada (Pendências)
            </h3>
          </div>
          <p className="text-xs text-slate-600">
            Aulas cuja data já ocorreu mas a chamada permanece em rascunho ou não foi iniciada. Estas aulas não geram faltas automáticas até a finalização.
          </p>

          {loading ? (
            <div className="p-8 text-center text-slate-400">Carregando pendências...</div>
          ) : pendingLessons.length === 0 ? (
            <div className="p-8 text-center text-emerald-700 bg-emerald-50 rounded-xl border border-emerald-200 font-semibold text-xs">
              ✓ Nenhuma pendência de chamada! Todas as aulas realizadas estão finalizadas.
            </div>
          ) : (
            <div className="space-y-3">
              {pendingLessons.map((l) => (
                <div
                  key={l.id}
                  className="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 bg-amber-50/50 border border-amber-200 rounded-xl gap-3"
                >
                  <div>
                    <span className="font-bold text-slate-900 text-sm">{l.className}</span>
                    <p className="text-xs text-slate-600 mt-0.5">
                      Data: {new Date(l.date + 'T12:00:00').toLocaleDateString('pt-BR')} ({l.startTime.substring(0, 5)} às {l.endTime.substring(0, 5)})
                    </p>
                  </div>
                  <button
                    onClick={() => onNavigate('attendance', { lessonId: l.id })}
                    className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-amber-600 hover:bg-amber-700 rounded-xl shadow-xs self-start sm:self-auto"
                  >
                    <ClipboardCheck className="w-4 h-4" /> Finalizar Chamada Agora
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
