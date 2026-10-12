import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { SchoolConfig } from '../types';
import { Settings as SettingsIcon, Save, CheckCircle2, AlertCircle } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const [config, setConfig] = useState<SchoolConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Campos
  const [schoolName, setSchoolName] = useState('');
  const [cnpj, setCnpj] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [timezone, setTimezone] = useState('America/Sao_Paulo');
  const [horizonDays, setHorizonDays] = useState(30);
  const [threshold, setThreshold] = useState(75.0);

  useEffect(() => {
    loadConfigs();
  }, []);

  const loadConfigs = async () => {
    setLoading(true);
    try {
      const res = await api.getConfigs();
      setConfig(res);
      setSchoolName(res.schoolName);
      setCnpj(res.cnpj || '');
      setPhone(res.phone || '');
      setAddress(res.address || '');
      setTimezone(res.timezone);
      setHorizonDays(res.lessonHorizonDays);
      setThreshold(res.lowFrequencyThreshold);
    } catch (err: any) {
      setErrorMsg(err.message || 'Erro ao carregar configurações.');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSuccessMsg(null);
    setErrorMsg(null);

    const payload = {
      schoolName: schoolName.trim(),
      cnpj: cnpj.trim() || null,
      phone: phone.trim() || null,
      address: address.trim() || null,
      timezone: timezone.trim(),
      lessonHorizonDays: horizonDays,
      lowFrequencyThreshold: threshold,
    };

    try {
      const updated = await api.updateConfigs(payload);
      setConfig(updated);
      setSuccessMsg('Configurações da escolinha salvas com sucesso!');
    } catch (err: any) {
      setErrorMsg(err.message || 'Erro ao salvar configurações.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="w-8 h-8 border-3 border-brand-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
          Configurações da Escolinha
        </h2>
        <p className="text-xs sm:text-sm text-slate-500">
          Dados cadastrais para cabeçalhos de relatórios, fuso horário padrão e parâmetros de chamada
        </p>
      </div>

      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl text-xs font-semibold flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-2xl text-xs font-semibold flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      <form onSubmit={handleSave} className="bg-white p-6 border border-slate-200 rounded-2xl shadow-sm space-y-5">
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-bold text-slate-800 mb-1">Nome da Escolinha *</label>
            <input
              type="text"
              required
              value={schoolName}
              onChange={(e) => setSchoolName(e.target.value)}
              className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-slate-800 mb-1">CNPJ</label>
              <input
                type="text"
                value={cnpj}
                onChange={(e) => setCnpj(e.target.value)}
                placeholder="12.345.678/0001-90"
                className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-800 mb-1">Telefone de Contato</label>
              <input
                type="text"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="(11) 3456-7890"
                className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
              />
            </div>
          </div>

          <div>
            <label className="block font-bold text-slate-800 mb-1">Endereço Completo</label>
            <input
              type="text"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder="Rua dos Esportes, 100 - Centro Esportivo Municipal, São Paulo - SP"
              className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-3 border-t border-slate-200">
            <div>
              <label className="block font-bold text-slate-800 mb-1">Fuso Horário Padrão *</label>
              <select
                value={timezone}
                onChange={(e) => setTimezone(e.target.value)}
                className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl font-mono text-xs"
              >
                <option value="America/Sao_Paulo">America/Sao_Paulo (Horário de Brasília)</option>
                <option value="America/Manaus">America/Manaus (UTC-4)</option>
                <option value="America/Fortaleza">America/Fortaleza (UTC-3)</option>
                <option value="America/Cuiaba">America/Cuiaba (UTC-4)</option>
              </select>
            </div>

            <div>
              <label className="block font-bold text-slate-800 mb-1">
                Horizonte Geração de Aulas (Dias) *
              </label>
              <input
                type="number"
                min={7}
                max={90}
                required
                value={horizonDays}
                onChange={(e) => setHorizonDays(parseInt(e.target.value))}
                className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
              />
              <span className="text-[10px] text-slate-400 mt-1 block">Dias à frente para gerar aulas</span>
            </div>

            <div>
              <label className="block font-bold text-slate-800 mb-1">
                Limite Baixa Frequência (%) *
              </label>
              <input
                type="number"
                min={10}
                max={99}
                step={0.5}
                required
                value={threshold}
                onChange={(e) => setThreshold(parseFloat(e.target.value))}
                className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
              />
              <span className="text-[10px] text-slate-400 mt-1 block">Alerta quando freq. &lt; limite</span>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-3">
          <button
            type="submit"
            disabled={saving}
            className="flex items-center gap-2 px-6 py-2.5 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow transition-all disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            <span>{saving ? 'Gravando...' : 'Salvar Configurações'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};
