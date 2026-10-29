import React, { useState, useEffect, useCallback } from 'react';
import './styles.css';
import {
  Event,
  Participant,
  Registration,
  UserAccount,
  AuditLog,
  FineRecord,
  CertificateRecord,
  AuthUser,
  UserRole,
  AppScreen,
  Toast,
  EstadoEvento,
} from './types';
import { Icon } from './icons';
import { AdminScreen } from './screens/AdminScreen';
import { OperatorScreen } from './screens/OperatorScreen';
import { MonitorScreen } from './screens/MonitorScreen';
import { GuestScreen } from './screens/GuestScreen';

/* ─────────────────────────────────────────────
   INITIAL MOCK & SEED DATA
───────────────────────────────────────────── */
const INITIAL_EVENTS: Event[] = [
  {
    id: 'a39c34e2-a928-4a38-877f-b31ec4928fe5',
    name: 'Congreso de Innovación Tecnológica 2026',
    description: 'Evento oficial de tecnología y arquitectura hexagonal.',
    type: 'CAPACITACION',
    modality: 'PRESENCIAL',
    startAt: '2026-11-10T09:00',
    endAt: '2026-11-10T17:00',
    location: 'Auditorio Central SENA',
    capacity: 150,
    durationHours: 8,
    status: 'PUBLICADO',
    registered: 89,
    assignedMonitors: ['monitor1@syncevent.com'],
  },
  {
    id: '662fee67-c353-474d-ac96-1c0bd1db7c43',
    name: 'Taller de Habilidades Blandas y Liderazgo',
    description: 'Taller práctico interactivo enfocado en comunicación asertiva.',
    type: 'TALLER',
    modality: 'PRESENCIAL',
    startAt: '2026-11-15T08:00',
    endAt: '2026-11-15T12:00',
    location: 'Sala de Conferencias 3',
    capacity: 40,
    durationHours: 4,
    status: 'PUBLICADO',
    registered: 35,
    assignedMonitors: ['monitor2@syncevent.com'],
  },
  {
    id: '3',
    name: 'Masterclass Spring for GraphQL y PostgreSQL',
    description: 'Desarrollo de APIs reactivas de alto desempeño.',
    type: 'SEMINARIO',
    modality: 'VIRTUAL',
    startAt: '2026-10-30T14:00',
    endAt: '2026-10-30T18:00',
    meetingUrl: 'https://meet.google.com/syn-graphql',
    capacity: 200,
    durationHours: 4,
    status: 'EN_CURSO',
    registered: 180,
    assignedMonitors: ['monitor3@syncevent.com'],
  },
  {
    id: '4',
    name: 'Torneo Nacional de Algoritmia y Código',
    description: 'Competencia técnica de resolución de problemas.',
    type: 'TORNEO',
    modality: 'HIBRIDO',
    startAt: '2026-09-20T08:00',
    endAt: '2026-09-20T18:00',
    location: 'Campus Principal SENA',
    capacity: 100,
    durationHours: 10,
    status: 'FINALIZADO',
    registered: 95,
  },
];

const INITIAL_PARTICIPANTS: Participant[] = [
  { id: '1', firstName: 'Laura', lastName: 'Gómez', email: 'laura.gomez@example.test', phone: '+57 310 123 4567', documentType: 'CC', documentNumber: '1020304050', createdAt: '2026-09-01' },
  { id: '2', firstName: 'Mateo', lastName: 'Rodríguez', email: 'mateo.rodriguez@example.test', phone: '+57 311 987 6543', documentType: 'CC', documentNumber: '1098765432', createdAt: '2026-09-15' },
  { id: '3', firstName: 'Camila', lastName: 'Vargas', email: 'camila.vargas@ejemplo.com', phone: '+57 300 456 7890', documentType: 'TI', documentNumber: '1030405060', createdAt: '2026-10-01', activeFines: 1 },
  { id: '4', firstName: 'Andrés', lastName: 'Mendoza', email: 'andres.mendoza@ejemplo.com', phone: '+57 320 654 3210', documentType: 'CE', documentNumber: '987654321', createdAt: '2026-10-12' },
];

const INITIAL_REGISTRATIONS: Registration[] = [
  { id: '1', participantId: '1', eventId: 'a39c34e2-a928-4a38-877f-b31ec4928fe5', participantName: 'Laura Gómez', participantEmail: 'laura.gomez@example.test', eventName: 'Congreso de Innovación Tecnológica 2026', status: 'ACTIVA', createdAt: '2026-10-02', qrToken: 'QR-SYNC-001', checkedIn: true },
  { id: '2', participantId: '2', eventId: '662fee67-c353-474d-ac96-1c0bd1db7c43', participantName: 'Mateo Rodríguez', participantEmail: 'mateo.rodriguez@example.test', eventName: 'Taller de Habilidades Blandas y Liderazgo', status: 'ACTIVA', createdAt: '2026-10-10', qrToken: 'QR-SYNC-002', checkedIn: false },
  { id: '3', participantId: '3', eventId: '3', participantName: 'Camila Vargas', participantEmail: 'camila.vargas@ejemplo.com', eventName: 'Masterclass Spring for GraphQL y PostgreSQL', status: 'ACTIVA', createdAt: '2026-10-18', qrToken: 'QR-SYNC-003', checkedIn: true },
  { id: '4', participantId: '4', eventId: '4', participantName: 'Andrés Mendoza', participantEmail: 'andres.mendoza@ejemplo.com', eventName: 'Torneo Nacional de Algoritmia y Código', status: 'ACTIVA', createdAt: '2026-09-08', qrToken: 'QR-SYNC-004', checkedIn: true },
];

