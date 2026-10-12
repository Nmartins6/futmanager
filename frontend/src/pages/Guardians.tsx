import React, { useState, useEffect } from 'react';
import { api, isValidEmail } from '../services/api';
import { Guardian } from '../types';
import { useAuth } from '../context/AuthContext';
import {
  HeartHandshake,
  Search,
  Phone,
  Mail,
  Users,
  Plus,
  Edit2,
  AlertCircle,
  X
} from 'lucide-react';

export const Guardians: React.FC = () => {
  const { isAdmin } = useAuth();
  const [guardians, setGuardians] = useState<Guardian[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Modal
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadGuardians(page);
  }, [page]);

  // Busca instantânea com debounce
  useEffect(() => {
    const timer = setTimeout(() => {
      if (page === 0) {
        loadGuardians(0);
      } else {
        setPage(0);
      }
    }, 350);
    return () => clearTimeout(timer);
  }, [search]);

  const loadGuardians = async (targetPage = page) => {
    setLoading(true);
    try {
      const q = new URLSearchParams();
      if (search.trim()) q.append('query', search.trim());
      q.append('page', targetPage.toString());
      q.append('size', '12');

      const res = await api.getGuardians(q.toString());
      setGuardians(res.content || []);
      setTotalPages(res.totalPages || 0);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    loadGuardians(0);
  };

  const handleClearSearch = () => {
    setSearch('');
    setPage(0);
    loadGuardians(0);
  };

  const handleOpenCreate = () => {
    setEditingId(null);
    setFullName('');
    setPhone('');
    setEmail('');
    setFormError(null);
    setShowModal(true);
  };

  const handleOpenEdit = async (g: Guardian) => {
    setEditingId(g.id);
    setFullName(g.fullName);
    setPhone(g.phone);
    setEmail(g.email || '');
    setFormError(null);
    setShowModal(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setFormError(null);

    if (email.trim() && !isValidEmail(email)) {
      setFormError('Por favor, informe um formato de e-mail válido (exemplo: responsavel@email.com).');
      setSaving(false);
      return;
    }

    const payload = {
      fullName: fullName.trim(),
      phone: phone.trim(),
      email: email.trim() || null,
    };

    try {
      if (editingId) {
        await api.updateGuardian(editingId, payload);
      } else {
        await api.createGuardian(payload);
      }
      setShowModal(false);
      loadGuardians();
    } catch (err: any) {
      setFormError(err.message || 'Erro ao salvar responsável.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
            Responsáveis & Contatos
          </h2>
          <p className="text-xs sm:text-sm text-slate-500">
            Telefones de emergência, WhatsApp e alunos vinculados
          </p>
        </div>
        {isAdmin && (
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-4 py-2.5 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-md transition-all self-start sm:self-auto"
          >
            <Plus className="w-4 h-4" /> Novo Responsável
          </button>
        )}
      </div>

      <div className="bg-white p-4 border border-slate-200 rounded-2xl shadow-sm">
        <form onSubmit={handleSearchSubmit} className="flex gap-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Pesquisar por nome, telefone ou e-mail..."
              className="w-full pl-10 pr-10 py-2 text-xs bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            {search && (
              <button
                type="button"
                onClick={handleClearSearch}
                className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
          <button
            type="submit"
            className="px-4 py-2 text-xs font-bold text-white bg-slate-800 rounded-xl hover:bg-slate-700 shadow-sm"
          >
            Buscar
          </button>
        </form>
      </div>

      {loading ? (
        <div className="p-12 text-center text-slate-500">
          <div className="w-8 h-8 border-3 border-brand-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs">Carregando responsáveis...</p>
        </div>
      ) : guardians.length === 0 ? (
        <div className="bg-white p-12 text-center border border-slate-200 rounded-2xl">
          <HeartHandshake className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-700">Nenhum responsável encontrado</h3>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {guardians.map((g) => (
            <div
              key={g.id}
              className="bg-white border border-slate-200 rounded-2xl p-4 shadow-sm flex flex-col justify-between hover:border-slate-300 transition-all"
            >
              <div>
                <div className="flex items-start justify-between gap-2 mb-2">
                  <h4 className="font-bold text-slate-900 text-sm">{g.fullName}</h4>
                  <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 shrink-0">
                    {g.linkedStudentsCount} {g.linkedStudentsCount === 1 ? 'aluno' : 'alunos'}
                  </span>
                </div>

                <div className="space-y-1.5 text-xs text-slate-600 pt-2 border-t border-slate-100">
                  <p className="flex items-center gap-2">
                    <Phone className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    <a
                      href={`https://wa.me/55${g.phone.replace(/\D/g, '')}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-slate-800 hover:text-emerald-700 hover:underline font-semibold"
                    >
                      {g.phone}
                    </a>
                  </p>
                  {g.email && (
                    <p className="flex items-center gap-2 truncate">
                      <Mail className="w-3.5 h-3.5 text-blue-600 shrink-0" />
                      <span>{g.email}</span>
                    </p>
                  )}
                </div>
              </div>

              {isAdmin && (
                <div className="flex justify-end pt-3 border-t border-slate-100 mt-3">
                  <button
                    type="button"
                    onClick={() => handleOpenEdit(g)}
                    className="p-1.5 text-slate-600 hover:text-brand-600 hover:bg-slate-100 rounded-lg"
                    title="Editar responsável"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-4">
          <button
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
            className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-slate-200 bg-white disabled:opacity-40"
          >
            Anterior
          </button>
          <span className="text-xs text-slate-600 font-medium">Página {page + 1} de {totalPages}</span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => setPage(page + 1)}
            className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-slate-200 bg-white disabled:opacity-40"
          >
            Próxima
          </button>
        </div>
      )}

      {/* Modal Criar/Editar Responsável */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-md bg-white p-6 rounded-2xl shadow-xl space-y-4">
            <h3 className="font-bold text-base text-slate-900">
              {editingId ? 'Editar Responsável' : 'Novo Responsável'}
            </h3>
            {formError && (
              <p className="text-xs text-rose-600 bg-rose-50 p-2.5 rounded-lg border border-rose-200">
                {formError}
              </p>
            )}
            <form onSubmit={handleSave} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Nome Completo *</label>
                <input
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Ex: Patrícia Mendes de Souza"
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Telefone / WhatsApp *</label>
                <input
                  type="text"
                  required
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="(11) 98765-4321"
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">E-mail (opcional)</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="exemplo@email.com"
                  className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-5 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow"
                >
                  {saving ? 'Gravando...' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
