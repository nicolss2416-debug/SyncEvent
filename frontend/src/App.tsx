import React, { useState, useEffect, useCallback } from 'react';
import './styles/styles.css';
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
  Toast,
  EstadoEvento,
  MonitorAvailability,
} from './types/types';
import { Icon } from './components/icons';
import { ThemeToggle } from './components/ThemeToggle';
import { AdminScreen } from './screens/AdminScreen';
import { OperatorScreen } from './screens/OperatorScreen';
import { MonitorScreen } from './screens/MonitorScreen';
import { GuestScreen } from './screens/GuestScreen';
import { LandingScreen } from './screens/LandingScreen';
import { LoginScreen } from './screens/LoginScreen';
import { RegisterScreen } from './screens/RegisterScreen';
import { API_BASE_URL } from './config/config';
import {
  roleNameMap,
  ROLE_ROUTE_MAP,
  ROUTE_ROLE_MAP,
  PROTECTED_ROUTES,
  normalizePath,
} from './config/routes';
import { getSavedSession } from './utils/session';

export { roleNameMap, ROLE_ROUTE_MAP, ROUTE_ROLE_MAP, PROTECTED_ROUTES };

interface BackendUserAccount {
  id: string;
  email: string;
  roles: UserRole[];
  active: boolean;
  createdAt: string;
}

interface AttendancePolicy {
  absenceThreshold: number;
  windowDays: number;
}

function mapBackendUser(account: BackendUserAccount): UserAccount {
  return { ...account, name: account.email };
}