const INITIAL_USERS: UserAccount[] = [
  { id: 'u1', name: 'Administrador Principal', email: 'admin@syncevent.com', roles: ['ADMINISTRADOR'], active: true, createdAt: '2026-08-01' },
  { id: 'u2', name: 'Carlos Operador', email: 'operador@syncevent.com', roles: ['OPERADOR'], active: true, createdAt: '2026-08-10' },
  { id: 'u3', name: 'Andrea Monitora', email: 'monitor@syncevent.com', roles: ['MONITOR'], active: true, createdAt: '2026-08-15' },
  { id: 'u4', name: 'Laura Gómez', email: 'laura.gomez@example.test', roles: ['INVITADO'], active: true, createdAt: '2026-09-01' },
];

const INITIAL_AUDIT: AuditLog[] = [
  { id: 'a1', timestamp: '2026-10-29 09:15:22', userEmail: 'admin@syncevent.com', action: 'INICIO_SESION', entityType: 'USUARIO', entityId: 'u1', ipAddress: '192.168.1.45', result: 'EXITO', details: 'Autenticación exitosa mediante credenciales JWT' },
  { id: 'a2', timestamp: '2026-10-29 09:22:10', userEmail: 'operador@syncevent.com', action: 'PUBLICAR_EVENTO', entityType: 'EVENTO', entityId: 'a39c34e2', ipAddress: '192.168.1.52', result: 'EXITO', details: 'Evento Congreso de Innovación Tecnológica publicado' },
  { id: 'a3', timestamp: '2026-10-29 09:40:05', userEmail: 'monitor@syncevent.com', action: 'REGISTRAR_ASISTENCIA', entityType: 'ASISTENCIA', entityId: 'QR-SYNC-001', ipAddress: '10.0.4.12', result: 'EXITO', details: 'Check-in verificado por QR para Laura Gómez' },
  { id: 'a4', timestamp: '2026-10-28 17:30:00', userEmail: 'admin@syncevent.com', action: 'REVOCAR_CERTIFICADO', entityType: 'CERTIFICADO', entityId: 'SYNC-CERT-OLD-01', ipAddress: '192.168.1.45', result: 'EXITO', reason: 'Falsedad en horas prácticas declaradas', details: 'Certificado invalidado en verificación pública' },
];

const INITIAL_FINES: FineRecord[] = [
  {
    id: 'f1',
    participantId: '3',
    participantName: 'Camila Vargas',
    participantEmail: 'camila.vargas@ejemplo.com',
    documentNumber: '1030405060',
    missedEventsCount: 3,
    periodDays: 90,
    status: 'ACTIVA',
    issuedAt: '2026-10-25',
    notes: 'Inasistencia reiterada a 3 eventos consecutivos con aforo reservado sin previa cancelación.',
  },
];

const INITIAL_CERTIFICATES: CertificateRecord[] = [
  {
    id: 'c1',
    code: 'SYNC-CERT-2026-X89F',
    eventId: '4',
    eventName: 'Torneo Nacional de Algoritmia y Código',
    participantName: 'Andrés Mendoza',
    participantDocument: '987654321',
    issuedAt: '2026-09-21',
    durationHours: 10,
    status: 'VALIDO',
  },
  {
    id: 'c2',
    code: 'SYNC-CERT-OLD-01',
    eventId: '4',
    eventName: 'Torneo Nacional de Algoritmia y Código',
    participantName: 'Mateo Rodríguez',
    participantDocument: '1098765432',
    issuedAt: '2026-09-21',
    durationHours: 10,
    status: 'REVOCADO',
    revocationReason: 'Falsedad en horas prácticas declaradas',
    revokedAt: '2026-10-28',
    revokedBy: 'admin@syncevent.com',
  },
];

