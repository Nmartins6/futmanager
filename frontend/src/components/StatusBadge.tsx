import React from 'react';
import { CheckCircle2, XCircle, AlertCircle, Clock, CalendarCheck, Ban } from 'lucide-react';
import { AttendanceStatus, LessonStatus, StudentStatus } from '../types';

export const AttendanceBadge: React.FC<{ status?: AttendanceStatus; size?: 'sm' | 'md' }> = ({
  status,
  size = 'md',
}) => {
  const isSm = size === 'sm';
  const iconClass = isSm ? 'w-3.5 h-3.5' : 'w-4 h-4';
  const textClass = isSm ? 'text-xs px-2 py-0.5' : 'text-sm px-2.5 py-1';

  if (status === 'PRESENTE') {
    return (
      <span className={`inline-flex items-center gap-1.5 font-semibold rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200 ${textClass}`}>
        <CheckCircle2 className={`${iconClass} text-emerald-600`} aria-hidden="true" />
        <span>Presente</span>
      </span>
    );
  }

  if (status === 'AUSENTE') {
    return (
      <span className={`inline-flex items-center gap-1.5 font-semibold rounded-full bg-rose-100 text-rose-800 border border-rose-200 ${textClass}`}>
        <XCircle className={`${iconClass} text-rose-600`} aria-hidden="true" />
        <span>Ausente</span>
      </span>
    );
  }

  if (status === 'FALTA_JUSTIFICADA') {
    return (
      <span className={`inline-flex items-center gap-1.5 font-semibold rounded-full bg-amber-100 text-amber-800 border border-amber-200 ${textClass}`}>
        <AlertCircle className={`${iconClass} text-amber-600`} aria-hidden="true" />
        <span>Falta Justificada</span>
      </span>
    );
  }

  return (
    <span className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-slate-100 text-slate-600 border border-slate-200 ${textClass}`}>
      <Clock className={`${iconClass} text-slate-400`} aria-hidden="true" />
      <span>Pendente</span>
    </span>
  );
};

export const LessonStatusBadge: React.FC<{ status: LessonStatus }> = ({ status }) => {
  if (status === 'REALIZADA') {
    return (
      <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
        <CalendarCheck className="w-3.5 h-3.5 text-emerald-600" /> Realizada
      </span>
    );
  }
  if (status === 'CANCELADA') {
    return (
      <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded-full bg-rose-100 text-rose-800 border border-rose-200">
        <Ban className="w-3.5 h-3.5 text-rose-600" /> Cancelada
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800 border border-blue-200">
      <Clock className="w-3.5 h-3.5 text-blue-600" /> Agendada
    </span>
  );
};

export const GeneralStatusBadge: React.FC<{ status: StudentStatus | string }> = ({ status }) => {
  const isAtivo = status === 'ATIVO';
  return (
    <span
      className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs font-semibold rounded-full ${
        isAtivo
          ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
          : 'bg-slate-100 text-slate-700 border border-slate-300'
      }`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${isAtivo ? 'bg-emerald-600' : 'bg-slate-400'}`} />
      {status}
    </span>
  );
};
