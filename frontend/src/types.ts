// Tipos do FutManager

export type Role = 'ADMINISTRADOR' | 'PROFESSOR' | 'SUPER_ADMIN';
export type UserStatus = 'ATIVO' | 'INATIVO';
export type StudentStatus = 'ATIVO' | 'INATIVO';
export type ClassStatus = 'ATIVO' | 'INATIVO';
export type EnrollmentStatus = 'ATIVO' | 'INATIVO' | 'CONCLUIDO' | 'TRANSFERIDO';
export type LessonStatus = 'AGENDADA' | 'REALIZADA' | 'CANCELADA';
export type LessonType = 'REGULAR' | 'EXTRA' | 'REMARCADA';
export type AttendanceState = 'RASCUNHO' | 'FINALIZADA';
export type AttendanceStatus = 'PRESENTE' | 'AUSENTE' | 'FALTA_JUSTIFICADA';
export type AuditAction = 'INICIADA' | 'SALVO_RASCUNHO' | 'FINALIZADA' | 'ALTERADA';
export type TenantPlan = 'STARTER' | 'PRO' | 'ENTERPRISE';
export type TenantStatus = 'TRIAL' | 'ATIVO' | 'SUSPENSO';

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  cnpj?: string;
  plan: TenantPlan;
  status: TenantStatus;
  maxStudents: number;
  phone?: string;
  address?: string;
  trialEndsAt?: string;
  createdAt: string;
}

export interface TenantOnboardRequest {
  schoolName: string;
  adminFullName: string;
  adminEmail: string;
  phone?: string;
  cnpj?: string;
  address?: string;
  plan?: TenantPlan;
}

export interface OnboardResponse {
  tenant: Tenant;
  adminUser: {
    id: string;
    email: string;
    fullName: string;
    role: Role;
    status: UserStatus;
  };
  token: string;
  message: string;
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  status: UserStatus;
  phone?: string;
  tenantId?: string;
  tenantName?: string;
  tenantSlug?: string;
  createdAt: string;
}

export interface StudentSummary {
  id: string;
  fullName: string;
  birthDate: string;
  age: number;
  photoUrl?: string;
  status: StudentStatus;
  enrollmentDate: string;
  currentClasses: string[];
}

export interface StudentGuardian {
  guardianId: string;
  fullName: string;
  phone: string;
  email?: string;
  relationship: string;
  mainContact: boolean;
  emergencyContact: boolean;
}

export interface EnrollmentSummary {
  enrollmentId: string;
  classId: string;
  className: string;
  category: string;
  entryDate: string;
  exitDate?: string;
  status: EnrollmentStatus;
}

export interface StudentDetail {
  id: string;
  fullName: string;
  birthDate: string;
  age: number;
  photoUrl?: string;
  status: StudentStatus;
  enrollmentDate: string;
  notes?: string;
  guardians: StudentGuardian[];
  enrollments: EnrollmentSummary[];
  createdAt: string;
}

export interface Guardian {
  id: string;
  fullName: string;
  phone: string;
  email?: string;
  linkedStudentsCount: number;
  createdAt: string;
}

export interface TeacherSummary {
  userId: string;
  fullName: string;
  email: string;
  substitute: boolean;
}

export interface ClassSchedule {
  id?: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
}

export interface SchoolClass {
  id: string;
  name: string;
  category: string;
  location?: string;
  maxCapacity?: number;
  status: ClassStatus;
  startDate?: string;
  endDate?: string;
  teachers: TeacherSummary[];
  schedules: ClassSchedule[];
  activeStudentsCount: number;
}

export interface Lesson {
  id: string;
  classId: string;
  className: string;
  category: string;
  date: string;
  startTime: string;
  endTime: string;
  status: LessonStatus;
  type: LessonType;
  notes?: string;
  attendanceId?: string;
  attendanceStatus?: AttendanceState;
  eligibleStudentsCount: number;
  hasPendingAttendance: boolean;
}

export interface AttendanceStudentItem {
  studentId: string;
  studentName: string;
  birthDate: string;
  ageOnLessonDate: number;
  photoUrl?: string;
  status?: AttendanceStatus;
  notes?: string;
}

export interface AttendanceAuditLog {
  id: string;
  action: AuditAction;
  changedByUserId: string;
  changedByUserName: string;
  reason: string;
  details: string;
  createdAt: string;
}

export interface AttendanceDetail {
  id: string;
  lessonId: string;
  classId: string;
  className: string;
  category: string;
  lessonDate: string;
  startTime: string;
  endTime: string;
  status: AttendanceState;
  startedByUserId: string;
  startedByUserName: string;
  startedAt: string;
  finalizedByUserId?: string;
  finalizedByUserName?: string;
  finalizedAt?: string;
  version: number;
  totalEligibleStudents: number;
  presentCount: number;
  absentCount: number;
  excusedCount: number;
  unclassifiedCount: number;
  students: AttendanceStudentItem[];
  auditLogs: AttendanceAuditLog[];
}

export interface DashboardOverview {
  totalActiveStudents: number;
  totalClasses: number;
  pendingLessonsCount: number;
  lowFrequencyStudentsCount: number;
  averageAttendancePercentage: number | null;
  todayLessons: Lesson[];
  lowFrequencyAlerts: StudentFrequencySummary[];
}

export interface StudentFrequencySummary {
  studentId: string;
  studentName: string;
  currentAge: number;
  photoUrl?: string;
  totalLessons: number;
  presences: number;
  unexcusedAbsences: number;
  excusedAbsences: number;
  attendancePercentage: number | null;
  belowThreshold: boolean;
  classBreakdowns: {
    classId: string;
    className: string;
    totalLessons: number;
    presences: number;
    unexcusedAbsences: number;
    excusedAbsences: number;
    attendancePercentage: number | null;
  }[];
}

export interface StudentHistoryItem {
  lessonId: string;
  classId: string;
  className: string;
  lessonDate: string;
  ageOnLessonDate: number;
  status: string;
  notes?: string;
}

export interface ClassComparison {
  classId: string;
  className: string;
  category: string;
  totalLessonsCounted: number;
  activeStudentsCount: number;
  totalPresences: number;
  totalAbsences: number;
  totalExcused: number;
  averageAttendancePercentage: number | null;
}

export interface MonthlyEvolution {
  monthYear: string;
  totalLessons: number;
  presences: number;
  absences: number;
  excusedAbsences: number;
  attendancePercentage: number | null;
}

export interface SchoolConfig {
  schoolName: string;
  cnpj: string;
  phone: string;
  address: string;
  timezone: string;
  lessonHorizonDays: number;
  lowFrequencyThreshold: number;
}

export interface ImportRow {
  lineNumber: number;
  studentName: string;
  birthDateStr: string;
  parsedBirthDate?: string;
  enrollmentDateStr?: string;
  parsedEnrollmentDate?: string;
  guardianName: string;
  guardianPhone: string;
  guardianEmail?: string;
  relationship: string;
  className?: string;
  notes?: string;
  valid: boolean;
  errors: string[];
  possibleDuplicate: boolean;
  duplicateReason?: string;
}

export interface ImportPreviewResponse {
  totalRows: number;
  validRows: number;
  invalidRows: number;
  duplicateWarningsCount: number;
  rows: ImportRow[];
}
