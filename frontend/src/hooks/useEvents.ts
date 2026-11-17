/**
 * useEvents — Custom hook que encapsula la carga y estado de eventos, participantes,
 * inscripciones, certificados, multas y política de asistencia desde el backend.
 *
 * Estrategia de carga:
 *  - Los datos se cargan vía GraphQL (eventos) + REST (el resto) en un único
 *    efecto que depende del token y del rol activo.
 *  - Cada rol recibe exactamente los datos que necesita (principio de mínimo privilegio).
 */
import { useState, useCallback, useEffect } from 'react';
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
} from '../types/types';
import { API_BASE_URL } from '../config/config';
import { ROLE_ROUTE_MAP, normalizePath } from '../config/routes';

interface AttendancePolicy {
  absenceThreshold: number;
  windowDays: number;
}

interface BackendUserAccount {
  id: string;
  email: string;
  roles: UserRole[];
  active: boolean;
  createdAt: string;
}

function mapBackendUser(account: BackendUserAccount): UserAccount {
  return { ...account, name: account.email };
}

interface UseEventsOptions {
  authUser: AuthUser | null;
  setAuthUser: React.Dispatch<React.SetStateAction<AuthUser | null>>;
  apiRequest: <T>(path: string, options?: RequestInit) => Promise<T>;
  navigateTo: (path: string, replace?: boolean) => void;
  addToast: (msg: string, type?: 'success' | 'info' | 'error' | 'warning') => void;
}

export interface UseEventsReturn {
  events: Event[];
  setEvents: React.Dispatch<React.SetStateAction<Event[]>>;
  participants: Participant[];
  setParticipants: React.Dispatch<React.SetStateAction<Participant[]>>;
  registrations: Registration[];
  setRegistrations: React.Dispatch<React.SetStateAction<Registration[]>>;
  users: UserAccount[];
  setUsers: React.Dispatch<React.SetStateAction<UserAccount[]>>;
  auditLogs: AuditLog[];
  fines: FineRecord[];
  certificates: CertificateRecord[];
  setCertificates: React.Dispatch<React.SetStateAction<CertificateRecord[]>>;
  attendancePolicy: AttendancePolicy;
  setAttendancePolicy: React.Dispatch<React.SetStateAction<AttendancePolicy>>;
  backendHealth: 'checking' | 'online' | 'offline';
  setBackendHealth: React.Dispatch<React.SetStateAction<'checking' | 'online' | 'offline'>>;
  loadAuditLogs: (lastHours?: number) => Promise<void>;
}

