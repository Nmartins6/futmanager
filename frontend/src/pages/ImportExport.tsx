import React, { useState } from 'react';
import { api } from '../services/api';
import { ImportPreviewResponse, ImportRow } from '../types';
import {
  UploadCloud,
  Download,
  FileSpreadsheet,
  FileText,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  ArrowRight,
  ShieldAlert,
  Info
} from 'lucide-react';

export const ImportExport: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [previewData, setPreviewData] = useState<ImportPreviewResponse | null>(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<string | null>(null);
  const [exporting, setExporting] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
      setPreviewData(null);
      setImportResult(null);
    }
  };

  const handlePreview = async () => {
    if (!file) return;
    setLoadingPreview(true);
    setImportResult(null);
    try {
      const res = await api.previewImport(file);
      setPreviewData(res);
    } catch (err: any) {
      alert(err.message || 'Erro ao processar arquivo para pré-visualização.');
    } finally {
      setLoadingPreview(false);
    }
  };

  const handleConfirmImport = async () => {
    if (!previewData) return;
    if (!window.confirm(`Deseja confirmar a importação de ${previewData.validRows} linhas válidas de forma transacional?`)) {
      return;
    }

    setImporting(true);
    try {
      const res = await api.confirmImport(previewData.rows);
      setImportResult(res.message);
      setPreviewData(null);
      setFile(null);
    } catch (err: any) {
      alert(err.message || 'Erro ao efetivar a importação transacional.');
    } finally {
      setImporting(false);
    }
  };

  const handleExport = async (type: 'csv' | 'xlsx') => {
    setExporting(type);
    try {
      if (type === 'csv') {
        await api.exportStudentsCsv();
      } else {
        await api.exportStudentsXlsx();
      }
    } catch (err: any) {
      alert(err.message || 'Erro ao exportar dados.');
    } finally {
      setExporting(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
          Importação & Exportação de Dados
        </h2>
        <p className="text-xs sm:text-sm text-slate-500">
          Importação em lote de alunos e responsáveis com pré-visualização, validação por linha e proteção contra fórmula injection
        </p>
      </div>

      {/* Seção 1: Download de Modelos */}
      <div className="bg-white p-5 border border-slate-200 rounded-2xl shadow-sm space-y-4">
        <div className="flex items-center gap-2.5 text-slate-900 font-bold text-base">
          <Download className="w-5 h-5 text-brand-600" />
          <span>1. Modelos Oficiais de Planilha para Preenchimento</span>
        </div>

        <p className="text-xs text-slate-600 leading-relaxed">
          Baixe um dos modelos abaixo contendo os cabeçalhos esperados e uma linha de exemplo.
          Preencha com os dados dos seus alunos e responsáveis e depois faça o upload na etapa seguinte.
        </p>

        <div className="flex flex-wrap gap-3 pt-1">
          <button
            onClick={() => api.downloadCsvTemplate()}
            className="flex items-center gap-2 px-4 py-2 text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
          >
            <FileText className="w-4 h-4 text-slate-500" /> Baixar Modelo CSV
          </button>
          <button
            onClick={() => api.downloadXlsxTemplate()}
            className="flex items-center gap-2 px-4 py-2 text-xs font-bold text-emerald-800 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-xl transition-colors"
          >
            <FileSpreadsheet className="w-4 h-4 text-emerald-600" /> Baixar Modelo XLSX (Excel)
          </button>
        </div>
      </div>

      {/* Seção 2: Upload e Pré-Visualização */}
      <div className="bg-white p-5 border border-slate-200 rounded-2xl shadow-sm space-y-4">
        <div className="flex items-center gap-2.5 text-slate-900 font-bold text-base">
          <UploadCloud className="w-5 h-5 text-brand-600" />
          <span>2. Carregar Planilha para Pré-Visualização & Validação</span>
        </div>

        <div className="p-4 bg-slate-50 border border-dashed border-slate-300 rounded-2xl flex flex-col sm:flex-row items-center justify-between gap-4">
          <input
            type="file"
            accept=".csv, .xlsx"
            onChange={handleFileChange}
            className="text-xs text-slate-600 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-brand-50 file:text-brand-700 hover:file:bg-brand-100"
          />

          <button
            onClick={handlePreview}
            disabled={!file || loadingPreview}
            className="px-5 py-2.5 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow transition-all disabled:opacity-50"
          >
            {loadingPreview ? 'Validando Planilha...' : 'Analisar e Pré-Visualizar'}
          </button>
        </div>

        {importResult && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs font-bold flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600" />
            <span>{importResult}</span>
          </div>
        )}

        {/* Tabela de Pré-visualização com Validação Linha a Linha */}
        {previewData && (
          <div className="space-y-4 pt-2">
            <div className="flex flex-wrap items-center justify-between gap-3 p-4 bg-slate-50 border border-slate-200 rounded-xl text-xs">
              <div className="flex items-center gap-3">
                <span className="font-semibold text-slate-700">Total: <strong>{previewData.totalRows}</strong> linhas</span>
                <span className="font-semibold text-emerald-700">Válidas: <strong>{previewData.validRows}</strong></span>
                {previewData.invalidRows > 0 && (
                  <span className="font-semibold text-rose-700">Inválidas: <strong>{previewData.invalidRows}</strong></span>
                )}
                {previewData.duplicateWarningsCount > 0 && (
                  <span className="font-semibold text-amber-700">Avisos de Homônimo: <strong>{previewData.duplicateWarningsCount}</strong></span>
                )}
              </div>

              <button
                onClick={handleConfirmImport}
                disabled={importing || previewData.validRows === 0}
                className="flex items-center gap-1.5 px-5 py-2 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl shadow transition-all disabled:opacity-50"
              >
                <span>{importing ? 'Importando...' : 'Confirmar e Efetivar Importação'}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>

            <div className="overflow-x-auto max-h-96 rounded-xl border border-slate-200">
              <table className="w-full text-xs text-left">
                <thead className="bg-slate-100 text-slate-700 uppercase text-[10px] font-bold sticky top-0 border-b border-slate-200">
                  <tr>
                    <th className="p-2.5 text-center">Linha</th>
                    <th className="p-2.5">Aluno</th>
                    <th className="p-2.5">Data Nasc.</th>
                    <th className="p-2.5">Responsável</th>
                    <th className="p-2.5">Telefone</th>
                    <th className="p-2.5">Turma</th>
                    <th className="p-2.5">Status Validação</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {previewData.rows.map((row) => (
                    <tr
                      key={row.lineNumber}
                      className={`hover:bg-slate-50 ${
                        !row.valid ? 'bg-rose-50/40' : row.possibleDuplicate ? 'bg-amber-50/40' : ''
                      }`}
                    >
                      <td className="p-2.5 text-center font-bold text-slate-600">{row.lineNumber}</td>
                      <td className="p-2.5 font-bold text-slate-900">{row.studentName}</td>
                      <td className="p-2.5">{row.birthDateStr}</td>
                      <td className="p-2.5">{row.guardianName} ({row.relationship})</td>
                      <td className="p-2.5">{row.guardianPhone}</td>
                      <td className="p-2.5">{row.className || '-'}</td>
                      <td className="p-2.5">
                        {row.valid ? (
                          <div className="space-y-0.5">
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                              <CheckCircle2 className="w-3 h-3 text-emerald-600" /> Válida
                            </span>
                            {row.possibleDuplicate && (
                              <p className="text-[10px] text-amber-700 font-semibold">{row.duplicateReason}</p>
                            )}
                          </div>
                        ) : (
                          <div className="text-[11px] text-rose-700 font-bold space-y-0.5">
                            <span className="inline-flex items-center gap-1 bg-rose-100 px-2 py-0.5 rounded-full">
                              <XCircle className="w-3 h-3 text-rose-600" /> Inválida
                            </span>
                            <ul className="list-disc list-inside text-[10px] font-normal text-rose-800">
                              {row.errors.map((err, i) => (
                                <li key={i}>{err}</li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Seção 3: Exportação Operacional com Proteção Contra Injeção de Fórmulas */}
      <div className="bg-white p-5 border border-slate-200 rounded-2xl shadow-sm space-y-4">
        <div className="flex items-center gap-2.5 text-slate-900 font-bold text-base">
          <FileSpreadsheet className="w-5 h-5 text-emerald-600" />
          <span>3. Exportação dos Dados do Sistema</span>
        </div>

        <p className="text-xs text-slate-600 leading-relaxed">
          Exporte os cadastros de alunos com suas turmas ativas. As exportações contam com proteção nativa contra injeção de fórmulas maliciosas (CSV Injection).
        </p>

        <div className="flex flex-wrap gap-3 pt-1">
          <button
            onClick={() => handleExport('csv')}
            disabled={exporting === 'csv'}
            className="flex items-center gap-2 px-4 py-2.5 text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors disabled:opacity-50"
          >
            <Download className="w-4 h-4" />
            <span>{exporting === 'csv' ? 'Exportando CSV...' : 'Exportar Alunos em CSV'}</span>
          </button>

          <button
            onClick={() => handleExport('xlsx')}
            disabled={exporting === 'xlsx'}
            className="flex items-center gap-2 px-4 py-2.5 text-xs font-bold text-emerald-800 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-xl transition-colors disabled:opacity-50"
          >
            <Download className="w-4 h-4" />
            <span>{exporting === 'xlsx' ? 'Exportando XLSX...' : 'Exportar Alunos em Planilha XLSX'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
