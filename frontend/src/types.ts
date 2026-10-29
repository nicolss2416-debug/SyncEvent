/* ─────────────────────────────────────────────
   SyncEvent – Centralized Types & Interfaces
   Aligned with System Requirements & ER Model
───────────────────────────────────────────── */

export type EstadoEvento = 'BORRADOR' | 'PUBLICADO' | 'EN_CURSO' | 'FINALIZADO' | 'CANCELADO' | 'APLAZADO';
export type Modalidad    = 'PRESENCIAL' | 'VIRTUAL' | 'HIBRIDO';
export type TipoEvento   = 'CAPACITACION' | 'TALLER' | 'TORNEO' | 'CONFERENCIA' | 'SEMINARIO';
export type UserRole     = 'ADMINISTRADOR' | 'OPERADOR' | 'MONITOR' | 'INVITADO';
export type AppScreen    = 'landing' | 'login' | 'register' | 'app';

export interface Event {
  id: string;
  name: string;
  type: TipoEvento;
  modality: Modalidad;
  startAt: string;
  endAt: string;
  location?: string;
  meetingUrl?: string;
  capacity: number;
  durationHours: number;
  status: EstadoEvento;
  description?: string;
  registered?: number;
  cancellationReason?: string;
  postponementReason?: string;
  assignedMonitors?: string[]; // IDs or emails of monitors
}

export interface Participant {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  documentType: string;
  documentNumber: string;
  createdAt: string;
  activeFines?: number; // count of unresolved fines
}

export interface Registration {
  id: string;
  participantId: string;
  eventId: string;
  participantName: string;
  participantEmail: string;
  eventName: string;
  status: 'ACTIVA' | 'CANCELADA';
  createdAt: string;
  qrToken: string;
  checkedIn: boolean;
  cancellationReason?: string;
  cancelledAt?: string;
}

export interface AuthUser {
  email: string;
  role: UserRole;
  name: string;
  token?: string;
  allRoles?: UserRole[];
}

export interface UserAccount {
  id: string;
  name: string;
  email: string;
  roles: UserRole[];
  active: boolean;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  timestamp: string;
  userEmail: string;
  action: string;
  entityType: 'USUARIO' | 'EVENTO' | 'INSCRIPCION' | 'ASISTENCIA' | 'CERTIFICADO' | 'POLITICA';
  entityId: string;
  ipAddress: string;
  result: 'EXITO' | 'FALLO';
  reason?: string;
  details?: string;
}

export interface FineRecord {
  id: string;
  participantId: string;
  participantName: string;
  participantEmail: string;
  documentNumber: string;
  missedEventsCount: number;
  periodDays: number;
  status: 'ACTIVA' | 'RESUELTA';
  issuedAt: string;
  resolvedAt?: string;
  resolvedBy?: string;
  notes?: string;
}

export interface CertificateRecord {
  id: string;
  code: string; // unique code, e.g., SYNC-CERT-2026-X89F
  eventId: string;
  eventName: string;
  participantName: string;
  participantDocument: string;
  issuedAt: string;
  durationHours: number;
  status: 'VALIDO' | 'REVOCADO';
  revocationReason?: string;
  revokedAt?: string;
  revokedBy?: string;
}

export interface Toast {
  id: string;
  message: string;
  type: 'success' | 'info' | 'error' | 'warning';
}