export function useEvents({
  authUser,
  setAuthUser,
  apiRequest,
  navigateTo,
  addToast,
}: UseEventsOptions): UseEventsReturn {
  const [events, setEvents] = useState<Event[]>([]);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [registrations, setRegistrations] = useState<Registration[]>([]);
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [fines, setFines] = useState<FineRecord[]>([]);
  const [attendancePolicy, setAttendancePolicy] = useState<AttendancePolicy>({
    absenceThreshold: 3,
    windowDays: 90,
  });
  const [certificates, setCertificates] = useState<CertificateRecord[]>([]);
  const [backendHealth, setBackendHealth] = useState<'checking' | 'online' | 'offline'>('checking');

  // ── Carga de bitácora de auditoría (solo ADMINISTRADOR) ────────────────────
  const loadAuditLogs = useCallback(
    async (lastHours?: number) => {
      if (authUser?.role !== 'ADMINISTRADOR') {
        setAuditLogs([]);
        return;
      }
      const query = new URLSearchParams({ page: '0', size: '200' });
      if (lastHours && lastHours > 0) query.set('lastHours', String(lastHours));
      const response = await apiRequest<{ items: AuditLog[] }>(`/api/admin/audit?${query.toString()}`);
      setAuditLogs(
        (response.items || []).map((item) => ({
          ...item,
          id: String(item.id),
          entityType: item.entityType as AuditLog['entityType'],
        })),
      );
    },
    [authUser?.role, apiRequest],
  );

  useEffect(() => {
    if (authUser?.role !== 'ADMINISTRADOR') {
      setAuditLogs([]);
      return;
    }
    let cancelled = false;
    void loadAuditLogs().catch(() => {
      if (!cancelled) setAuditLogs([]);
    });
    return () => {
      cancelled = true;
    };
  }, [authUser?.role, authUser?.token, loadAuditLogs]);

  // ── Carga principal: eventos + datos según rol ─────────────────────────────
  useEffect(() => {
    if (!authUser?.token) {
      setEvents([]);
      return;
    }

    async function loadGraphQLData(token: string) {
      try {
        let activeToken = token;
        let activeRole: UserRole = authUser?.role || 'INVITADO';

        // Verificar validez del token y obtener roles actuales
        const meRes = await fetch(`${API_BASE_URL}/api/auth/me`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (meRes.status === 401) {
          localStorage.removeItem('syncevent_session');
          setAuthUser(null);
          navigateTo('/login', true);
          return;
        }
        if (meRes.ok) {
          const meData = (await meRes.json()) as { accessToken?: string; roles?: UserRole[] };
          const liveRoles: UserRole[] =
            Array.isArray(meData.roles) && meData.roles.length > 0 ? meData.roles : ['INVITADO'];
          const resolvedRole: UserRole = liveRoles.includes(activeRole) ? activeRole : liveRoles[0];
          const rolesChanged =
            resolvedRole !== authUser?.role ||
            JSON.stringify(liveRoles) !== JSON.stringify(authUser?.allRoles || []);
          if (meData.accessToken) activeToken = meData.accessToken;
          activeRole = resolvedRole;
          if (rolesChanged && authUser) {
            const updatedSession: AuthUser = {
              ...authUser,
              token: activeToken,
              role: resolvedRole,
              allRoles: liveRoles,
            };
            setAuthUser(updatedSession);
            localStorage.setItem('syncevent_session', JSON.stringify(updatedSession));
            const targetPath = ROLE_ROUTE_MAP[resolvedRole] || '/invitado';
            if (normalizePath(window.location.pathname) !== targetPath) {
              navigateTo(targetPath, true);
            }
            addToast(
              `Permisos sincronizados con la base de datos: rol activo ${resolvedRole}.`,
              'info',
            );
            return;
          }
        }

        // Cargar eventos vía GraphQL
        const res = await fetch(`${API_BASE_URL}/graphql`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${activeToken}` },
          body: JSON.stringify({
            query: `query {
              events { id name description type modality startAt endAt location meetingUrl capacity durationHours status registered assignedMonitors }
            }`,
          }),
        });
        const json = (await res.json()) as { data?: { events?: Event[] }; errors?: unknown[] };
        if (!res.ok || (json.errors?.length ?? 0) > 0) {
          throw new Error('No fue posible consultar los eventos autorizados.');
        }
        const fromBackend: Event[] = (json.data?.events || []).map((event: Event) => ({ ...event }));
        setEvents(fromBackend);
        setBackendHealth('online');

        // ── INVITADO ────────────────────────────────────────────────────────
        if (activeRole === 'INVITADO') {
          let participant: Participant | null = null;
          try {
            participant = await apiRequest<Participant>('/api/participants/me');
          } catch {
            // Perfil de participante aún no creado
          }
          if (!participant) {
            setParticipants([]);
            setRegistrations([]);
            setCertificates([]);
            return;
          }
          setParticipants([participant]);

          const eventsById = new Map(fromBackend.map((e) => [e.id, e]));
          const ownRegistrations = await apiRequest<
            Array<{
              id: string;
              eventId: string;
              eventName: string;
              participantId: string;
              status: 'ACTIVA' | 'CANCELADA';
              qrToken: string;
              createdAt: string;
              checkedIn?: boolean;
            }>
          >('/api/registrations/mine');
          setRegistrations(
            ownRegistrations.map((reg) => ({
              ...reg,
              participantName: `${participant!.firstName} ${participant!.lastName}`,
              participantEmail: participant!.email,
              checkedIn: Boolean(reg.checkedIn),
            })),
          );

          const ownCertificates = await apiRequest<
            Array<{
              id: string;
              eventId: string;
              verificationCode: string;
              participantName: string;
              participantDocumentType?: string;
              participantDocumentNumber?: string;
              eventName: string;
              issuedAt: string;
              eventStartAt?: string;
              eventEndAt?: string;
              status: string;
              revocationReason?: string;
            }>
          >('/api/certificates/mine');
          setCertificates(
            ownCertificates.map((cert) => {
              const ev = eventsById.get(cert.eventId);
              const fallbackHours =
                cert.eventStartAt && cert.eventEndAt
                  ? Math.max(
                      0,
                      Math.round(
                        ((Date.parse(cert.eventEndAt) - Date.parse(cert.eventStartAt)) / 3_600_000) * 10,
                      ) / 10,
                    )
                  : 0;
              return {
                id: cert.id,
                code: cert.verificationCode,
                eventId: cert.eventId,
                eventName: cert.eventName,
                participantName: cert.participantName,
                participantDocument: cert.participantDocumentNumber || participant!.documentNumber || '',
                issuedAt: cert.issuedAt,
                durationHours: ev?.durationHours || fallbackHours,
                status: cert.status === 'VIGENTE' ? 'VALIDO' : 'REVOCADO',
                revocationReason: cert.revocationReason,
              };
            }),
          );
          return;
        }

        // ── ADMINISTRADOR ───────────────────────────────────────────────────
        if (activeRole === 'ADMINISTRADOR') {
          const [userPage, participantPage, finePage, policy, certificatePage] = await Promise.all([
            apiRequest<{ content: BackendUserAccount[] }>('/api/admin/users?page=0&size=100').catch(
              () => ({ content: [] }),
            ),
            apiRequest<{ content: Participant[] }>('/api/participants?q=&page=0&size=100').catch(
              () => ({ content: [] }),
            ),
            apiRequest<{
              content: Array<{
                id: string;
                participantId: string;
                absenceThreshold: number;
                windowDays: number;
                status: string;
                createdAt: string;
                resolvedAt?: string;
                resolvedBy?: string;
              }>;
            }>('/api/admin/fines?page=0&size=100').catch(() => ({ content: [] })),
            apiRequest<AttendancePolicy>('/api/admin/attendance-policy').catch(() => ({
              absenceThreshold: 3,
              windowDays: 90,
            })),
            apiRequest<
              Array<{
                id: string;
                eventId: string;
                verificationCode: string;
                participantName: string;
                participantDocumentNumber: string;
                eventName: string;
                issuedAt: string;
                eventStartAt: string;
                eventEndAt: string;
                status: string;
                revocationReason?: string;
              }>
            >('/api/admin/certificates').catch(() => []),
          ]);

          const eventsById = new Map(fromBackend.map((e) => [e.id, e]));
          const participantsById = new Map(
            (participantPage?.content || []).map((p) => [p.id, p]),
          );

          const registrationsByEvent = await Promise.all(
            fromBackend.map(async (event) => {
              const eventRegs = await apiRequest<
                Array<{
                  id: string;
                  eventId: string;
                  participantId: string;
                  status: 'ACTIVA' | 'CANCELADA';
                  qrToken: string;
                  createdAt: string;
                  checkedIn?: boolean;
                }>
              >(`/api/events/${event.id}/registrations`).catch(() => []);
              return eventRegs.map((reg) => {
                const part = participantsById.get(reg.participantId);
                return {
                  ...reg,
                  participantName: part ? `${part.firstName} ${part.lastName}` : 'Participante',
                  participantEmail: part?.email || '',
                  eventName: event.name,
                  checkedIn: Boolean(reg.checkedIn),
                };
              });
            }),
          );

          setRegistrations(registrationsByEvent.flat());
          setUsers((userPage?.content || []).map(mapBackendUser));
          setParticipants(participantPage?.content || []);
          if (policy) setAttendancePolicy(policy);
          setCertificates(
            certificatePage.map((cert) => {
              const ev = eventsById.get(cert.eventId);
              const fallbackHours = Math.max(
                0,
                Math.round(
                  ((Date.parse(cert.eventEndAt) - Date.parse(cert.eventStartAt)) / 3_600_000) * 10,
                ) / 10,
              );
              return {
                id: cert.id,
                code: cert.verificationCode,
                eventId: cert.eventId,
                eventName: cert.eventName,
                participantName: cert.participantName,
                participantDocument: cert.participantDocumentNumber,
                issuedAt: cert.issuedAt,
                durationHours: ev?.durationHours || fallbackHours,
                status: cert.status === 'VIGENTE' ? 'VALIDO' : 'REVOCADO',
                revocationReason: cert.revocationReason,
              };
            }),
          );
          setFines(
            finePage.content.map((fine) => {
              const p = participantsById.get(fine.participantId);
              return {
                id: fine.id,
                participantId: fine.participantId,
                participantName: p ? `${p.firstName} ${p.lastName}` : 'Perfil no disponible',
                participantEmail: p?.email || '',
                documentNumber: p?.documentNumber || '',
                missedEventsCount: fine.absenceThreshold + 1,
                periodDays: fine.windowDays,
                status: fine.status === 'PENDIENTE' ? 'ACTIVA' : 'RESUELTA',
                issuedAt: fine.createdAt,
                resolvedAt: fine.resolvedAt,
                resolvedBy: fine.resolvedBy,
              };
            }),
          );
          return;
        }

        // ── OPERADOR / MONITOR ──────────────────────────────────────────────
        if (activeRole === 'OPERADOR' || activeRole === 'MONITOR') {
          const [participantPage, certificateList] = await Promise.all([
            apiRequest<{ content: Participant[] }>('/api/participants?q=&page=0&size=100').catch(
              () => ({ content: [] }),
            ),
            activeRole === 'OPERADOR'
              ? apiRequest<
                  Array<{
                    id: string;
                    eventId: string;
                    verificationCode: string;
                    participantName: string;
                    participantDocumentNumber: string;
                    eventName: string;
                    issuedAt: string;
                    eventStartAt: string;
                    eventEndAt: string;
                    status: string;
                    revocationReason?: string;
                  }>
                >('/api/admin/certificates').catch(() => [])
              : Promise.resolve([]),
          ]);

          const eventsById = new Map(fromBackend.map((e) => [e.id, e]));
          const participantsById = new Map(participantPage.content.map((p) => [p.id, p]));

          const registrationsByEvent = await Promise.all(
            fromBackend.map(async (event) => {
              const eventRegs = await apiRequest<
                Array<{
                  id: string;
                  eventId: string;
                  participantId: string;
                  status: 'ACTIVA' | 'CANCELADA';
                  qrToken: string;
                  createdAt: string;
                  checkedIn?: boolean;
                }>
              >(`/api/events/${event.id}/registrations`).catch(() => []);
              return eventRegs.map((reg) => {
                const part = participantsById.get(reg.participantId);
                return {
                  ...reg,
                  participantName: part ? `${part.firstName} ${part.lastName}` : 'Participante',
                  participantEmail: part?.email || '',
                  eventName: event.name,
                  checkedIn: Boolean(reg.checkedIn),
                };
              });
            }),
          );

          setParticipants(participantPage.content);
          setCertificates(
            certificateList.map((cert) => {
              const ev = eventsById.get(cert.eventId);
              const fallbackHours = Math.max(
                0,
                Math.round(
                  ((Date.parse(cert.eventEndAt) - Date.parse(cert.eventStartAt)) / 3_600_000) * 10,
                ) / 10,
              );
              return {
                id: cert.id,
                code: cert.verificationCode,
                eventId: cert.eventId,
                eventName: cert.eventName,
                participantName: cert.participantName,
                participantDocument: cert.participantDocumentNumber,
                issuedAt: cert.issuedAt,
                durationHours: ev?.durationHours || fallbackHours,
                status: cert.status === 'VIGENTE' ? 'VALIDO' : 'REVOCADO',
                revocationReason: cert.revocationReason,
              };
            }),
          );
          setRegistrations(registrationsByEvent.flat());
          setUsers([]);
          setFines([]);
          return;
        }

        // Cualquier otro rol sin datos específicos
        setParticipants([]);
        setRegistrations([]);
        setUsers([]);
        setFines([]);
        setCertificates([]);
      } catch {
        setBackendHealth('offline');
        setEvents([]);
        setParticipants([]);
        setRegistrations([]);
        setUsers([]);
        setFines([]);
        setCertificates([]);
      }
    }

    void loadGraphQLData(authUser.token);
  }, [authUser?.token, authUser?.role, apiRequest, navigateTo, setAuthUser, addToast]);

  return {
    events,
    setEvents,
    participants,
    setParticipants,
    registrations,
    setRegistrations,
    users,
    setUsers,
    auditLogs,
    fines,
    certificates,
    setCertificates,
    attendancePolicy,
    setAttendancePolicy,
    backendHealth,
    setBackendHealth,
    loadAuditLogs,
  };
}
