import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { StudentSummary, StudentDetail, StudentStatus, SchoolClass } from '../types';
import { useAuth } from '../context/AuthContext';
import { GeneralStatusBadge } from '../components/StatusBadge';
import {
  GraduationCap,
  Search,
  Plus,
  Filter,
  Phone,
  Mail,
  Calendar,
  User,
  Users,
  Eye,
  Edit2,
  PowerOff,
  Power,
  Upload,
  AlertCircle
} from 'lucide-react';

export const Students: React.FC = () => {
  const { isAdmin } = useAuth();
  const [students, setStudents] = useState<StudentSummary[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [classFilter, setClassFilter] = useState<string>('');
  const [minAge, setMinAge] = useState<string>('');
  const [maxAge, setMaxAge] = useState<string>('');

  // Paginação
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Ficha detalhada
  const [selectedStudent, setSelectedStudent] = useState<StudentDetail | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);

  // Modal de Cadastro/Edição
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formName, setFormName] = useState('');
  const [formBirthDate, setFormBirthDate] = useState('');
  const [formEnrollDate, setFormEnrollDate] = useState(new Date().toISOString().substring(0, 10));
  const [formStatus, setFormStatus] = useState<StudentStatus>('ATIVO');
  const [formNotes, setFormNotes] = useState('');
  const [formPhotoUrl, setFormPhotoUrl] = useState('');
  const [formSelectedClasses, setFormSelectedClasses] = useState<string[]>([]);

  // Responsável do aluno
  const [guardianName, setGuardianName] = useState('');
  const [guardianPhone, setGuardianPhone] = useState('');
  const [guardianEmail, setGuardianEmail] = useState('');
  const [guardianRel, setGuardianRel] = useState('Mãe');

  const [formError, setFormError] = useState<string | null>(null);
  const [formSaving, setFormSaving] = useState(false);

  useEffect(() => {
    loadClasses();
  }, []);

  useEffect(() => {
    loadStudents();
  }, [page, statusFilter, classFilter]);

  const loadClasses = async () => {
    try {
      const res = await api.getAllActiveClasses();
      setClasses(res);
    } catch (e) {
      console.error(e);
    }
  };

  const loadStudents = async () => {
    setLoading(true);
    try {
      const q = new URLSearchParams();
      if (search) q.append('query', search);
      if (statusFilter) q.append('status', statusFilter);
      if (classFilter) q.append('classId', classFilter);
      if (minAge) q.append('minAge', minAge);
      if (maxAge) q.append('maxAge', maxAge);
      q.append('page', page.toString());
      q.append('size', '12');

      const res = await api.getStudents(q.toString());
      setStudents(res.content || []);
      setTotalPages(res.totalPages || 0);
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    loadStudents();
  };

  const handleViewDetails = async (id: string) => {
    setLoadingDetail(true);
    try {
      const detail = await api.getStudentById(id);
      setSelectedStudent(detail);
    } catch (err: any) {
      alert(err.message || 'Erro ao carregar detalhes do aluno.');
    } finally {
      setLoadingDetail(false);
    }
  };

  const handleOpenCreate = () => {
    setEditingId(null);
    setFormName('');
    setFormBirthDate('');
    setFormEnrollDate(new Date().toISOString().substring(0, 10));
    setFormStatus('ATIVO');
    setFormNotes('');
    setFormPhotoUrl('');
    setFormSelectedClasses([]);
    setGuardianName('');
    setGuardianPhone('');
    setGuardianEmail('');
    setGuardianRel('Mãe');
    setFormError(null);
    setShowModal(true);
  };

  const handleOpenEdit = async (id: string) => {
    setEditingId(id);
    setFormError(null);
    try {
      const detail = await api.getStudentById(id);
      setFormName(detail.fullName);
      setFormBirthDate(detail.birthDate);
      setFormEnrollDate(detail.enrollmentDate);
      setFormStatus(detail.status);
      setFormNotes(detail.notes || '');
      setFormPhotoUrl(detail.photoUrl || '');
      setFormSelectedClasses(detail.enrollments ? detail.enrollments.map((e: any) => e.classId) : []);

      if (detail.guardians && detail.guardians.length > 0) {
        const g = detail.guardians[0];
        setGuardianName(g.fullName);
        setGuardianPhone(g.phone);
        setGuardianEmail(g.email || '');
        setGuardianRel(g.relationship);
      } else {
        setGuardianName('');
        setGuardianPhone('');
        setGuardianEmail('');
        setGuardianRel('Mãe');
      }

      setShowModal(true);
    } catch (err: any) {
      alert(err.message || 'Erro ao abrir edição.');
    }
  };

  const handleToggleStatus = async (id: string) => {
    if (!window.confirm('Deseja realmente alterar a situação deste aluno?')) return;
    try {
      await api.toggleStudentStatus(id);
      loadStudents();
      if (selectedStudent?.id === id) {
        handleViewDetails(id);
      }
    } catch (err: any) {
      alert(err.message || 'Erro ao alterar situação.');
    }
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const url = await api.uploadPhoto(file);
      setFormPhotoUrl(url);
    } catch (err: any) {
      alert(err.message || 'Falha no upload da foto.');
    }
  };

  const handleSaveStudent = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormSaving(true);
    setFormError(null);

    // Validação de data no futuro
    if (new Date(formBirthDate) > new Date()) {
      setFormError('A data de nascimento não pode estar no futuro.');
      setFormSaving(false);
      return;
    }

    // Se informou responsável, o telefone é obrigatório e único
    if (guardianName.trim() && !guardianPhone.trim()) {
      setFormError('O telefone/WhatsApp do responsável é obrigatório.');
      setFormSaving(false);
      return;
    }

    const payload: any = {
      fullName: formName.trim(),
      birthDate: formBirthDate,
      enrollmentDate: formEnrollDate,
      status: formStatus,
      notes: formNotes,
      photoUrl: formPhotoUrl || null,
      guardians: guardianName.trim()
        ? [
            {
              guardianFullName: guardianName.trim(),
              guardianPhone: guardianPhone.trim(),
              guardianEmail: guardianEmail.trim() || null,
              relationship: guardianRel,
              mainContact: true,
              emergencyContact: true,
            },
          ]
        : [],
      classIds: formSelectedClasses,
    };

    try {
      if (editingId) {
        await api.updateStudent(editingId, payload);
      } else {
        await api.createStudent(payload);
      }
      setShowModal(false);
      loadStudents();
    } catch (err: any) {
      setFormError(err.message || 'Erro ao salvar aluno.');
    } finally {
      setFormSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Cabeçalho da Página */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
            Gestão de Alunos
          </h2>
          <p className="text-xs sm:text-sm text-slate-500">
            Fichas cadastrais, idades calculadas, turmas e contatos dos responsáveis
          </p>
        </div>
        {isAdmin && (
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-4 py-2.5 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-md transition-all self-start sm:self-auto"
          >
            <Plus className="w-4 h-4" /> Novo Aluno
          </button>
        )}
      </div>

      {/* Barra de Filtros e Pesquisa */}
      <div className="bg-white p-4 border border-slate-200 rounded-2xl shadow-sm space-y-3">
        <form onSubmit={handleSearchSubmit} className="flex flex-col sm:flex-row gap-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar por nome do aluno ou responsável..."
              className="w-full pl-10 pr-4 py-2 text-xs bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <button
            type="submit"
            className="px-4 py-2 text-xs font-bold text-white bg-slate-800 rounded-xl hover:bg-slate-700"
          >
            Pesquisar
          </button>
        </form>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1 border-t border-slate-100 text-xs">
          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">Situação</label>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            >
              <option value="">Todas as situações</option>
              <option value="ATIVO">Apenas Ativos</option>
              <option value="INATIVO">Apenas Inativos</option>
            </select>
          </div>

          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">Turma</label>
            <select
              value={classFilter}
              onChange={(e) => {
                setClassFilter(e.target.value);
                setPage(0);
              }}
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            >
              <option value="">Todas as turmas</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">Idade Mínima</label>
            <input
              type="number"
              value={minAge}
              onChange={(e) => setMinAge(e.target.value)}
              placeholder="Ex: 7"
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            />
          </div>

          <div>
            <label className="block text-[10px] font-semibold text-slate-500 uppercase mb-1">Idade Máxima</label>
            <input
              type="number"
              value={maxAge}
              onChange={(e) => setMaxAge(e.target.value)}
              placeholder="Ex: 13"
              className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
            />
          </div>
        </div>
      </div>

      {/* Grid de Alunos */}
      {loading ? (
        <div className="p-12 text-center text-slate-500">
          <div className="w-8 h-8 border-3 border-brand-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs">Carregando alunos...</p>
        </div>
      ) : students.length === 0 ? (
        <div className="bg-white p-12 text-center border border-slate-200 rounded-2xl">
          <GraduationCap className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-700">Nenhum aluno encontrado</h3>
          <p className="text-xs text-slate-500 mt-1">Tente ajustar os filtros ou cadastrar um novo aluno.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {students.map((student) => (
            <div
              key={student.id}
              className="bg-white border border-slate-200 hover:border-slate-300 rounded-2xl p-4 shadow-sm flex flex-col justify-between transition-all"
            >
              <div>
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="flex items-center gap-3">
                    {student.photoUrl ? (
                      <img
                        src={student.photoUrl}
                        alt={student.fullName}
                        className="w-12 h-12 rounded-xl object-cover border border-slate-200 shadow-sm shrink-0"
                      />
                    ) : (
                      <div className="flex items-center justify-center w-12 h-12 rounded-xl bg-slate-100 text-slate-700 font-extrabold text-base shrink-0">
                        {student.fullName.substring(0, 2).toUpperCase()}
                      </div>
                    )}
                    <div>
                      <h4 className="font-bold text-slate-900 text-sm leading-tight line-clamp-1">
                        {student.fullName}
                      </h4>
                      <p className="text-xs font-semibold text-brand-600 mt-0.5">
                        {student.age} anos ({new Date(student.birthDate + 'T12:00:00').toLocaleDateString('pt-BR')})
                      </p>
                    </div>
                  </div>
                  <GeneralStatusBadge status={student.status} />
                </div>

                <div className="space-y-1 text-xs text-slate-600 pt-2 border-t border-slate-100">
                  <p className="flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>Ingresso: {new Date(student.enrollmentDate + 'T12:00:00').toLocaleDateString('pt-BR')}</span>
                  </p>
                  <p className="flex items-center gap-1.5">
                    <Users className="w-3.5 h-3.5 text-slate-400" />
                    <span className="truncate">
                      Turmas: {student.currentClasses?.length > 0 ? student.currentClasses.join(', ') : 'Nenhuma'}
                    </span>
                  </p>
                </div>
              </div>

              {/* Ações */}
              <div className="flex items-center justify-end gap-1.5 mt-4 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => handleViewDetails(student.id)}
                  className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                >
                  <Eye className="w-3.5 h-3.5" /> Ficha
                </button>
                {isAdmin && (
                  <>
                    <button
                      type="button"
                      onClick={() => handleOpenEdit(student.id)}
                      className="p-1.5 text-slate-600 hover:text-brand-600 hover:bg-slate-100 rounded-lg transition-colors"
                      title="Editar"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleToggleStatus(student.id)}
                      className={`p-1.5 rounded-lg transition-colors ${
                        student.status === 'ATIVO'
                          ? 'text-slate-400 hover:text-rose-600 hover:bg-rose-50'
                          : 'text-slate-400 hover:text-emerald-600 hover:bg-emerald-50'
                      }`}
                      title={student.status === 'ATIVO' ? 'Inativar Aluno' : 'Ativar Aluno'}
                    >
                      {student.status === 'ATIVO' ? (
                        <PowerOff className="w-4 h-4" />
                      ) : (
                        <Power className="w-4 h-4" />
                      )}
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Paginação */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-4">
          <button
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
            className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-slate-200 bg-white disabled:opacity-40"
          >
            Anterior
          </button>
          <span className="text-xs text-slate-600 font-medium">
            Página {page + 1} de {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => setPage(page + 1)}
            className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-slate-200 bg-white disabled:opacity-40"
          >
            Próxima
          </button>
        </div>
      )}

      {/* Modal Ficha Completa do Aluno */}
      {selectedStudent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-xl bg-white p-6 rounded-2xl shadow-2xl max-h-[90vh] overflow-y-auto space-y-4">
            <div className="flex items-start justify-between pb-3 border-b border-slate-200">
              <div className="flex items-center gap-3">
                {selectedStudent.photoUrl ? (
                  <img
                    src={selectedStudent.photoUrl}
                    alt={selectedStudent.fullName}
                    className="w-16 h-16 rounded-2xl object-cover border border-slate-200 shadow-sm"
                  />
                ) : (
                  <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-brand-100 text-brand-700 font-black text-xl">
                    {selectedStudent.fullName.substring(0, 2).toUpperCase()}
                  </div>
                )}
                <div>
                  <h3 className="text-lg font-bold text-slate-900">{selectedStudent.fullName}</h3>
                  <p className="text-xs text-slate-500 font-medium">
                    Idade Atual: <strong className="text-slate-800">{selectedStudent.age} anos</strong> (Nasc: {new Date(selectedStudent.birthDate + 'T12:00:00').toLocaleDateString('pt-BR')})
                  </p>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Data de Ingresso: {new Date(selectedStudent.enrollmentDate + 'T12:00:00').toLocaleDateString('pt-BR')}
                  </p>
                </div>
              </div>
              <GeneralStatusBadge status={selectedStudent.status} />
            </div>

            {selectedStudent.notes && (
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700">
                <strong className="block text-slate-900 mb-1">Observações Gerais:</strong>
                {selectedStudent.notes}
              </div>
            )}

            {/* Responsáveis Vinculados */}
            <div>
              <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Responsáveis Vinculados
              </h4>
              <div className="space-y-2">
                {selectedStudent.guardians?.map((g) => (
                  <div key={g.guardianId} className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs flex items-center justify-between">
                    <div>
                      <p className="font-bold text-slate-900">{g.fullName} ({g.relationship})</p>
                      <p className="text-slate-500 flex items-center gap-2 mt-0.5">
                        <span className="flex items-center gap-1">
                          <Phone className="w-3 h-3 text-emerald-600" /> {g.phone}
                        </span>
                        {g.email && (
                          <span className="flex items-center gap-1">
                            <Mail className="w-3 h-3 text-blue-600" /> {g.email}
                          </span>
                        )}
                      </p>
                    </div>
                    {g.emergencyContact && (
                      <span className="text-[10px] font-bold text-rose-700 bg-rose-100 px-2 py-0.5 rounded-full">
                        Emergência
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Histórico de Turmas */}
            <div>
              <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Histórico de Vínculos / Turmas
              </h4>
              <div className="space-y-2">
                {selectedStudent.enrollments?.map((e) => (
                  <div key={e.enrollmentId} className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs flex items-center justify-between">
                    <div>
                      <p className="font-bold text-slate-900">{e.className} ({e.category})</p>
                      <p className="text-slate-500 text-[11px] mt-0.5">
                        Período: {new Date(e.entryDate + 'T12:00:00').toLocaleDateString('pt-BR')} até{' '}
                        {e.exitDate ? new Date(e.exitDate + 'T12:00:00').toLocaleDateString('pt-BR') : 'Presente'}
                      </p>
                    </div>
                    <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-200 text-slate-700">
                      {e.status}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <button
                type="button"
                onClick={() => setSelectedStudent(null)}
                className="px-5 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl"
              >
                Fechar Ficha
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Cadastro/Edição de Aluno */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="w-full max-w-xl bg-white p-6 rounded-2xl shadow-2xl max-h-[90vh] overflow-y-auto space-y-4">
            <h3 className="text-base font-bold text-slate-900">
              {editingId ? 'Editar Aluno' : 'Novo Aluno'}
            </h3>

            {formError && (
              <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleSaveStudent} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <div className="sm:col-span-2">
                  <label className="block font-bold text-slate-700 mb-1">Nome Completo do Aluno *</label>
                  <input
                    type="text"
                    required
                    value={formName}
                    onChange={(e) => setFormName(e.target.value)}
                    placeholder="Ex: Gabriel Mendes de Souza"
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Data de Nascimento *</label>
                  <input
                    type="date"
                    required
                    max={new Date().toISOString().substring(0, 10)}
                    value={formBirthDate}
                    onChange={(e) => setFormBirthDate(e.target.value)}
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Data de Ingresso *</label>
                  <input
                    type="date"
                    required
                    value={formEnrollDate}
                    onChange={(e) => setFormEnrollDate(e.target.value)}
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Situação *</label>
                  <select
                    value={formStatus}
                    onChange={(e) => setFormStatus(e.target.value as StudentStatus)}
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
                  >
                    <option value="ATIVO">Ativo</option>
                    <option value="INATIVO">Inativo</option>
                  </select>
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Foto do Aluno (opcional)</label>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    onChange={handlePhotoUpload}
                    className="w-full p-1.5 bg-slate-50 border border-slate-300 rounded-xl text-xs"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block font-bold text-slate-700 mb-1">Observações Gerais</label>
                  <textarea
                    rows={2}
                    value={formNotes}
                    onChange={(e) => setFormNotes(e.target.value)}
                    placeholder="Ex: Canhoto, joga de lateral, precisa de óculos..."
                    className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
                  />
                </div>
              </div>

              {/* Seção Responsável Principal */}
              <div className="pt-3 border-t border-slate-200">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2">
                  Responsável Principal
                </h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div>
                    <label className="block font-bold text-slate-700 mb-1">Nome do Responsável</label>
                    <input
                      type="text"
                      value={guardianName}
                      onChange={(e) => setGuardianName(e.target.value)}
                      placeholder="Ex: Patrícia Mendes"
                      className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
                    />
                  </div>

                  <div>
                    <label className="block font-bold text-slate-700 mb-1">Parentesco / Vínculo</label>
                    <select
                      value={guardianRel}
                      onChange={(e) => setGuardianRel(e.target.value)}
                      className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
                    >
                      <option value="Mãe">Mãe</option>
                      <option value="Pai">Pai</option>
                      <option value="Avó/Avô">Avó/Avô</option>
                      <option value="Tio/Tia">Tio/Tia</option>
                      <option value="Outro">Outro Responsável</option>
                    </select>
                  </div>

                  <div>
                    <label className="block font-bold text-slate-700 mb-1">Telefone / WhatsApp *</label>
                    <input
                      type="text"
                      value={guardianPhone}
                      onChange={(e) => setGuardianPhone(e.target.value)}
                      placeholder="(11) 98765-4321"
                      className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
                    />
                  </div>

                  <div>
                    <label className="block font-bold text-slate-700 mb-1">E-mail (opcional)</label>
                    <input
                      type="email"
                      value={guardianEmail}
                      onChange={(e) => setGuardianEmail(e.target.value)}
                      placeholder="email@exemplo.com"
                      className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500"
                    />
                  </div>
                </div>
              </div>

              {/* Matrícula em Turma */}
              <div className="pt-3 border-t border-slate-200">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2">
                  Turmas Vinculadas
                </h4>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  {classes.map((c) => (
                    <label
                      key={c.id}
                      className="flex items-center gap-2 p-2.5 bg-slate-50 border border-slate-200 rounded-xl cursor-pointer hover:bg-slate-100"
                    >
                      <input
                        type="checkbox"
                        checked={formSelectedClasses.includes(c.id)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setFormSelectedClasses([...formSelectedClasses, c.id]);
                          } else {
                            setFormSelectedClasses(formSelectedClasses.filter((id) => id !== c.id));
                          }
                        }}
                        className="rounded text-brand-600 focus:ring-brand-500"
                      />
                      <span className="font-semibold text-slate-800 truncate">{c.name} ({c.category})</span>
                    </label>
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
                  {formSaving ? 'Gravando...' : 'Salvar Aluno'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
