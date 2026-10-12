import React from 'react';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard,
  CalendarDays,
  Users,
  GraduationCap,
  HeartHandshake,
  UserCog,
  BarChart3,
  FileSpreadsheet,
  UploadCloud,
  Settings,
  X,
  ShieldCheck,
  CheckSquare
} from 'lucide-react';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  activePage: string;
  onNavigate: (page: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  isOpen,
  onClose,
  activePage,
  onNavigate,
}) => {
  const { isAdmin, user } = useAuth();

  const navItems = [
    { id: 'dashboard', label: 'Painel Inicial', icon: LayoutDashboard },
    { id: 'lessons', label: 'Agenda & Aulas', icon: CalendarDays },
    { id: 'students', label: 'Alunos', icon: GraduationCap },
    { id: 'classes', label: 'Turmas & Horários', icon: Users },
    { id: 'guardians', label: 'Responsáveis', icon: HeartHandshake },
    ...(isAdmin ? [{ id: 'teachers', label: 'Professores', icon: UserCog }] : []),
    { id: 'statistics', label: 'Estatísticas', icon: BarChart3 },
    { id: 'reports', label: 'Relatórios', icon: FileSpreadsheet },
    ...(isAdmin ? [
      { id: 'import-export', label: 'Importar / Exportar', icon: UploadCloud },
      { id: 'settings', label: 'Configurações', icon: Settings },
    ] : []),
  ];

  return (
    <>
      {/* Overlay no mobile */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/50 backdrop-blur-sm lg:hidden transition-opacity"
          onClick={onClose}
        />
      )}

      <aside
        className={`fixed top-0 bottom-0 left-0 z-50 w-64 bg-slate-900 text-slate-100 flex flex-col transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:z-auto ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Cabeçalho do Sidebar com Logo esportivo */}
        <div className="flex items-center justify-between h-16 px-4 bg-slate-950/60 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="flex items-center justify-center w-9 h-9 rounded-xl bg-pitch-600 text-white font-black text-xl shadow-md">
              ⚽
            </div>
            <div>
              <span className="font-extrabold text-lg tracking-tight text-white block leading-tight">
                Fut<span className="text-pitch-500">Manager</span>
              </span>
              <span className="text-[10px] uppercase font-semibold text-slate-400 tracking-wider">
                Gestão de Chamadas
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 rounded-lg lg:hidden hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Links de navegação */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activePage === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  onNavigate(item.id);
                  onClose();
                }}
                className={`flex items-center w-full gap-3 px-3.5 py-2.5 text-sm font-medium rounded-xl transition-all ${
                  isActive
                    ? 'bg-brand-600 text-white shadow-sm font-semibold'
                    : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
                }`}
              >
                <Icon className={`w-5 h-5 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* Rodapé do Sidebar com identificação da Escolinha (SaaS Tenant) */}
        <div className="p-3 border-t border-slate-800 text-xs text-slate-300">
          <div className="flex items-center gap-2 mb-1">
            <span className="w-2 h-2 rounded-full bg-emerald-400 shrink-0"></span>
            <p className="font-semibold text-white truncate" title={user?.tenantName || 'FutManager SaaS'}>
              {user?.tenantName || 'FutManager SaaS'}
            </p>
          </div>
          <div className="flex items-center justify-between text-[11px] text-slate-400">
            <span className="uppercase tracking-wider font-bold text-pitch-400">SaaS Multi-Escola</span>
            <span className="bg-slate-800 text-slate-300 px-1.5 py-0.5 rounded text-[10px]">Ativo</span>
          </div>
        </div>
      </aside>
    </>
  );
};