/* ─────────────────────────────────────────────
   ROOT COMPONENT: APP
───────────────────────────────────────────── */
export default function App() {
  const [authUser, setAuthUser] = useState<AuthUser | null>(getSavedSession);
  const [currentPath, setCurrentPath] = useState<string>(() => normalizePath(window.location.pathname));

  // Core system collections
  const [events, setEvents] = useState<Event[]>([]);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [registrations, setRegistrations] = useState<Registration[]>([]);
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [fines, setFines] = useState<FineRecord[]>([]);
  const [attendancePolicy, setAttendancePolicy] = useState<AttendancePolicy>({ absenceThreshold: 3, windowDays: 90 });
  const [certificates, setCertificates] = useState<CertificateRecord[]>([]);

  // System indicators
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [backendHealth, setBackendHealth] = useState<'checking' | 'online' | 'offline'>('checking');

  const addToast = useCallback((message: string, type: Toast['type'] = 'info') => {
    const id = Date.now().toString() + Math.random().toString();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4500);
  }, []);

  const navigateTo = useCallback((newPath: string, replace = false) => {
    const clean = normalizePath(newPath);
    if (replace) {
      window.history.replaceState({}, '', clean);
    } else if (normalizePath(window.location.pathname) !== clean) {
      window.history.pushState({}, '', clean);
    }
    setCurrentPath(clean);
  }, []);

  // Escuchar botones de Atrás / Adelante del navegador
  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(normalizePath(window.location.pathname));
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  // Guardia de protección de URLs y sincronización de estado
  useEffect(() => {
    const path = currentPath;

    // Caso 1: Usuario NO autenticado
    if (!authUser) {
      if (PROTECTED_ROUTES.includes(path)) {
        sessionStorage.setItem('syncevent_redirect', path);
        navigateTo('/login', true);
        const requiredRole = ROUTE_ROLE_MAP[path];
        addToast(
          `Acceso protegido: inicia sesión para ingresar al módulo de ${roleNameMap[requiredRole] || 'este actor'}.`,
          'warning'
        );
      } else if (path !== '/' && path !== '/landing' && path !== '/login' && path !== '/registro' && path !== '/register') {
        navigateTo('/', true);
      }
      return;
    }

    // Caso 2: Usuario autenticado visita landing o auth (redirección a su módulo)
    if (path === '/' || path === '/landing' || path === '/login' || path === '/registro' || path === '/register') {
      const targetRolePath = ROLE_ROUTE_MAP[authUser.role] || '/invitado';
      navigateTo(targetRolePath, true);
      return;
    }

    // Caso 3: Usuario autenticado navega a una ruta protegida
    if (PROTECTED_ROUTES.includes(path)) {
      const targetRole = ROUTE_ROLE_MAP[path];
      if (targetRole) {
        const hasPermission = authUser.role === targetRole || (authUser.allRoles && authUser.allRoles.includes(targetRole));
        if (hasPermission) {
          if (authUser.role !== targetRole) {
            const updated: AuthUser = { ...authUser, role: targetRole };
            setAuthUser(updated);
            localStorage.setItem('syncevent_session', JSON.stringify(updated));
            addToast(`Cambiado a pantalla de actor: ${roleNameMap[targetRole]}`, 'info');
          }
        } else {
          addToast(
            `Acceso denegado: tu cuenta no cuenta con permisos para el módulo de ${roleNameMap[targetRole] || targetRole}.`,
            'error'
          );
          const fallbackPath = ROLE_ROUTE_MAP[authUser.role] || '/invitado';
          navigateTo(fallbackPath, true);
        }
      }
    } else {
      const targetRolePath = ROLE_ROUTE_MAP[authUser.role] || '/invitado';
      navigateTo(targetRolePath, true);
    }
  }, [currentPath, authUser, navigateTo, addToast]);

  const syncLivePermissions = useCallback(async () => {
    if (!authUser?.token) return;
    try {
      const meRes = await fetch(`${API_BASE_URL}/api/auth/me`, {
        headers: { Authorization: `Bearer ${authUser.token}` },
      });
      if (meRes.status === 401) {
        localStorage.removeItem('syncevent_session');
        setAuthUser(null);
        navigateTo('/login', true);
        return;
      }
      if (!meRes.ok) return;
      const meData = await meRes.json() as { accessToken?: string; roles?: UserRole[] };
      const liveRoles: UserRole[] = Array.isArray(meData.roles) && meData.roles.length > 0
        ? meData.roles
        : ['INVITADO'];
      const resolvedRole: UserRole = liveRoles.includes(authUser.role) ? authUser.role : liveRoles[0];
      const rolesChanged =
        resolvedRole !== authUser.role ||
        JSON.stringify(liveRoles) !== JSON.stringify(authUser.allRoles || []);
      if (rolesChanged) {
        const updatedSession: AuthUser = {
          ...authUser,
          token: meData.accessToken || authUser.token,
          role: resolvedRole,
          allRoles: liveRoles,
        };
        setAuthUser(updatedSession);
        localStorage.setItem('syncevent_session', JSON.stringify(updatedSession));
        const targetPath = ROLE_ROUTE_MAP[resolvedRole] || '/invitado';
        if (normalizePath(window.location.pathname) !== targetPath) {
          navigateTo(targetPath, true);
        }
        addToast(`Permisos actualizados desde la base de datos: ahora ingresas como ${roleNameMap[resolvedRole]}.`, 'info');
      }
    } catch {
      // Ignorar fallos de red temporales
    }
  }, [authUser, navigateTo, addToast]);

  useEffect(() => {
    if (!authUser?.token) return;
    const handleFocus = () => {
      void syncLivePermissions();
    };
    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [authUser?.token, syncLivePermissions]);

  const apiRequest = useCallback(async <T,>(path: string, options: RequestInit = {}): Promise<T> => {
    if (!authUser?.token) throw new Error('Inicia sesión para realizar esta operación.');
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${authUser.token}`,
        ...options.headers,
      },
    });
    if (!response.ok) {
      if (response.status === 401) {
        localStorage.removeItem('syncevent_session');
        setAuthUser(null);
        navigateTo('/login', true);
        addToast('Tu sesión ha expirado o no es válida. Por favor, inicia sesión nuevamente.', 'warning');
        throw new Error('Sesión expirada.');
      }
      if (response.status === 403) {
        void syncLivePermissions();
      }
      const problem = await response.json().catch(() => null);
      throw new Error(problem?.detail || problem?.title || 'No se pudo completar la operación.');
    }
    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  }, [authUser?.token, navigateTo, addToast, syncLivePermissions]);

  // Consulta la bitácora real almacenada en el backend (solo Administración).
  const loadAuditLogs = useCallback(async (lastHours?: number) => {
    if (authUser?.role !== 'ADMINISTRADOR') {
      setAuditLogs([]);
      return;
    }
    const query = new URLSearchParams({ page: '0', size: '200' });
    if (lastHours && lastHours > 0) query.set('lastHours', String(lastHours));
    const response = await apiRequest<{ items: AuditLog[] }>(`/api/admin/audit?${query.toString()}`);
    setAuditLogs((response.items || []).map(item => ({
      ...item,
      id: String(item.id),
      entityType: item.entityType as AuditLog['entityType'],
    })));
  }, [authUser?.role, apiRequest]);

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

  useEffect(() => {
    if (!authUser?.token) {
      setEvents([]);
      return;
    }

    async function loadGraphQLData(token: string) {
      try {
        let activeToken = token;
        let activeRole: UserRole = authUser?.role || 'INVITADO';
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
          const meData = await meRes.json() as { accessToken?: string; roles?: UserRole[] };
          const liveRoles: UserRole[] = Array.isArray(meData.roles) && meData.roles.length > 0
            ? meData.roles
            : ['INVITADO'];
          const resolvedRole: UserRole = liveRoles.includes(activeRole) ? activeRole : liveRoles[0];
          const rolesChanged =
            resolvedRole !== authUser?.role ||
            JSON.stringify(liveRoles) !== JSON.stringify(authUser?.allRoles || []);
          if (meData.accessToken) {
            activeToken = meData.accessToken;
          }
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
            addToast(`Permisos sincronizados con la base de datos: rol activo ${roleNameMap[resolvedRole]}.`, 'info');
            return;
          }
        }

        const res = await fetch(`${API_BASE_URL}/graphql`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${activeToken}` },
          body: JSON.stringify({
            query: `query {
              events { id name description type modality startAt endAt location meetingUrl capacity durationHours status registered }
            }`
          })
        });
        const json = await res.json();
        if (!res.ok || json.errors?.length) {
          throw new Error('No fue posible consultar los eventos autorizados.');
        }
        const fromBackend: Event[] = (json.data?.events || []).map((event: Event) => ({ ...event }));
        setEvents(fromBackend);
        setBackendHealth('online');
        if (activeRole === 'ADMINISTRADOR' || activeRole === 'OPERADOR') {
          // Completa la lista de monitores asignados que se muestra en el listado de eventos.
          const eventsWithMonitors = await Promise.all(fromBackend.map(async event => {
            try {
              const monitors = await apiRequest<MonitorAvailability[]>(`/api/events/${event.id}/monitors`);
              return {
                ...event,
                assignedMonitors: monitors
                  .filter(monitor => monitor.assignedToEvent)
                  .map(monitor => monitor.email),
              };
            } catch {
              return event;
            }
          }));
          setEvents(eventsWithMonitors);
        }
        if (authUser?.role === 'INVITADO') {
          let participant: Participant | null = null;
          try {
            participant = await apiRequest<Participant>('/api/participants/me');
          } catch {
            // Older guest accounts need to complete their participant profile once.
          }
          if (!participant) {
            setParticipants([]);
            setRegistrations([]);
            setCertificates([]);
            return;
          }
          setParticipants([participant]);
          const eventsById = new Map(fromBackend.map(event => [event.id, event]));
          const registrationsByEvent = await Promise.all(fromBackend.map(async event => {
            const ownRegistrations = await apiRequest<Array<{
              id: string;
              eventId: string;
              participantId: string;
              status: 'ACTIVA' | 'CANCELADA';
              qrToken: string;
              createdAt: string;
              checkedIn?: boolean;
            }>>(`/api/events/${event.id}/registrations/mine`);
            return ownRegistrations.map(registration => ({
              ...registration,
              participantName: `${participant.firstName} ${participant.lastName}`,
              participantEmail: participant.email,
              eventName: event.name,
              checkedIn: Boolean(registration.checkedIn),
            }));
          }));
          setRegistrations(registrationsByEvent.flat());
          const ownCertificates = await apiRequest<Array<{
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
          }>>('/api/certificates/mine');
          setCertificates(ownCertificates.map(certificate => {
            const ev = eventsById.get(certificate.eventId);
            const fallbackHours = certificate.eventStartAt && certificate.eventEndAt
              ? Math.max(0, Math.round(((Date.parse(certificate.eventEndAt) - Date.parse(certificate.eventStartAt)) / 3_600_000) * 10) / 10)
              : 0;
            return {
              id: certificate.id,
              code: certificate.verificationCode,
              eventId: certificate.eventId,
              eventName: certificate.eventName,
              participantName: certificate.participantName,
              participantDocument: certificate.participantDocumentNumber || participant.documentNumber || '',
              issuedAt: certificate.issuedAt,
              durationHours: ev?.durationHours || fallbackHours,
              status: certificate.status === 'VIGENTE' ? 'VALIDO' : 'REVOCADO',
              revocationReason: certificate.revocationReason,
            };
          }));
        } else if (authUser?.role === 'ADMINISTRADOR') {
          const [userPage, participantPage, finePage, policy, certificatePage] = await Promise.all([
            apiRequest<{ content: BackendUserAccount[] }>('/api/admin/users?page=0&size=100').catch(() => ({ content: [] })),
            apiRequest<{ content: Participant[] }>('/api/participants?q=&page=0&size=100').catch(() => ({ content: [] })),
            apiRequest<{ content: Array<{
              id: string;
              participantId: string;
              absenceThreshold: number;
              windowDays: number;
              status: string;
              createdAt: string;
              resolvedAt?: string;
              resolvedBy?: string;
            }> }>('/api/admin/fines?page=0&size=100').catch(() => ({ content: [] })),
            apiRequest<AttendancePolicy>('/api/admin/attendance-policy').catch(() => ({ absenceThreshold: 3, windowDays: 90 })),
            apiRequest<Array<{
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
            }>>('/api/admin/certificates').catch(() => []),
          ]);
          const eventsById = new Map(fromBackend.map(event => [event.id, event]));
          const participantsById = new Map((participantPage?.content || []).map(participant => [participant.id, participant]));
          const registrationsByEvent = await Promise.all(fromBackend.map(async event => {
            const eventRegistrations = await apiRequest<Array<{
              id: string;
              eventId: string;
              participantId: string;
              status: 'ACTIVA' | 'CANCELADA';
              qrToken: string;
              createdAt: string;
              checkedIn?: boolean;
            }>>(`/api/events/${event.id}/registrations`).catch(() => []);
            return eventRegistrations.map(registration => {
              const part = participantsById.get(registration.participantId);
              return {
                ...registration,
                participantName: part ? `${part.firstName} ${part.lastName}` : 'Participante',
                participantEmail: part?.email || '',
                eventName: event.name,
                checkedIn: Boolean(registration.checkedIn),
              };
            });
          }));
          setRegistrations(registrationsByEvent.flat());
          setUsers((userPage?.content || []).map(mapBackendUser));
          setParticipants(participantPage?.content || []);
          if (policy) setAttendancePolicy(policy);
          setCertificates(certificatePage.map(certificate => {
            const ev = eventsById.get(certificate.eventId);
            const fallbackHours = Math.max(0, Math.round(((Date.parse(certificate.eventEndAt) - Date.parse(certificate.eventStartAt)) / 3_600_000) * 10) / 10);
            return {
              id: certificate.id,
              code: certificate.verificationCode,
              eventId: certificate.eventId,
              eventName: certificate.eventName,
              participantName: certificate.participantName,
              participantDocument: certificate.participantDocumentNumber,
              issuedAt: certificate.issuedAt,
              durationHours: ev?.durationHours || fallbackHours,
              status: certificate.status === 'VIGENTE' ? 'VALIDO' : 'REVOCADO',
              revocationReason: certificate.revocationReason,
            };
          }));
          setFines(finePage.content.map(fine => {
            const participant = participantsById.get(fine.participantId);
            return {
              id: fine.id,
              participantId: fine.participantId,
              participantName: participant ? `${participant.firstName} ${participant.lastName}` : 'Perfil no disponible',
              participantEmail: participant?.email || '',
              documentNumber: participant?.documentNumber || '',
              missedEventsCount: fine.absenceThreshold + 1,
              periodDays: fine.windowDays,
              status: fine.status === 'PENDIENTE' ? 'ACTIVA' : 'RESUELTA',
              issuedAt: fine.createdAt,
              resolvedAt: fine.resolvedAt,
              resolvedBy: fine.resolvedBy,
            };
          }));
        } else if (authUser?.role === 'OPERADOR' || authUser?.role === 'MONITOR') {
          const [participantPage, certificateList] = await Promise.all([
            apiRequest<{ content: Participant[] }>('/api/participants?q=&page=0&size=100').catch(() => ({ content: [] })),
            authUser.role === 'OPERADOR'
              ? apiRequest<Array<{
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
                }>>('/api/admin/certificates').catch(() => [])
              : Promise.resolve([]),
          ]);
          const eventsById = new Map(fromBackend.map(event => [event.id, event]));
          const participantsById = new Map(participantPage.content.map(participant => [participant.id, participant]));
          const registrationsByEvent = await Promise.all(fromBackend.map(async event => {
            const eventRegistrations = await apiRequest<Array<{
              id: string;
              eventId: string;
              participantId: string;
              status: 'ACTIVA' | 'CANCELADA';
              qrToken: string;
              createdAt: string;
              checkedIn?: boolean;
            }>>(`/api/events/${event.id}/registrations`).catch(() => []);
            return eventRegistrations.map(registration => {
              const part = participantsById.get(registration.participantId);
              return {
                ...registration,
                participantName: part ? `${part.firstName} ${part.lastName}` : 'Participante',
                participantEmail: part?.email || '',
                eventName: event.name,
                checkedIn: Boolean(registration.checkedIn),
              };
            });
          }));
          setParticipants(participantPage.content);
          setCertificates(certificateList.map(certificate => {
            const ev = eventsById.get(certificate.eventId);
            const fallbackHours = Math.max(0, Math.round(((Date.parse(certificate.eventEndAt) - Date.parse(certificate.eventStartAt)) / 3_600_000) * 10) / 10);
            return {
              id: certificate.id,
              code: certificate.verificationCode,
              eventId: certificate.eventId,
              eventName: certificate.eventName,
              participantName: certificate.participantName,
              participantDocument: certificate.participantDocumentNumber,
              issuedAt: certificate.issuedAt,
              durationHours: ev?.durationHours || fallbackHours,
              status: certificate.status === 'VIGENTE' ? 'VALIDO' : 'REVOCADO',
              revocationReason: certificate.revocationReason,
            };
          }));
          setRegistrations(registrationsByEvent.flat());
          setUsers([]);
          setFines([]);
        } else {
          setParticipants([]);
          setRegistrations([]);
          setUsers([]);
          setFines([]);
          setCertificates([]);
        }
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
  }, [authUser?.token, authUser?.role, apiRequest]);

  /* ── Auth Flow ── */
  const handleLoginSuccess = (user: AuthUser) => {
    setAuthUser(user);
    localStorage.setItem('syncevent_session', JSON.stringify(user));
    addToast(`¡Bienvenido a SyncEvent, ${user.name}! Accediendo como ${user.role}.`, 'success');

    // Revisar si intentaba ingresar a una ruta protegida previamente
    const savedRedirect = sessionStorage.getItem('syncevent_redirect');
    sessionStorage.removeItem('syncevent_redirect');
    if (savedRedirect && ROUTE_ROLE_MAP[savedRedirect] && user.allRoles?.includes(ROUTE_ROLE_MAP[savedRedirect])) {
      const redirectRole = ROUTE_ROLE_MAP[savedRedirect];
      const updatedUser = { ...user, role: redirectRole };
      setAuthUser(updatedUser);
      localStorage.setItem('syncevent_session', JSON.stringify(updatedUser));
      navigateTo(savedRedirect, true);
    } else {
      navigateTo(ROLE_ROUTE_MAP[user.role] || '/invitado', true);
    }
  };

  const handleLogout = () => {
    if (authUser) {
      if (authUser.token) {
        void fetch(`${API_BASE_URL}/api/auth/logout`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${authUser.token}` },
        }).catch(() => undefined);
      }
    }
    localStorage.removeItem('syncevent_session');
    sessionStorage.removeItem('syncevent_redirect');
    setAuthUser(null);
    setEvents([]);
    setBackendHealth('checking');
    navigateTo('/', true);
    addToast('Sesión cerrada correctamente', 'info');
  };

  const handleDownloadCertificate = async (certificateId: string) => {
    if (!authUser?.token) return;
    try {
      const response = await fetch(`${API_BASE_URL}/api/certificates/${certificateId}/download`, {
        headers: { Authorization: `Bearer ${authUser.token}` },
      });
      if (!response.ok) {
        const problem = await response.json().catch(() => null);
        throw new Error(problem?.detail || 'No se pudo descargar el certificado.');
      }
      const pdf = await response.blob();
      const url = URL.createObjectURL(pdf);
      const link = document.createElement('a');
      link.href = url;
      link.download = `certificado-${certificateId}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo descargar el certificado.', 'error');
    }
  };

  /* ── Permitted role selection for accounts with multiple assigned roles ── */
  const handleSwitchActorRole = (newRole: UserRole) => {
    if (!authUser || !authUser.allRoles?.includes(newRole)) return;
    // Se conserva la identidad de la cuenta autenticada; solo cambia el rol activo.
    const updatedUser = {
      ...authUser,
      role: newRole,
    };
    setAuthUser(updatedUser);
    localStorage.setItem('syncevent_session', JSON.stringify(updatedUser));
    navigateTo(ROLE_ROUTE_MAP[newRole] || '/invitado');
    addToast(`Cambiado a pantalla de actor: ${roleNameMap[newRole] || newRole}`, 'info');
  };

  /* ── Admin Handlers ── */
  const handleAddUser = async (user: Omit<UserAccount, 'id' | 'createdAt'> & { password: string }): Promise<boolean> => {
    try {
      const created = await apiRequest<BackendUserAccount>('/api/admin/users', {
        method: 'POST',
        body: JSON.stringify({ email: user.email, password: user.password, roles: user.roles }),
      });
      setUsers(previous => [mapBackendUser(created), ...previous]);
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo crear la cuenta.', 'error');
      return false;
    }
  };

  const handleUpdateUser = async (id: string, updates: Partial<UserAccount>): Promise<boolean> => {
    try {
      let updated = users.find(user => user.id === id);
      if (!updated) throw new Error('No se encontró la cuenta seleccionada.');
      if (updates.email && updates.email !== updated.email) {
        const result = await apiRequest<BackendUserAccount>(`/api/admin/users/${id}`, {
          method: 'PUT', body: JSON.stringify({ email: updates.email }),
        });
        updated = mapBackendUser(result);
      }
      const nextRole = updates.roles && updates.roles.length > 0 ? updates.roles[updates.roles.length - 1] : undefined;
      if (nextRole && (updated.roles.length !== 1 || updated.roles[0] !== nextRole)) {
        const result = await apiRequest<BackendUserAccount>(`/api/admin/users/${id}/roles`, {
          method: 'POST', body: JSON.stringify({ role: nextRole }),
        });
        updated = mapBackendUser(result);
      }
      setUsers(previous => previous.map(user => user.id === id ? updated! : user));
      if (authUser && updated && updated.email.toLowerCase() === authUser.email.toLowerCase()) {
        const liveRole = updated.roles[0] || 'INVITADO';
        const nextSession: AuthUser = {
          ...authUser,
          role: liveRole,
          allRoles: [liveRole],
        };
        setAuthUser(nextSession);
        localStorage.setItem('syncevent_session', JSON.stringify(nextSession));
      }
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo actualizar la cuenta.', 'error');
      return false;
    }
  };

  const handleToggleUserStatus = async (id: string): Promise<boolean> => {
    const user = users.find(item => item.id === id);
    if (!user) return false;
    try {
      const updated = await apiRequest<BackendUserAccount>(`/api/admin/users/${id}/active`, {
        method: 'PATCH', body: JSON.stringify({ active: !user.active }),
      });
      setUsers(previous => previous.map(item => item.id === id ? mapBackendUser(updated) : item));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo cambiar el estado de la cuenta.', 'error');
      return false;
    }
  };

  const handleResolveFine = async (fineId: string): Promise<boolean> => {
    try {
      await apiRequest(`/api/admin/fines/${fineId}/resolution`, { method: 'POST' });
      setFines(previous => previous.map(fine => fine.id === fineId
        ? { ...fine, status: 'RESUELTA', resolvedAt: new Date().toISOString(), resolvedBy: authUser?.email }
        : fine));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo resolver la multa.', 'error');
      return false;
    }
  };

  const handleUpdateAttendancePolicy = async (policy: AttendancePolicy): Promise<boolean> => {
    try {
      const saved = await apiRequest<AttendancePolicy>('/api/admin/attendance-policy', {
        method: 'PUT', body: JSON.stringify(policy),
      });
      setAttendancePolicy(saved);
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo actualizar la política.', 'error');
      return false;
    }
  };

  const handleRevokeCertificate = async (certId: string, reason: string): Promise<boolean> => {
    try {
      await apiRequest(`/api/certificates/${certId}/revoke`, {
        method: 'POST', body: JSON.stringify({ reason }),
      });
      setCertificates(previous => previous.map(certificate => certificate.id === certId
        ? { ...certificate, status: 'REVOCADO', revocationReason: reason }
        : certificate));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo revocar el certificado.', 'error');
      return false;
    }
  };

  /* ── Operator Handlers ── */
  const handleSaveEvent = async (data: Partial<Event>): Promise<boolean> => {
    try {
      const body = {
        name: data.name,
        description: data.description,
        type: data.type,
        modality: data.modality,
        startAt: data.startAt,
        endAt: data.endAt,
        location: data.location,
        meetingUrl: data.meetingUrl,
        capacity: data.capacity,
        durationHours: data.durationHours,
      };
      const saved = await apiRequest<Event>(data.id ? `/api/events/${data.id}` : '/api/events', {
        method: data.id ? 'PUT' : 'POST',
        body: JSON.stringify(body),
      });
      setEvents(previous => data.id
        ? previous.map(event => event.id === data.id ? saved : event)
        : [saved, ...previous]);
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo guardar el evento.', 'error');
      return false;
    }
  };

  const handleUpdateEventStatus = async (id: string, status: EstadoEvento): Promise<boolean> => {
    try {
      const updated = await apiRequest<Event>(`/api/events/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      });
      setEvents(previous => previous.map(event => event.id === id ? updated : event));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo actualizar el estado.', 'error');
      return false;
    }
  };

  const handleCancelEvent = async (id: string, reason: string): Promise<boolean> => {
    try {
      const updated = await apiRequest<Event>(`/api/events/${id}/cancellation`, {
        method: 'POST',
        body: JSON.stringify({ reason }),
      });
      setEvents(previous => previous.map(event => event.id === id ? updated : event));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo cancelar el evento.', 'error');
      return false;
    }
  };

  const handlePostponeEvent = async (id: string, newStart: string, newEnd: string, reason: string): Promise<boolean> => {
    try {
      const updated = await apiRequest<Event>(`/api/events/${id}/postponements`, {
        method: 'POST',
        body: JSON.stringify({ reason, newStartAt: newStart, newEndAt: newEnd }),
      });
      setEvents(previous => previous.map(event => event.id === id ? updated : event));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo aplazar el evento.', 'error');
      return false;
    }
  };

  const handleLoadMonitorAvailability = useCallback(async (eventId: string): Promise<MonitorAvailability[]> => {
    return apiRequest<MonitorAvailability[]>(`/api/events/${eventId}/monitors`);
  }, [apiRequest]);

  const handleAssignMonitors = async (eventId: string, monitorIds: string[]): Promise<boolean> => {
    try {
      const current = await apiRequest<MonitorAvailability[]>(`/api/events/${eventId}/monitors`);
      const assignedIds = current.filter(monitor => monitor.assignedToEvent).map(monitor => monitor.monitorId);
      const toAssign = monitorIds.filter(monitorId => !assignedIds.includes(monitorId));
      const toRemove = assignedIds.filter(monitorId => !monitorIds.includes(monitorId));

      for (const monitorId of toAssign) {
        await apiRequest(`/api/events/${eventId}/monitors`, {
          method: 'POST',
          body: JSON.stringify({ monitorId }),
        });
      }
      for (const monitorId of toRemove) {
        await apiRequest(`/api/events/${eventId}/monitors/${monitorId}`, { method: 'DELETE' });
      }

      const emailById = new Map(current.map(monitor => [monitor.monitorId, monitor.email]));
      const assignedEmails = monitorIds.map(monitorId => emailById.get(monitorId) || monitorId);
      setEvents(previous => previous.map(event => event.id === eventId
        ? { ...event, assignedMonitors: assignedEmails }
        : event));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo actualizar la asignación de monitores.', 'error');
      return false;
    }
  };

  const handleAddParticipant = async (data: Omit<Participant, 'id' | 'createdAt'>): Promise<boolean> => {
    try {
      const created = await apiRequest<Participant>('/api/participants', {
        method: 'POST', body: JSON.stringify(data),
      });
      setParticipants(previous => [created, ...previous]);
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo crear el participante.', 'error');
      return false;
    }
  };

  const handleRegisterParticipantToEvent = async (participantId: string, eventId: string): Promise<boolean> => {
    const part = participants.find(p => p.id === participantId);
    const ev = events.find(e => e.id === eventId);
    if (!part || !ev) return false;
    try {
      const created = await apiRequest<Omit<Registration, 'participantName' | 'participantEmail' | 'eventName' | 'checkedIn'>>(
        `/api/events/${eventId}/registrations`,
        { method: 'POST', body: JSON.stringify({ participantId }) },
      );
      setRegistrations(previous => [{
        ...created,
        participantName: `${part.firstName} ${part.lastName}`,
        participantEmail: part.email,
        eventName: ev.name,
        checkedIn: false,
      }, ...previous]);
      setEvents(previous => previous.map(item => item.id === eventId
        ? { ...item, registered: (item.registered ?? 0) + 1 }
        : item));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo inscribir al participante.', 'error');
      return false;
    }
  };

  const handleBulkImportParticipants = async (items: Omit<Participant, 'id' | 'createdAt'>[]): Promise<boolean> => {
    try {
      const createdList: Participant[] = [];
      for (const item of items) {
        const created = await apiRequest<Participant>('/api/participants', {
          method: 'POST',
          body: JSON.stringify({
            ...item,
            lastName: item.lastName?.trim() || '—',
            phone: item.phone?.replace(/\D/g, '') || '3000000000',
          }),
        });
        createdList.push(created);
      }
      setParticipants(previous => [...createdList, ...previous]);
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudieron importar todos los participantes.', 'error');
      return false;
    }
  };

  const handleIssueEventCertificates = async (eventId: string): Promise<boolean> => {
    try {
      const issued = await apiRequest<Array<{
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
      }>>(`/api/events/${eventId}/certificates`, { method: 'POST' });

      const ev = events.find(e => e.id === eventId);
      const mapped: CertificateRecord[] = issued.map(certificate => {
        const fallbackHours = Math.max(
          0,
          Math.round(((Date.parse(certificate.eventEndAt) - Date.parse(certificate.eventStartAt)) / 3_600_000) * 10) / 10,
        );
        return {
          id: certificate.id,
          code: certificate.verificationCode,
          eventId: certificate.eventId,
          eventName: certificate.eventName,
          participantName: certificate.participantName,
          participantDocument: certificate.participantDocumentNumber,
          issuedAt: certificate.issuedAt,
          durationHours: ev?.durationHours || fallbackHours,
          status: certificate.status === 'VIGENTE' ? 'VALIDO' : 'REVOCADO',
          revocationReason: certificate.revocationReason,
        };
      });

      setCertificates(previous => {
        const byId = new Map(previous.map(item => [item.id, item]));
        for (const cert of mapped) {
          byId.set(cert.id, cert);
        }
        return Array.from(byId.values());
      });
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudieron generar los certificados.', 'error');
      return false;
    }
  };

  /* ── Monitor Handlers ── */
  const handleCheckInByToken = useCallback(async (token: string, eventId: string) => {
    try {
      const attendance = await apiRequest<{
        registrationId: string;
        participantId: string;
        participantName: string;
        eventId: string;
      }>(`/api/events/${eventId}/attendance/qr`, {
        method: 'POST', body: JSON.stringify({ qrToken: token.trim() }),
      });
      const event = events.find(item => item.id === eventId);
      const existing = registrations.find(item => item.id === attendance.registrationId);
      const registration: Registration = {
        ...(existing || {
          id: attendance.registrationId,
          participantId: attendance.participantId,
          eventId,
          participantName: attendance.participantName,
          participantEmail: '',
          eventName: event?.name || '',
          status: 'ACTIVA' as const,
          createdAt: '',
          qrToken: token,
          checkedIn: true,
        }),
        checkedIn: true,
      };
      setRegistrations(previous => previous.some(item => item.id === registration.id)
        ? previous.map(item => item.id === registration.id ? registration : item)
        : [registration, ...previous]);
      return { success: true, message: `Asistencia registrada para ${attendance.participantName}.`, reg: registration };
    } catch (error) {
      return { success: false, message: error instanceof Error ? error.message : 'No se pudo validar el código.' };
    }
  }, [apiRequest, events, registrations]);

  const handleLoadAttendanceStats = useCallback(async (eventId: string) => {
    const result = await apiRequest<{
      data?: { attendanceLiveStats: {
        eventId: string;
        eventStatus: string;
        checkedInCount: number;
        activeRegistrationCount: number;
        capacity: number;
        occupancyPercentage: number;
        capacityAlert: boolean;
      } };
      errors?: Array<{ message: string }>;
    }>('/graphql', {
      method: 'POST',
      body: JSON.stringify({
        query: `query { attendanceLiveStats(eventId: "${eventId}") { eventId eventStatus checkedInCount activeRegistrationCount capacity occupancyPercentage capacityAlert } }`,
      }),
    });
    if (result.errors?.length || !result.data?.attendanceLiveStats) {
      throw new Error(result.errors?.[0]?.message || 'No se pudieron consultar las estadísticas.');
    }
    return result.data.attendanceLiveStats;
  }, [apiRequest]);

  const handleSearchAttendanceRegistrations = useCallback(async (eventId: string, query: string): Promise<Registration[]> => {
    try {
      const candidates = await apiRequest<Array<{
        registrationId: string;
        participantId: string;
        name: string;
        documentType: string;
        documentNumber: string;
      }>>(`/api/events/${eventId}/attendance/registrations?q=${encodeURIComponent(query)}`);
      const eventName = events.find(event => event.id === eventId)?.name || '';
      return candidates.map(candidate => ({
        id: candidate.registrationId,
        eventId,
        participantId: candidate.participantId,
        participantName: candidate.name,
        participantEmail: '',
        eventName,
        status: 'ACTIVA',
        createdAt: '',
        qrToken: '',
        checkedIn: false,
        documentType: candidate.documentType,
        documentNumber: candidate.documentNumber,
      }));
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo buscar a los participantes.', 'error');
      return [];
    }
  }, [addToast, apiRequest, events]);

  const handleManualCheckIn = useCallback(async (eventId: string, participantId: string): Promise<boolean> => {
    try {
      const attendance = await apiRequest<{ registrationId: string }>(`/api/events/${eventId}/attendance/manual`, {
        method: 'POST', body: JSON.stringify({ participantId }),
      });
      setRegistrations(previous => previous.map(registration => registration.id === attendance.registrationId
        ? { ...registration, checkedIn: true }
        : registration));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo registrar la asistencia.', 'error');
      return false;
    }
  }, [addToast, apiRequest]);

  /* ── Guest Handlers ── */
  const currentParticipant = participants.find(
    p => p.email.toLowerCase() === (authUser?.email || '').toLowerCase()
  ) || null;

  const handleSelfRegister = async (eventId: string): Promise<boolean> => {
    if (!currentParticipant) {
      addToast('Tu cuenta aún no está vinculada a una ficha de participante. Contacta al Administrador.', 'error');
      return false;
    }
    try {
      const registration = await apiRequest<Registration>(`/api/events/${eventId}/registrations/self`, {
        method: 'POST',
      });
      const event = events.find(item => item.id === eventId);
      if (event) {
        setRegistrations(previous => [{
          ...registration,
          participantName: `${currentParticipant.firstName} ${currentParticipant.lastName}`,
          participantEmail: currentParticipant.email,
          eventName: event.name,
          checkedIn: false,
        }, ...previous]);
        setEvents(previous => previous.map(item => item.id === eventId
          ? { ...item, registered: (item.registered ?? 0) + 1 }
          : item));
      }
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo completar la inscripción.', 'error');
      return false;
    }
  };

  const handleCompleteGuestProfile = async (profile: {
    documentType: string;
    documentNumber: string;
    firstName: string;
    lastName: string;
    phone: string;
  }): Promise<boolean> => {
    try {
      const participant = await apiRequest<Participant>('/api/auth/profile', {
        method: 'PUT',
        body: JSON.stringify(profile),
      });
      setParticipants([participant]);
      const eventsById = new Map(events.map(event => [event.id, event]));
      const [registrationGroups, ownCertificates] = await Promise.all([
        Promise.all(events.map(async event => {
          const ownRegistrations = await apiRequest<Array<{
            id: string;
            eventId: string;
            participantId: string;
            status: 'ACTIVA' | 'CANCELADA';
            qrToken: string;
            createdAt: string;
            checkedIn?: boolean;
          }>>(`/api/events/${event.id}/registrations/mine`);
          return ownRegistrations.map(registration => ({
            ...registration,
            participantName: `${participant.firstName} ${participant.lastName}`,
            participantEmail: participant.email,
            eventName: event.name,
            checkedIn: Boolean(registration.checkedIn),
          }));
        })),
        apiRequest<Array<{
          id: string;
          eventId: string;
          verificationCode: string;
          participantName: string;
          participantDocumentNumber?: string;
          eventName: string;
          issuedAt: string;
          eventStartAt?: string;
          eventEndAt?: string;
          status: string;
          revocationReason?: string;
        }>>('/api/certificates/mine'),
      ]);
      setRegistrations(registrationGroups.flat());
      setCertificates(ownCertificates.map(certificate => {
        const ev = eventsById.get(certificate.eventId);
        const fallbackHours = certificate.eventStartAt && certificate.eventEndAt
          ? Math.max(0, Math.round(((Date.parse(certificate.eventEndAt) - Date.parse(certificate.eventStartAt)) / 3_600_000) * 10) / 10)
          : 0;
        return {
          id: certificate.id,
          code: certificate.verificationCode,
          eventId: certificate.eventId,
          eventName: certificate.eventName,
          participantName: certificate.participantName,
          participantDocument: certificate.participantDocumentNumber || profile.documentNumber,
          issuedAt: certificate.issuedAt,
          durationHours: ev?.durationHours || fallbackHours,
          status: certificate.status === 'VIGENTE' ? 'VALIDO' : 'REVOCADO',
          revocationReason: certificate.revocationReason,
        };
      }));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo completar el perfil.', 'error');
      return false;
    }
  };

  const handleSelfCancelRegistration = async (regId: string, reason: string): Promise<boolean> => {
    const registration = registrations.find(item => item.id === regId);
    if (!registration) return false;
    try {
      await apiRequest(`/api/events/${registration.eventId}/registrations/${regId}/cancellation`, {
        method: 'POST',
        body: JSON.stringify({ reason }),
      });
      setRegistrations(previous => previous.map(item => item.id === regId
        ? { ...item, status: 'CANCELADA', cancellationReason: reason, cancelledAt: new Date().toISOString() }
        : item));
      setEvents(previous => previous.map(item => item.id === registration.eventId
        ? { ...item, registered: Math.max(0, (item.registered ?? 1) - 1) }
        : item));
      return true;
    } catch (error) {
      addToast(error instanceof Error ? error.message : 'No se pudo cancelar la inscripción.', 'error');
      return false;
    }
  };

  /* ─────────────────────────────────────────────
     RENDER: LANDING, LOGIN, REGISTER, APP
  ───────────────────────────────────────────── */
  if (!authUser) {
    if (currentPath === '/login') {
      return (
        <LoginScreen
          onLogin={handleLoginSuccess}
          onGoRegister={() => navigateTo('/registro')}
          onBack={() => navigateTo('/')}
        />
      );
    }

    if (currentPath === '/registro' || currentPath === '/register') {
      return (
        <RegisterScreen
          onRegistered={() => {
            addToast('Cuenta de Invitado creada exitosamente. Inicia sesión.', 'success');
            navigateTo('/login');
          }}
          onGoLogin={() => navigateTo('/login')}
          onBack={() => navigateTo('/')}
        />
      );
    }

    return (
      <LandingScreen
        events={events}
        onLogin={() => navigateTo('/login')}
        onRegister={() => navigateTo('/registro')}
      />
    );
  }

  /* ─────────────────────────────────────────────
     ACTIVE APPLICATION SHELL (POST-LOGIN)
  ───────────────────────────────────────────── */
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

          {authUser?.allRoles && authUser.allRoles.length > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ fontSize: '0.76rem', color: 'var(--stone-400)', fontWeight: 600 }}>
                Rol activo:
              </span>
              <div style={{ display: 'flex', gap: 4, background: 'var(--surface-2)', padding: 3, borderRadius: 'var(--radius-sm)', border: '1px solid var(--glass-border)' }}>
              {authUser.allRoles.map(r => (
                <button
                  key={r}
                  className={`btn btn-sm ${authUser?.role === r ? 'btn-primary' : 'btn-ghost'}`}
                  style={{ fontSize: '0.74rem', padding: '4px 10px', height: 28 }}
                  onClick={() => handleSwitchActorRole(r)}
                  title={`Usar permisos de ${roleNameMap[r]}`}
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
          )}
        </div>

        {/* User profile & backend health */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <ThemeToggle />
          {/* Backend Status indicator */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.76rem', color: backendHealth === 'online' ? 'var(--success-400)' : 'var(--error-400)', background: 'var(--surface-2)', padding: '5px 12px', borderRadius: 20, border: '1px solid var(--glass-border)' }}>
            <span className="pulse-dot" style={{ width: 8, height: 8, borderRadius: '50%', background: backendHealth === 'online' ? 'var(--success-400)' : 'var(--error-400)' }} />
            {backendHealth === 'online' ? 'Servidor conectado' : backendHealth === 'checking' ? 'Conectando…' : 'Servidor no disponible'}
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
            onRefreshAudit={loadAuditLogs}
            fines={fines}
            attendancePolicy={attendancePolicy}
            certificates={certificates}
            onAddUser={handleAddUser}
            onUpdateUser={handleUpdateUser}
            onToggleUserStatus={handleToggleUserStatus}
            onResolveFine={handleResolveFine}
            onUpdateAttendancePolicy={handleUpdateAttendancePolicy}
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
            onLoadMonitorAvailability={handleLoadMonitorAvailability}
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
            onSearchRegistrations={handleSearchAttendanceRegistrations}
            onManualCheckIn={handleManualCheckIn}
            onLoadStats={handleLoadAttendanceStats}
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
            onCompleteProfile={handleCompleteGuestProfile}
            onDownloadCertificate={handleDownloadCertificate}
            addToast={addToast}
          />
        )}
      </main>
    </div>
  );
}
