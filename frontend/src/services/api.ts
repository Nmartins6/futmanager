// API Client para comunicação com o backend Spring Boot do FutManager

const envApiUrl = import.meta.env.VITE_API_URL;
const BASE_URL = envApiUrl ? (envApiUrl.endsWith('/api') ? envApiUrl : `${envApiUrl.replace(/\/$/, '')}/api`) : '/api';

export class ApiError extends Error {
  status: number;
  details?: any;

  constructor(message: string, status: number, details?: any) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('futmanager_token');

  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (!(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorMessage = `Erro HTTP ${response.status}`;
    let details: any = null;

    try {
      const data = await response.json();
      if (data.message) errorMessage = data.message;
      else if (data.error) errorMessage = data.error;
      details = data.details || null;
    } catch {
      // Fallback para texto plano se não for JSON
      const text = await response.text();
      if (text) errorMessage = text;
    }

    if (response.status === 401) {
      localStorage.removeItem('futmanager_token');
      localStorage.removeItem('futmanager_user');
      window.dispatchEvent(new Event('auth:unauthorized'));
    }

    throw new ApiError(errorMessage, response.status, details);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

async function downloadFile(endpoint: string, defaultFilename: string) {
  const token = localStorage.getItem('futmanager_token');
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const response = await fetch(`${BASE_URL}${endpoint}`, { headers });
  if (!response.ok) {
    throw new ApiError('Falha no download do arquivo.', response.status);
  }

  const disposition = response.headers.get('Content-Disposition');
  let filename = defaultFilename;
  if (disposition && disposition.includes('filename=')) {
    const match = disposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) filename = match[1];
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

export const api = {
  // Autenticação
  login: (email: string, password?: string) => request<any>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  }),
  getMe: () => request<any>('/auth/me'),
  recoverPassword: (email: string) => request<any>('/auth/recover-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  }),
  resetPassword: (email: string, newPassword: string) => request<any>('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ email, newPassword }),
  }),

  // Usuários / Professores
  getUsers: (params?: string) => request<any>(`/users${params ? `?${params}` : ''}`),
  getActiveTeachers: () => request<any[]>('/users/teachers'),
  inviteUser: (data: any) => request<any>('/users/invite', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  updateUser: (id: string, data: any) => request<any>(`/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  toggleUserStatus: (id: string) => request<any>(`/users/${id}/toggle-status`, {
    method: 'PATCH',
  }),

  // Alunos
  getStudents: (params?: string) => request<any>(`/students${params ? `?${params}` : ''}`),
  getStudentById: (id: string) => request<any>(`/students/${id}`),
  createStudent: (data: any) => request<any>('/students', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  updateStudent: (id: string, data: any) => request<any>(`/students/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  toggleStudentStatus: (id: string) => request<any>(`/students/${id}/toggle-status`, {
    method: 'PATCH',
  }),

  // Responsáveis
  getGuardians: (params?: string) => request<any>(`/guardians${params ? `?${params}` : ''}`),
  getGuardianById: (id: string) => request<any>(`/guardians/${id}`),
  createGuardian: (data: any) => request<any>('/guardians', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  updateGuardian: (id: string, data: any) => request<any>(`/guardians/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),

  // Turmas
  getClasses: (params?: string) => request<any>(`/classes${params ? `?${params}` : ''}`),
  getAllActiveClasses: () => request<any[]>('/classes/all'),
  getClassById: (id: string) => request<any>(`/classes/${id}`),
  createClass: (data: any) => request<any>('/classes', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  updateClass: (id: string, data: any) => request<any>(`/classes/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  getClassEnrollments: (classId: string) => request<any[]>(`/classes/${classId}/enrollments`),
  enrollStudent: (data: any) => request<any>('/classes/enroll', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  updateEnrollment: (enrollmentId: string, data: any) => request<any>(`/classes/enrollments/${enrollmentId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),

  // Aulas
  getLessons: (params?: string) => request<any>(`/lessons${params ? `?${params}` : ''}`),
  getTodayLessons: () => request<any[]>('/lessons/today'),
  getLessonsByDate: (date: string) => request<any[]>(`/lessons/by-date?date=${date}`),
  getLessonById: (id: string) => request<any>(`/lessons/${id}`),
  createLesson: (data: any) => request<any>('/lessons', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  cancelLesson: (id: string, reason?: string) => request<any>(`/lessons/${id}/cancel`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  }),
  rescheduleLesson: (id: string, data: any) => request<any>(`/lessons/${id}/reschedule`, {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  generateHorizonLessons: (horizonDays?: number) => request<any>('/lessons/generate-horizon', {
    method: 'POST',
    body: JSON.stringify({ horizonDays }),
  }),

  // Chamada
  getLessonAttendance: (lessonId: string) => request<any>(`/lessons/${lessonId}/attendance`),
  saveLessonAttendance: (lessonId: string, data: any) => request<any>(`/lessons/${lessonId}/attendance`, {
    method: 'POST',
    body: JSON.stringify(data),
  }),

  // Estatísticas
  getDashboardOverview: () => request<any>('/statistics/dashboard'),
  getStudentFrequency: (studentId: string, classId?: string, startDate?: string, endDate?: string) => {
    const q = new URLSearchParams();
    if (classId) q.append('classId', classId);
    if (startDate) q.append('startDate', startDate);
    if (endDate) q.append('endDate', endDate);
    return request<any>(`/statistics/students/${studentId}?${q.toString()}`);
  },
  getStudentHistory: (studentId: string, classId?: string, startDate?: string, endDate?: string) => {
    const q = new URLSearchParams();
    if (classId) q.append('classId', classId);
    if (startDate) q.append('startDate', startDate);
    if (endDate) q.append('endDate', endDate);
    return request<any[]>(`/statistics/students/${studentId}/history?${q.toString()}`);
  },
  getClassesComparison: (startDate?: string, endDate?: string) => {
    const q = new URLSearchParams();
    if (startDate) q.append('startDate', startDate);
    if (endDate) q.append('endDate', endDate);
    return request<any[]>(`/statistics/classes/comparison?${q.toString()}`);
  },
  getMonthlyEvolution: (classId?: string, year?: number) => {
    const q = new URLSearchParams();
    if (classId) q.append('classId', classId);
    if (year) q.append('year', year.toString());
    return request<any[]>(`/statistics/evolution/monthly?${q.toString()}`);
  },
  getPendingLessons: (classId?: string, startDate?: string, endDate?: string) => {
    const q = new URLSearchParams();
    if (classId) q.append('classId', classId);
    if (startDate) q.append('startDate', startDate);
    if (endDate) q.append('endDate', endDate);
    return request<any[]>(`/statistics/pending-lessons?${q.toString()}`);
  },

  // Relatórios (Download PDF e XLSX)
  downloadClassFrequencyPdf: (classId: string, startDate?: string, endDate?: string) => {
    const q = new URLSearchParams({ classId });
    if (startDate) q.append('startDate', startDate);
    if (endDate) q.append('endDate', endDate);
    return downloadFile(`/reports/class-frequency/pdf?${q.toString()}`, 'relatorio_turma.pdf');
  },
  downloadStudentIndividualPdf: (studentId: string, classId?: string, startDate?: string, endDate?: string) => {
    const q = new URLSearchParams({ studentId });
    if (classId) q.append('classId', classId);
    if (startDate) q.append('startDate', startDate);
    if (endDate) q.append('endDate', endDate);
    return downloadFile(`/reports/student-individual/pdf?${q.toString()}`, 'ficha_individual_aluno.pdf');
  },
  downloadLowFrequencyPdf: (classId?: string, startDate?: string, endDate?: string) => {
    const q = new URLSearchParams();
    if (classId) q.append('classId', classId);
    if (startDate) q.append('startDate', startDate);
    if (endDate) q.append('endDate', endDate);
    return downloadFile(`/reports/low-frequency/pdf?${q.toString()}`, 'relatorio_baixa_frequencia.pdf');
  },
  downloadMonthlySummaryXlsx: (classId?: string, year?: number) => {
    const q = new URLSearchParams();
    if (classId) q.append('classId', classId);
    if (year) q.append('year', year.toString());
    return downloadFile(`/reports/monthly-summary/xlsx?${q.toString()}`, 'resumo_mensal.xlsx');
  },

  // Importação e Exportação
  downloadCsvTemplate: () => downloadFile('/import-export/template/csv', 'modelo_importacao.csv'),
  downloadXlsxTemplate: () => downloadFile('/import-export/template/xlsx', 'modelo_importacao.xlsx'),
  previewImport: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return request<any>('/import-export/preview', {
      method: 'POST',
      body: formData,
    });
  },
  confirmImport: (rows: any[]) => request<any>('/import-export/confirm', {
    method: 'POST',
    body: JSON.stringify({ rows }),
  }),
  exportStudentsCsv: (query?: string, status?: string, classId?: string) => {
    const q = new URLSearchParams();
    if (query) q.append('query', query);
    if (status) q.append('status', status);
    if (classId) q.append('classId', classId);
    return downloadFile(`/import-export/export/students/csv?${q.toString()}`, 'alunos.csv');
  },
  exportStudentsXlsx: (query?: string, status?: string, classId?: string) => {
    const q = new URLSearchParams();
    if (query) q.append('query', query);
    if (status) q.append('status', status);
    if (classId) q.append('classId', classId);
    return downloadFile(`/import-export/export/students/xlsx?${q.toString()}`, 'alunos.xlsx');
  },

  // SaaS Multi-Tenancy
  onboardSchool: (data: any) => request<any>('/tenants/onboard', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  getCurrentTenant: () => request<any>('/tenants/current'),
  getAllTenants: () => request<any[]>('/tenants'),

  // Configurações da Escolinha
  getConfigs: () => request<any>('/configs'),
  updateConfigs: (data: any) => request<any>('/configs', {
    method: 'PUT',
    body: JSON.stringify(data),
  }),

  // Upload de Fotos
  uploadPhoto: async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await request<{ photoUrl: string }>('/photos/upload', {
      method: 'POST',
      body: formData,
    });
    return res.photoUrl;
  },
};
