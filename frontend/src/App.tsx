import React, { useState } from 'react';
import { useAuth } from './context/AuthContext';
import { Layout } from './components/Layout';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Attendance } from './pages/Attendance';
import { Students } from './pages/Students';
import { Classes } from './pages/Classes';
import { Lessons } from './pages/Lessons';
import { Guardians } from './pages/Guardians';
import { Teachers } from './pages/Teachers';
import { Statistics } from './pages/Statistics';
import { Reports } from './pages/Reports';
import { ImportExport } from './pages/ImportExport';
import { SettingsPage } from './pages/Settings';

export const App: React.FC = () => {
  const { user, loading, isAdmin } = useAuth();
  const [currentPage, setCurrentPage] = useState<string>('dashboard');
  const [pageParams, setPageParams] = useState<any>({});

  const handleNavigate = (page: string, params: any = {}) => {
    // Validação de acesso de permissão no frontend
    if (!isAdmin && ['teachers', 'import-export', 'settings'].includes(page)) {
      setCurrentPage('dashboard');
      return;
    }
    setCurrentPage(page);
    setPageParams(params);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-slate-900 text-white">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-xs font-semibold text-slate-300">Iniciando FutManager...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Login />;
  }

  // Título dinâmico da página atual
  const pageTitles: Record<string, string> = {
    dashboard: 'Painel Geral',
    lessons: 'Agenda & Aulas',
    attendance: 'Chamada da Aula',
    students: 'Alunos',
    classes: 'Turmas & Horários',
    guardians: 'Responsáveis & Contatos',
    teachers: 'Equipe de Professores',
    statistics: 'Estatísticas & Frequência',
    reports: 'Relatórios Oficiais',
    'import-export': 'Importação & Exportação',
    settings: 'Configurações da Escolinha',
  };

  return (
    <Layout
      activePage={currentPage}
      pageTitle={pageTitles[currentPage] || 'FutManager'}
      onNavigate={handleNavigate}
    >
      {currentPage === 'dashboard' && <Dashboard onNavigate={handleNavigate} />}
      {currentPage === 'lessons' && <Lessons onNavigate={handleNavigate} />}
      {currentPage === 'attendance' && (
        <Attendance
          lessonId={pageParams.lessonId}
          onBack={() => handleNavigate('dashboard')}
        />
      )}
      {currentPage === 'students' && <Students />}
      {currentPage === 'classes' && <Classes />}
      {currentPage === 'guardians' && <Guardians />}
      {currentPage === 'teachers' && isAdmin && <Teachers />}
      {currentPage === 'statistics' && <Statistics onNavigate={handleNavigate} />}
      {currentPage === 'reports' && <Reports />}
      {currentPage === 'import-export' && isAdmin && <ImportExport />}
      {currentPage === 'settings' && isAdmin && <SettingsPage />}
    </Layout>
  );
};