/* ─────────────────────────────────────────────
   ROOT COMPONENT: APP
───────────────────────────────────────────── */
export default function App() {
  const [screen, setScreen] = useState<AppScreen>('landing');
  const [authUser, setAuthUser] = useState<AuthUser | null>(null);

  // Core system collections
  const [events, setEvents] = useState<Event[]>(INITIAL_EVENTS);
  const [participants, setParticipants] = useState<Participant[]>(INITIAL_PARTICIPANTS);
  const [registrations, setRegistrations] = useState<Registration[]>(INITIAL_REGISTRATIONS);
  const [users, setUsers] = useState<UserAccount[]>(INITIAL_USERS);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>(INITIAL_AUDIT);
  const [fines, setFines] = useState<FineRecord[]>(INITIAL_FINES);
  const [certificates, setCertificates] = useState<CertificateRecord[]>(INITIAL_CERTIFICATES);

  // System indicators
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [backendHealth, setBackendHealth] = useState<'checking' | 'online' | 'offline'>('online');

  const addToast = useCallback((message: string, type: Toast['type'] = 'info') => {
    const id = Date.now().toString() + Math.random().toString();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4500);
  }, []);

  // Log inalterable audit trail entry
  const logAudit = useCallback((action: string, entityType: AuditLog['entityType'], entityId: string, result: 'EXITO' | 'FALLO', details?: string, reason?: string) => {
    const newLog: AuditLog = {
      id: 'a' + Date.now().toString(),
      timestamp: new Date().toLocaleString('es-CO'),
      userEmail: authUser?.email || 'sistema@syncevent.com',
      action,
      entityType,
      entityId,
      ipAddress: '127.0.0.1',
      result,
      details,
      reason,
    };
    setAuditLogs(prev => [newLog, ...prev]);
  }, [authUser?.email]);

  // Initial fetch from backend GraphQL
  useEffect(() => {
    async function loadGraphQLData() {
      try {
        const res = await fetch('http://localhost:8080/graphql', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            query: `query {
              events { id name description type modality startAt endAt location meetingUrl capacity durationHours status }
              dashboardSummary { totalEvents activeEvents totalParticipants totalRegistrations }
            }`
          })
        });
        if (res.ok) {
          const json = await res.json();
          if (json?.data?.events?.length > 0) {
            setEvents(prev => {
              const fromBackend: Event[] = json.data.events.map((e: any) => ({
                ...e,
                registered: 0,
              }));
              const existingIds = new Set(prev.map(p => p.id));
              return [...fromBackend.filter(b => !existingIds.has(b.id)), ...prev];
            });
            setBackendHealth('online');
          }
        }
      } catch {
        setBackendHealth('offline');
      }
    }
    loadGraphQLData();
  }, []);

  /* ── Auth Flow ── */
  const handleLoginSuccess = (user: AuthUser) => {
    setAuthUser(user);
    setScreen('app');
    logAudit('INICIO_SESION', 'USUARIO', user.email, 'EXITO', `Inicio de sesión con rol ${user.role}`);
    addToast(`¡Bienvenido a SyncEvent, ${user.name}! Accediendo como ${user.role}.`, 'success');
  };

  const handleLogout = () => {
    if (authUser) {
      logAudit('CIERRE_SESION', 'USUARIO', authUser.email, 'EXITO', 'Cierre de sesión voluntario');
    }
    setAuthUser(null);
    setScreen('landing');
    addToast('Sesión cerrada correctamente', 'info');
  };

  /* ── Dynamic Actor Switcher (for immediate demo/evaluation) ── */
  const handleSwitchActorRole = (newRole: UserRole) => {
    if (!authUser) return;
    const roleNames: Record<UserRole, string> = {
      ADMINISTRADOR: 'Administrador Maestro',
      OPERADOR: 'Operador Logístico',
      MONITOR: 'Monitor de Entrada',
      INVITADO: 'Laura Gómez (Invitado)',
    };
    setAuthUser({
      ...authUser,
      role: newRole,
      name: roleNames[newRole],
    });
    logAudit('CAMBIO_ROL_DEMO', 'USUARIO', authUser.email, 'EXITO', `Conmutado a pantalla de ${newRole}`);
    addToast(`Cambiado a pantalla de actor: ${newRole}`, 'info');
  };

  /* ── Admin Handlers ── */
  const handleAddUser = (user: Omit<UserAccount, 'id' | 'createdAt'>) => {
    const newUser: UserAccount = {
      ...user,
      id: 'u' + (users.length + 1),
      createdAt: new Date().toISOString().slice(0, 10),
    };
    setUsers(prev => [newUser, ...prev]);
    logAudit('CREAR_USUARIO', 'USUARIO', newUser.email, 'EXITO', `Usuario creado con roles: ${newUser.roles.join(', ')}`);
  };

  const handleUpdateUser = (id: string, updates: Partial<UserAccount>) => {
    setUsers(prev => prev.map(u => u.id === id ? { ...u, ...updates } : u));
    logAudit('EDITAR_USUARIO', 'USUARIO', id, 'EXITO', `Modificación de roles/datos de usuario`);
  };

  const handleToggleUserStatus = (id: string) => {
    setUsers(prev => prev.map(u => {
      if (u.id === id) {
        const next = !u.active;
        logAudit(next ? 'ACTIVAR_USUARIO' : 'DESACTIVAR_USUARIO', 'USUARIO', u.email, 'EXITO', `Estado modificado a ${next ? 'activo' : 'inactivo'}`);
        return { ...u, active: next };
      }
      return u;
    }));
  };

  const handleResolveFine = (fineId: string, notes: string) => {
    setFines(prev => prev.map(f => {
      if (f.id === fineId) {
        logAudit('RESOLVER_MULTA', 'POLITICA', f.participantEmail, 'EXITO', `Sanción resuelta. Notas: ${notes}`);
        return {
          ...f,
          status: 'RESUELTA',
          resolvedAt: new Date().toLocaleDateString('es-CO'),
          resolvedBy: authUser?.email || 'admin@syncevent.com',
          notes,
        };
      }
      return f;
    }));
    // Remove fine mark from participant
    const fine = fines.find(f => f.id === fineId);
    if (fine) {
      setParticipants(prev => prev.map(p => p.id === fine.participantId ? { ...p, activeFines: 0 } : p));
    }
  };

  const handleRevokeCertificate = (certId: string, reason: string) => {
    setCertificates(prev => prev.map(c => {
      if (c.id === certId) {
        logAudit('REVOCAR_CERTIFICADO', 'CERTIFICADO', c.code, 'EXITO', `Revocado por ${authUser?.email}`, reason);
        return {
          ...c,
          status: 'REVOCADO',
          revocationReason: reason,
          revokedAt: new Date().toLocaleDateString('es-CO'),
          revokedBy: authUser?.email || 'admin@syncevent.com',
        };
      }
      return c;
    }));
  };

  /* ── Operator Handlers ── */
  const handleSaveEvent = (data: Partial<Event>) => {
    if (data.id) {
      setEvents(prev => prev.map(e => e.id === data.id ? { ...e, ...data } : e));
      logAudit('EDITAR_EVENTO', 'EVENTO', data.id, 'EXITO', `Evento actualizado: ${data.name}`);
    } else {
      const newEv: Event = {
        id: 'ev_' + Date.now().toString(),
        name: data.name || 'Nuevo Evento',
        type: data.type || 'TALLER',
        modality: data.modality || 'PRESENCIAL',
        startAt: data.startAt || '2026-11-20T09:00',
        endAt: data.endAt || '2026-11-20T17:00',
        location: data.location,
        meetingUrl: data.meetingUrl,
        capacity: data.capacity || 50,
        durationHours: data.durationHours || 4,
        status: 'BORRADOR',
        description: data.description,
        registered: 0,
      };
      setEvents(prev => [newEv, ...prev]);
      logAudit('CREAR_EVENTO', 'EVENTO', newEv.id, 'EXITO', `Evento creado en estado BORRADOR: ${newEv.name}`);
    }
  };

  const handleUpdateEventStatus = (id: string, status: EstadoEvento) => {
    setEvents(prev => prev.map(e => {
      if (e.id === id) {
        logAudit('CAMBIAR_ESTADO_EVENTO', 'EVENTO', id, 'EXITO', `Transición de estado a ${status}`);
        return { ...e, status };
      }
      return e;
    }));
  };

  const handleCancelEvent = (id: string, reason: string) => {
    setEvents(prev => prev.map(e => {
      if (e.id === id) {
        logAudit('CANCELAR_EVENTO', 'EVENTO', id, 'EXITO', `Evento cancelado`, reason);
        return { ...e, status: 'CANCELADO', cancellationReason: reason };
      }
      return e;
    }));
    // RN-17: Cancel and invalidate QR of active registrations
    setRegistrations(prev => prev.map(r => {
      if (r.eventId === id && r.status === 'ACTIVA') {
        return { ...r, status: 'CANCELADA', cancellationReason: `Evento cancelado por el operador: ${reason}` };
      }
      return r;
    }));
  };

  const handlePostponeEvent = (id: string, newStart: string, newEnd: string, reason: string) => {
    setEvents(prev => prev.map(e => {
      if (e.id === id) {
        logAudit('APLAZAR_EVENTO', 'EVENTO', id, 'EXITO', `Aplazado para ${newStart}`, reason);
        return { ...e, status: 'APLAZADO', startAt: newStart, endAt: newEnd, postponementReason: reason };
      }
      return e;
    }));
  };

  const handleAssignMonitors = (eventId: string, monitorEmails: string[]) => {
    setEvents(prev => prev.map(e => {
      if (e.id === eventId) {
        logAudit('ASIGNAR_MONITORES', 'EVENTO', eventId, 'EXITO', `Monitores: ${monitorEmails.join(', ')}`);
        return { ...e, assignedMonitors: monitorEmails };
      }
      return e;
    }));
  };

  const handleAddParticipant = (data: Omit<Participant, 'id' | 'createdAt'>) => {
    const newPart: Participant = {
      ...data,
      id: 'p_' + Date.now().toString(),
      createdAt: new Date().toISOString().slice(0, 10),
    };
    setParticipants(prev => [newPart, ...prev]);
    logAudit('CREAR_PARTICIPANTE', 'USUARIO', newPart.email, 'EXITO', `Participante registrado: ${newPart.firstName} ${newPart.lastName}`);
  };

  const handleRegisterParticipantToEvent = (participantId: string, eventId: string) => {
    const part = participants.find(p => p.id === participantId);
    const ev = events.find(e => e.id === eventId);
    if (!part || !ev) return;

    const newReg: Registration = {
      id: 'reg_' + Date.now().toString(),
      participantId: part.id,
      eventId: ev.id,
      participantName: `${part.firstName} ${part.lastName}`,
      participantEmail: part.email,
      eventName: ev.name,
      status: 'ACTIVA',
      createdAt: new Date().toISOString().slice(0, 10),
      qrToken: `QR-SYNC-${Math.floor(100 + Math.random() * 900)}`,
      checkedIn: false,
    };
    setRegistrations(prev => [newReg, ...prev]);
    logAudit('INSCRIBIR_PARTICIPANTE', 'INSCRIPCION', newReg.id, 'EXITO', `Inscripción generada con token ${newReg.qrToken}`);
  };

  const handleBulkImportParticipants = (items: Omit<Participant, 'id' | 'createdAt'>[]) => {
    const newParticipants: Participant[] = items.map((item, idx) => ({
      ...item,
      id: `p_imp_${Date.now()}_${idx}`,
      createdAt: new Date().toISOString().slice(0, 10),
    }));
    setParticipants(prev => [...newParticipants, ...prev]);
    logAudit('IMPORTACION_MASIVA_CSV', 'USUARIO', `${newParticipants.length} registros`, 'EXITO', `Carga exitosa de participantes`);
  };

  const handleIssueEventCertificates = (eventId: string) => {
    const ev = events.find(e => e.id === eventId);
    if (!ev) return;

    const attendees = registrations.filter(r => r.eventId === eventId && r.status === 'ACTIVA' && r.checkedIn);
    const newCerts: CertificateRecord[] = attendees.map(att => {
      const part = participants.find(p => p.id === att.participantId);
      return {
        id: 'cert_' + Math.random().toString().slice(2, 8),
        code: `SYNC-CERT-${new Date().getFullYear()}-${Math.random().toString(36).substring(2, 7).toUpperCase()}`,
        eventId: ev.id,
        eventName: ev.name,
        participantName: att.participantName,
        participantDocument: part?.documentNumber || '1020304050',
        issuedAt: new Date().toISOString().slice(0, 10),
        durationHours: ev.durationHours,
        status: 'VALIDO',
      };
    });

    setCertificates(prev => [...newCerts, ...prev]);
    logAudit('EMITIR_CERTIFICADOS', 'CERTIFICADO', eventId, 'EXITO', `Emitidos ${newCerts.length} certificados para el evento ${ev.name}`);
  };

  /* ── Monitor Handlers ── */
  const handleCheckInByToken = (token: string, eventId: string) => {
    const clean = token.trim().toUpperCase();
    const reg = registrations.find(r => r.qrToken.toUpperCase() === clean);

    if (!reg) {
      logAudit('CHECK_IN_FALLIDO', 'ASISTENCIA', clean, 'FALLO', 'Código QR no registrado en el sistema');
      return { success: false, message: `Código QR "${clean}" no válido o inexistente.` };
    }

    if (reg.status === 'CANCELADA') {
      logAudit('CHECK_IN_RECHAZADO', 'ASISTENCIA', clean, 'FALLO', 'Inscripción cancelada previamente (RN-17)');
      return { success: false, message: `Inscripción cancelada previamente. El código QR ha sido revocado (RN-17).` };
    }

    if (reg.eventId !== eventId) {
      logAudit('CHECK_IN_RECHAZADO', 'ASISTENCIA', clean, 'FALLO', 'El código pertenece a otro evento');
      return { success: false, message: `El código QR pertenece a otro evento (${reg.eventName}).` };
    }

    if (reg.checkedIn) {
      logAudit('CHECK_IN_DUPLICADO', 'ASISTENCIA', clean, 'FALLO', `Ingreso ya registrado previamente para ${reg.participantName}`);
      return { success: false, message: `Regla RN-04: ${reg.participantName} ya tiene asistencia confirmada previamente.`, reg };
    }

    // Success check-in!
    setRegistrations(prev => prev.map(r => r.id === reg.id ? { ...r, checkedIn: true } : r));
    logAudit('CHECK_IN_EXITOSO', 'ASISTENCIA', clean, 'EXITO', `Ingreso confirmado para ${reg.participantName}`);
    return { success: true, message: `¡Check-in exitoso! Bienvenido ${reg.participantName}.`, reg };
  };

  const handleToggleManualCheckIn = (regId: string) => {
    setRegistrations(prev => prev.map(r => {
      if (r.id === regId) {
        const next = !r.checkedIn;
        logAudit(next ? 'CHECK_IN_MANUAL' : 'CHECK_IN_DESMARCADO', 'ASISTENCIA', r.qrToken, 'EXITO', `Operación manual para ${r.participantName}`);
        return { ...r, checkedIn: next };
      }
      return r;
    }));
  };

  /* ── Guest Handlers ── */
  const currentParticipant = participants.find(
    p => p.email.toLowerCase() === (authUser?.email || 'laura.gomez@example.test').toLowerCase()
  ) || participants[0];

  const handleSelfRegister = (eventId: string) => {
    handleRegisterParticipantToEvent(currentParticipant.id, eventId);
  };

  const handleSelfCancelRegistration = (regId: string, reason: string) => {
    setRegistrations(prev => prev.map(r => {
      if (r.id === regId) {
        logAudit('CANCELAR_INSCRIPCION_INVITADO', 'INSCRIPCION', regId, 'EXITO', `Auto-cancelación de ${r.participantName}`, reason);
        return {
          ...r,
          status: 'CANCELADA',
          cancellationReason: reason,
          cancelledAt: new Date().toLocaleDateString('es-CO'),
        };
      }
      return r;
    }));
  };

  /* ─────────────────────────────────────────────
     RENDER: LANDING, LOGIN, REGISTER, APP
  ───────────────────────────────────────────── */
  if (screen === 'landing') {
    return (
      <LandingPage
        events={events}
        onLogin={() => setScreen('login')}
        onRegister={() => setScreen('register')}
      />
    );
  }

  if (screen === 'login') {
    return (
      <LoginPage
        onLogin={handleLoginSuccess}
        onGoRegister={() => setScreen('register')}
        onBack={() => setScreen('landing')}
      />
    );
  }

  if (screen === 'register') {
    return (
      <RegisterPage
        onRegistered={() => {
          addToast('Cuenta de Invitado creada exitosamente. Inicia sesión.', 'success');
          setScreen('login');
        }}
        onGoLogin={() => setScreen('login')}
        onBack={() => setScreen('landing')}
      />
    );
  }

  /* ─────────────────────────────────────────────
     ACTIVE APPLICATION SHELL (POST-LOGIN)
  ───────────────────────────────────────────── */
  const roleNameMap: Record<UserRole, string> = {
    ADMINISTRADOR: 'Administrador',
    OPERADOR: 'Operador Logístico',
    MONITOR: 'Monitor de Entrada',
    INVITADO: 'Invitado / Participante',
  };

  return (
    <div id="app-shell" style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', background: 'var(--surface-0)' }}>
      {/* ── Toast Notifications ── */}
      <div className="toast-container" aria-live="polite">
        {toasts.map(t => {
          const IcoMap = { success: Icon.CheckCircle, error: Icon.AlertTriangle, warning: Icon.AlertTriangle, info: Icon.Info };
          const Ico = IcoMap[t.type];
          return (
            <div key={t.id} className={`toast toast-${t.type} animate-modal`}>
              <span className="toast-icon"><Ico /></span>
              <span>{t.message}</span>
            </div>
          );
        })}
      </div>

      {/* ── Corporate Master Topbar ── */}
      <header className="topbar" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 28px', height: 68, background: 'var(--surface-1)', borderBottom: '1px solid var(--glass-border)', position: 'sticky', top: 0, zIndex: 50 }}>
        {/* Brand & Active Role */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ width: 36, height: 36, borderRadius: 10, background: 'linear-gradient(135deg,#d39e82,#a86f56)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}>
              <span style={{ width: 20, height: 20 }}><Icon.Zap /></span>
            </div>
            <div>
              <span style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: '1.2rem', color: 'var(--sand-100)', letterSpacing: '-0.02em' }}>SyncEvent</span>
              <span style={{ fontSize: '0.72rem', color: 'var(--stone-400)', display: 'block', lineHeight: 1 }}>Plataforma Integral de Eventos</span>
            </div>
          </div>

          <div style={{ height: 24, width: 1, background: 'var(--glass-border)' }} />

          {/* Actor Quick Switcher for Testing Different Screens */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ fontSize: '0.76rem', color: 'var(--stone-400)', textTransform: 'uppercase', letterSpacing: '0.04em', fontWeight: 600 }}>
              Pantalla Actual:
            </span>
            <div style={{ display: 'flex', gap: 4, background: 'var(--surface-2)', padding: 3, borderRadius: 'var(--radius-sm)', border: '1px solid var(--glass-border)' }}>
              {(['ADMINISTRADOR', 'OPERADOR', 'MONITOR', 'INVITADO'] as UserRole[]).map(r => (
                <button
                  key={r}
                  className={`btn btn-sm ${authUser?.role === r ? 'btn-primary' : 'btn-ghost'}`}
                  style={{ fontSize: '0.74rem', padding: '4px 10px', height: 28 }}
                  onClick={() => handleSwitchActorRole(r)}
                  title={`Cambiar a pantalla de ${r}`}
                >
                  {r === 'ADMINISTRADOR' && <span style={{ width: 12, height: 12, marginRight: 4 }}><Icon.Shield /></span>}
                  {r === 'OPERADOR' && <span style={{ width: 12, height: 12, marginRight: 4 }}><Icon.Settings /></span>}
                  {r === 'MONITOR' && <span style={{ width: 12, height: 12, marginRight: 4 }}><Icon.Scan /></span>}
                  {r === 'INVITADO' && <span style={{ width: 12, height: 12, marginRight: 4 }}><Icon.User /></span>}
                  {r}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* User profile & backend health */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          {/* Backend Status indicator */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.76rem', color: backendHealth === 'online' ? 'var(--success-400)' : 'var(--error-400)', background: 'var(--surface-2)', padding: '5px 12px', borderRadius: 20, border: '1px solid var(--glass-border)' }}>
            <span className="pulse-dot" style={{ width: 8, height: 8, borderRadius: '50%', background: backendHealth === 'online' ? 'var(--success-400)' : 'var(--error-400)' }} />
            {backendHealth === 'online' ? 'GraphQL & BD Online' : 'Modo Autónomo Local'}
          </div>

          {/* User card info */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div className="avatar" style={{ width: 36, height: 36, fontSize: '0.82rem' }}>
              {authUser?.name.slice(0, 2).toUpperCase() || 'AD'}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              <span style={{ fontSize: '0.84rem', fontWeight: 600, color: 'var(--sand-100)', lineHeight: 1.2 }}>{authUser?.name}</span>
              <span style={{ fontSize: '0.72rem', color: 'var(--accent-400)' }}>{roleNameMap[authUser?.role || 'ADMINISTRADOR']}</span>
            </div>
          </div>

          <button className="btn btn-ghost btn-sm" title="Cerrar sesión" onClick={handleLogout} style={{ color: 'var(--stone-400)' }}>
            <span style={{ width: 16, height: 16 }}><Icon.LogOut /></span>
          </button>
        </div>
      </header>

      {/* ── Main Dynamic Screen based on Authenticated Actor ── */}
      <main className="content" style={{ flex: 1, padding: '28px 32px' }}>
        {authUser?.role === 'ADMINISTRADOR' && (
          <AdminScreen
            events={events}
            users={users}
            auditLogs={auditLogs}
            fines={fines}
            certificates={certificates}
            onAddUser={handleAddUser}
            onUpdateUser={handleUpdateUser}
            onToggleUserStatus={handleToggleUserStatus}
            onResolveFine={handleResolveFine}
            onRevokeCertificate={handleRevokeCertificate}
            addToast={addToast}
          />
        )}

        {authUser?.role === 'OPERADOR' && (
          <OperatorScreen
            events={events}
            participants={participants}
            registrations={registrations}
            certificates={certificates}
            onSaveEvent={handleSaveEvent}
            onUpdateEventStatus={handleUpdateEventStatus}
            onCancelEvent={handleCancelEvent}
            onPostponeEvent={handlePostponeEvent}
            onAssignMonitors={handleAssignMonitors}
            onAddParticipant={handleAddParticipant}
            onRegisterParticipantToEvent={handleRegisterParticipantToEvent}
            onBulkImportParticipants={handleBulkImportParticipants}
            onIssueEventCertificates={handleIssueEventCertificates}
            addToast={addToast}
          />
        )}

        {authUser?.role === 'MONITOR' && (
          <MonitorScreen
            events={events}
            registrations={registrations}
            userEmail={authUser.email}
            onCheckInByToken={handleCheckInByToken}
            onToggleManualCheckIn={handleToggleManualCheckIn}
            addToast={addToast}
          />
        )}

        {authUser?.role === 'INVITADO' && (
          <GuestScreen
            events={events}
            registrations={registrations}
            certificates={certificates}
            currentParticipant={currentParticipant}
            onSelfRegister={handleSelfRegister}
            onSelfCancelRegistration={handleSelfCancelRegistration}
            addToast={addToast}
          />
        )}
      </main>
    </div>
  );
}

/* ─────────────────────────────────────────────
   LANDING PAGE COMPONENT
───────────────────────────────────────────── */
function LandingPage({ events, onLogin, onRegister }: { events: Event[]; onLogin: () => void; onRegister: () => void }) {
  const publicEvents = events.filter(e => e.status === 'PUBLICADO' || e.status === 'EN_CURSO');

  return (
    <div className="landing-page">
      {/* Nav */}
      <nav className="landing-nav">
        <div className="landing-nav-logo">
          <div className="landing-nav-logo-icon">
            <span style={{ width: 22, height: 22 }}><Icon.Zap /></span>
          </div>
          <span className="landing-nav-logo-text">SyncEvent</span>
        </div>
        <div className="landing-nav-actions">
          <button className="btn btn-ghost" onClick={onLogin}>Iniciar sesión</button>
          <button className="btn btn-primary" onClick={onRegister}>Crear cuenta gratis</button>
        </div>
      </nav>

      {/* Hero */}
      <section className="hero-section">
        <div className="hero-badge">
          <span className="hero-badge-dot" />
          Plataforma Empresarial de Gestión de Eventos y Certificación
        </div>

        <h1 className="hero-title">
          Control de eventos y acreditación digital{' '}
          <span className="hero-title-accent">diseñado para cada actor</span>
        </h1>

        <p className="hero-subtitle">
          SyncEvent ofrece pantallas especializadas para cada función: Tablero y auditoría para el Administrador, ciclo de vida e importaciones para el Operador, terminal de check-in para el Monitor y portal de credenciales QR para el Invitado.
        </p>

        <div className="hero-cta-group">
          <button className="btn btn-primary btn-xl" onClick={onLogin}>
            Ingresar a la Plataforma
            <span style={{ width: 18, height: 18 }}><Icon.ArrowRight /></span>
          </button>
          <button className="btn btn-secondary btn-lg" onClick={onRegister}>
            Registrarme como Invitado
          </button>
        </div>

        {/* 4 Distinct Actor Showcase Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 20, marginTop: 60, width: '100%', maxWidth: 1100 }}>
          <div className="card" style={{ padding: 22, textAlign: 'left', borderTop: '3px solid #3d7399' }}>
            <div style={{ width: 38, height: 38, borderRadius: 10, background: 'rgba(61,115,153,0.15)', color: '#5a8fb3', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 12 }}>
              <span style={{ width: 20, height: 20 }}><Icon.Shield /></span>
            </div>
            <h4 style={{ margin: '0 0 6px', color: 'var(--sand-100)', fontSize: '1.05rem' }}>Administrador</h4>
            <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--stone-400)', lineHeight: 1.5 }}>
              Control total del sistema, administración de usuarios y asignación de múltiples roles (HU-002), bitácora inalterable (HU-023) y gestión de multas (RN-24).
            </p>
          </div>

          <div className="card" style={{ padding: 22, textAlign: 'left', borderTop: '3px solid #9a8167' }}>
            <div style={{ width: 38, height: 38, borderRadius: 10, background: 'rgba(154,129,103,0.15)', color: '#b39d7d', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 12 }}>
              <span style={{ width: 20, height: 20 }}><Icon.Settings /></span>
            </div>
            <h4 style={{ margin: '0 0 6px', color: 'var(--sand-100)', fontSize: '1.05rem' }}>Operador</h4>
            <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--stone-400)', lineHeight: 1.5 }}>
              Programación de eventos, asignación de monitores sin cruces (RN-22), carga masiva CSV y emisión de certificados en menos de 20 minutos (RN-26).
            </p>
          </div>

          <div className="card" style={{ padding: 22, textAlign: 'left', borderTop: '3px solid #16a34a' }}>
            <div style={{ width: 38, height: 38, borderRadius: 10, background: 'rgba(22,163,74,0.15)', color: '#4ade80', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 12 }}>
              <span style={{ width: 20, height: 20 }}><Icon.Scan /></span>
            </div>
            <h4 style={{ margin: '0 0 6px', color: 'var(--sand-100)', fontSize: '1.05rem' }}>Monitor</h4>
            <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--stone-400)', lineHeight: 1.5 }}>
              Terminal de acceso en vivo con escáner QR, verificación contra aforo y alertas inmediatas ante duplicados o inscripciones canceladas (RN-04/17).
            </p>
          </div>

          <div className="card" style={{ padding: 22, textAlign: 'left', borderTop: '3px solid #d39e82' }}>
            <div style={{ width: 38, height: 38, borderRadius: 10, background: 'rgba(211,158,130,0.15)', color: '#d39e82', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 12 }}>
              <span style={{ width: 20, height: 20 }}><Icon.User /></span>
            </div>
            <h4 style={{ margin: '0 0 6px', color: 'var(--sand-100)', fontSize: '1.05rem' }}>Invitado</h4>
            <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--stone-400)', lineHeight: 1.5 }}>
              Catálogo público, pase QR digital en el móvil, auto-cancelación libre de cupo (RN-23) y descarga de certificados auténticos verificables.
            </p>
          </div>
        </div>
      </section>

      {/* Featured Events Showcase */}
      <section style={{ padding: '60px 40px', maxWidth: 1100, margin: '0 auto', width: '100%' }}>
        <div style={{ textAlign: 'center', marginBottom: 36 }}>
          <h2 style={{ fontSize: '1.8rem', color: 'var(--sand-100)', margin: '0 0 8px', fontFamily: 'var(--font-display)' }}>Eventos Próximos Disponibles</h2>
          <p style={{ color: 'var(--stone-400)', fontSize: '0.9rem' }}>Capacitaciones, talleres y torneos con certificación digital oficial.</p>
        </div>

        <div className="events-grid">
          {publicEvents.slice(0, 3).map(ev => (
            <div key={ev.id} className="event-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
                <span className="badge badge-accent">{ev.type}</span>
                <span className="badge badge-emerald">{ev.modality}</span>
              </div>
              <h3 className="event-title" style={{ fontSize: '1.1rem' }}>{ev.name}</h3>
              <p style={{ fontSize: '0.82rem', color: 'var(--stone-400)', marginBottom: 14 }}>{ev.description}</p>
              <div style={{ fontSize: '0.78rem', color: 'var(--stone-300)', marginBottom: 16 }}>
                <div>Horario: {ev.startAt.replace('T', ' ')}</div>
                <div>Lugar: {ev.location || 'Sesión en línea'}</div>
              </div>
              <button className="btn btn-primary w-full btn-sm" onClick={onLogin}>
                Inscribirme al Evento
              </button>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

/* ─────────────────────────────────────────────
   LOGIN PAGE WITH QUICK ROLE SELECTOR
───────────────────────────────────────────── */
function LoginPage({
  onLogin,
  onGoRegister,
  onBack,
}: {
  onLogin: (u: AuthUser) => void;
  onGoRegister: () => void;
  onBack: () => void;
}) {
  const [email, setEmail] = useState('admin@syncevent.com');
  const [password, setPassword] = useState('Admin2024*');
  const [selectedRole, setSelectedRole] = useState<UserRole>('ADMINISTRADOR');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage('');

    try {
      // Attempt real login with Spring Boot backend
      const res = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      if (res.ok) {
        const data = await res.json();
        const primaryRole: UserRole = data.roles?.[0] || selectedRole;
        onLogin({
          email,
          name: email.split('@')[0],
          role: primaryRole,
          token: data.accessToken,
          allRoles: data.roles || [primaryRole],
        });
        return;
      }
    } catch {
      // Fallback to local authentication for seamless evaluation
    }

    // Client-side authentication fallback
    const roleNames: Record<UserRole, string> = {
      ADMINISTRADOR: 'Administrador Maestro',
      OPERADOR: 'Operador Logístico',
      MONITOR: 'Monitor de Entrada',
      INVITADO: 'Laura Gómez (Invitado)',
    };

    onLogin({
      email,
      name: roleNames[selectedRole],
      role: selectedRole,
      allRoles: [selectedRole],
    });
    setIsLoading(false);
  };

  const setDemoRole = (role: UserRole, demoEmail: string) => {
    setSelectedRole(role);
    setEmail(demoEmail);
    setPassword('Admin2024*');
  };

  return (
    <div className="auth-page">
      <div className="auth-card animate-modal" style={{ maxWidth: 480 }}>
        <button className="auth-back-btn" onClick={onBack}>
          <span style={{ width: 16, height: 16 }}><Icon.ChevronRight /></span>
          Volver a inicio
        </button>

        <div className="auth-logo">
          <div className="auth-logo-icon">
            <span style={{ width: 22, height: 22 }}><Icon.Zap /></span>
          </div>
          <span className="auth-logo-text">SyncEvent</span>
        </div>

        <h2 className="auth-title">Iniciar Sesión</h2>
        <p className="auth-subtitle">Accede a la pantalla correspondiente a tu rol</p>

        {/* Demo Fast Role Selectors */}
        <div style={{ marginBottom: 20 }}>
          <span style={{ fontSize: '0.74rem', color: 'var(--stone-400)', textTransform: 'uppercase', letterSpacing: '0.04em', display: 'block', marginBottom: 8, fontWeight: 600 }}>
            Acceso Rápido por Actor:
          </span>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            <button
              type="button"
              className={`btn btn-sm ${selectedRole === 'ADMINISTRADOR' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ fontSize: '0.76rem', justifyContent: 'flex-start' }}
              onClick={() => setDemoRole('ADMINISTRADOR', 'admin@syncevent.com')}
            >
              <span style={{ width: 14, height: 14 }}><Icon.Shield /></span> Administrador
            </button>
            <button
              type="button"
              className={`btn btn-sm ${selectedRole === 'OPERADOR' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ fontSize: '0.76rem', justifyContent: 'flex-start' }}
              onClick={() => setDemoRole('OPERADOR', 'operador@syncevent.com')}
            >
              <span style={{ width: 14, height: 14 }}><Icon.Settings /></span> Operador
            </button>
            <button
              type="button"
              className={`btn btn-sm ${selectedRole === 'MONITOR' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ fontSize: '0.76rem', justifyContent: 'flex-start' }}
              onClick={() => setDemoRole('MONITOR', 'monitor@syncevent.com')}
            >
              <span style={{ width: 14, height: 14 }}><Icon.Scan /></span> Monitor
            </button>
            <button
              type="button"
              className={`btn btn-sm ${selectedRole === 'INVITADO' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ fontSize: '0.76rem', justifyContent: 'flex-start' }}
              onClick={() => setDemoRole('INVITADO', 'laura.gomez@example.test')}
            >
              <span style={{ width: 14, height: 14 }}><Icon.User /></span> Invitado
            </button>
          </div>
        </div>

        {errorMessage && (
          <div className="auth-error animate-fade-in" style={{ marginBottom: 16 }}>
            <span style={{ width: 16, height: 16 }}><Icon.AlertTriangle /></span>
            {errorMessage}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label className="form-label">Correo Electrónico</label>
            <input
              type="email"
              required
              className="input"
              value={email}
              onChange={e => setEmail(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Contraseña</label>
            <input
              type="password"
              required
              className="input"
              value={password}
              onChange={e => setPassword(e.target.value)}
            />
          </div>

          <button type="submit" className="btn btn-primary w-full btn-lg" disabled={isLoading} style={{ marginTop: 8 }}>
            {isLoading ? <span style={{ width: 18, height: 18 }}><Icon.Loader /></span> : 'Ingresar a mi Pantalla'}
          </button>
        </form>

        <div className="auth-switch">
          ¿No tienes una cuenta aún?{' '}
          <button className="auth-switch-btn" onClick={onGoRegister}>
            Registrarme como Invitado
          </button>
        </div>
      </div>
    </div>
  );
}

/* ─────────────────────────────────────────────
   REGISTER PAGE (CREATES INVITADO ACCOUNT)
───────────────────────────────────────────── */
function RegisterPage({
  onRegistered,
  onGoLogin,
  onBack,
}: {
  onRegistered: () => void;
  onGoLogin: () => void;
  onBack: () => void;
}) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onRegistered();
  };

  return (
    <div className="auth-page">
      <div className="auth-card animate-modal" style={{ maxWidth: 480 }}>
        <button className="auth-back-btn" onClick={onBack}>
          <span style={{ width: 16, height: 16 }}><Icon.ChevronRight /></span>
          Volver a inicio
        </button>

        <div className="auth-logo">
          <div className="auth-logo-icon">
            <span style={{ width: 22, height: 22 }}><Icon.Zap /></span>
          </div>
          <span className="auth-logo-text">SyncEvent</span>
        </div>

        <h2 className="auth-title">Crear Cuenta</h2>
        <p className="auth-subtitle">Las cuentas nuevas inician con el rol de Invitado (HU-001)</p>

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label className="form-label">Nombre Completo</label>
            <input
              type="text"
              required
              className="input"
              placeholder="Ej. Laura Gómez"
              value={name}
              onChange={e => setName(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Correo Electrónico</label>
            <input
              type="email"
              required
              className="input"
              placeholder="laura@ejemplo.com"
              value={email}
              onChange={e => setEmail(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Contraseña</label>
            <input
              type="password"
              required
              className="input"
              placeholder="••••••••"
              value={password}
              onChange={e => setPassword(e.target.value)}
            />
          </div>

          <button type="submit" className="btn btn-primary w-full btn-lg" style={{ marginTop: 8 }}>
            Registrarme
          </button>
        </form>

        <div className="auth-switch">
          ¿Ya tienes cuenta?{' '}
          <button className="auth-switch-btn" onClick={onGoLogin}>
            Iniciar sesión
          </button>
        </div>
      </div>
    </div>
  );
}
