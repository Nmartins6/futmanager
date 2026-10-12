import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { SchoolClass, StudentSummary } from '../types';
import {
  FileSpreadsheet,
  FileText,
  Download,
  Calendar,
  Users,
  GraduationCap,
  AlertTriangle,
  FileCheck
} from 'lucide-react';

export const Reports: React.FC = () => {
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [students, setStudents] = useState<StudentSummary[]>([]);
  const [selectedClassId, setSelectedClassId] = useState('');
  const [selectedStudentId, setSelectedStudentId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [downloading, setDownloading] = useState<string | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [cls, stus] = await Promise.all([
        api.getAllActiveClasses(),
        api.getStudents('size=100'),
      ]);
      setClasses(cls);
      setStudents(stus.content || []);
      if (cls.length > 0) setSelectedClassId(cls[0].id);
      if (stus.content?.length > 0) setSelectedStudentId(stus.content[0].id);
    } catch (e) {
      console.error(e);
    }
  };

  const handleDownload = async (type: string, fn: () => Promise<void>) => {
    setDownloading(type);
    try {
      await fn();
    } catch (err: any) {
      alert(err.message || 'Erro ao gerar relatório.');
    } finally {
      setDownloading(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
          Central de Relatórios
        </h2>
        <p className="text-xs sm:text-sm text-slate-500">
          Emissão oficial em PDF e planilhas XLSX com cabeçalho institucional, filtros e data de geração
        </p>
      </div>

      {/* Painel de Filtros para os Relatórios */}
      <div className="bg-white p-5 border border-slate-200 rounded-2xl shadow-sm space-y-3">
        <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
          Filtros Aplicáveis aos Relatórios
        </h3>
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 text-xs">
          <div>
            <label className="block font-bold text-slate-700 mb-1">Turma</label>
            <select
              value={selectedClassId}
              onChange={(e) => setSelectedClassId(e.target.value)}
              className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
            >
              {classes.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-bold text-slate-700 mb-1">Aluno (para ficha individual)</label>
            <select
              value={selectedStudentId}
              onChange={(e) => setSelectedStudentId(e.target.value)}
              className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
            >
              {students.map((s) => (
                <option key={s.id} value={s.id}>{s.fullName} ({s.age} anos)</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-bold text-slate-700 mb-1">Data Inicial</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
            />
          </div>

          <div>
            <label className="block font-bold text-slate-700 mb-1">Data Final</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
            />
          </div>
        </div>
      </div>

      {/* Grid de Cartões de Relatórios para Download */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Relatório 1: Frequência por Turma (PDF) */}
        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col justify-between hover:border-brand-300 transition-all">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2.5 bg-rose-50 text-rose-600 rounded-xl">
                <FileText className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-slate-900 text-sm">Relatório de Frequência por Turma</h4>
                <span className="text-[10px] font-bold uppercase tracking-wider text-rose-700 bg-rose-100 px-2 py-0.5 rounded-full">
                  Formato PDF
                </span>
              </div>
            </div>
            <p className="text-xs text-slate-600 mt-2 leading-relaxed">
              Lista nominal dos alunos da turma selecionada com total de aulas elegíveis, presenças, faltas justificadas e percentual calculado.
            </p>
          </div>

          <button
            onClick={() =>
              handleDownload('class-pdf', () =>
                api.downloadClassFrequencyPdf(selectedClassId, startDate || undefined, endDate || undefined)
              )
            }
            disabled={downloading === 'class-pdf' || !selectedClassId}
            className="mt-4 flex items-center justify-center gap-2 py-2.5 px-4 text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-xl transition-colors disabled:opacity-50"
          >
            <Download className="w-4 h-4" />
            <span>{downloading === 'class-pdf' ? 'Gerando PDF...' : 'Baixar PDF da Turma'}</span>
          </button>
        </div>

        {/* Relatório 2: Ficha Individual do Aluno (PDF) */}
        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col justify-between hover:border-brand-300 transition-all">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2.5 bg-blue-50 text-blue-600 rounded-xl">
                <GraduationCap className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-slate-900 text-sm">Ficha Individual de Frequência</h4>
                <span className="text-[10px] font-bold uppercase tracking-wider text-blue-700 bg-blue-100 px-2 py-0.5 rounded-full">
                  Formato PDF
                </span>
              </div>
            </div>
            <p className="text-xs text-slate-600 mt-2 leading-relaxed">
              Ficha detalhada com dados do aluno, histórico aula a aula com idade na data da aula, justificativas registradas e frequência consolidada.
            </p>
          </div>

          <button
            onClick={() =>
              handleDownload('student-pdf', () =>
                api.downloadStudentIndividualPdf(selectedStudentId, selectedClassId || undefined, startDate || undefined, endDate || undefined)
              )
            }
            disabled={downloading === 'student-pdf' || !selectedStudentId}
            className="mt-4 flex items-center justify-center gap-2 py-2.5 px-4 text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-xl transition-colors disabled:opacity-50"
          >
            <Download className="w-4 h-4" />
            <span>{downloading === 'student-pdf' ? 'Gerando Ficha...' : 'Baixar Ficha do Aluno (PDF)'}</span>
          </button>
        </div>

        {/* Relatório 3: Alunos com Baixa Frequência (PDF) */}
        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col justify-between hover:border-brand-300 transition-all">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2.5 bg-amber-50 text-amber-600 rounded-xl">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-slate-900 text-sm">Alunos em Alerta de Baixa Frequência</h4>
                <span className="text-[10px] font-bold uppercase tracking-wider text-amber-800 bg-amber-100 px-2 py-0.5 rounded-full">
                  Formato PDF
                </span>
              </div>
            </div>
            <p className="text-xs text-slate-600 mt-2 leading-relaxed">
              Relatório de auditoria com alunos que estão abaixo do percentual configurado (&lt; 75%), detalhando faltas sem justificativa.
            </p>
          </div>

          <button
            onClick={() =>
              handleDownload('low-freq-pdf', () =>
                api.downloadLowFrequencyPdf(selectedClassId || undefined, startDate || undefined, endDate || undefined)
              )
            }
            disabled={downloading === 'low-freq-pdf'}
            className="mt-4 flex items-center justify-center gap-2 py-2.5 px-4 text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-xl transition-colors disabled:opacity-50"
          >
            <Download className="w-4 h-4" />
            <span>{downloading === 'low-freq-pdf' ? 'Gerando Relatório...' : 'Baixar Alunos em Alerta (PDF)'}</span>
          </button>
        </div>

        {/* Relatório 4: Resumo Mensal (Planilha XLSX) */}
        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col justify-between hover:border-brand-300 transition-all">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-xl">
                <FileSpreadsheet className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-slate-900 text-sm">Resumo Mensal Consolidado</h4>
                <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded-full">
                  Formato Excel XLSX
                </span>
              </div>
            </div>
            <p className="text-xs text-slate-600 mt-2 leading-relaxed">
              Planilha com totalização mês a mês de aulas realizadas, presenças, faltas e percentuais médios para controle administrativo.
            </p>
          </div>

          <button
            onClick={() =>
              handleDownload('monthly-xlsx', () =>
                api.downloadMonthlySummaryXlsx(selectedClassId || undefined)
              )
            }
            disabled={downloading === 'monthly-xlsx'}
            className="mt-4 flex items-center justify-center gap-2 py-2.5 px-4 text-xs font-bold text-white bg-emerald-700 hover:bg-emerald-800 rounded-xl transition-colors disabled:opacity-50"
          >
            <Download className="w-4 h-4" />
            <span>{downloading === 'monthly-xlsx' ? 'Gerando XLSX...' : 'Baixar Planilha Excel (XLSX)'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
