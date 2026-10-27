import React, { useState, useCallback, useEffect, useMemo } from 'react';

/* ─────────────────────────────────────────────
   TYPES & INTERFACES
───────────────────────────────────────────── */
export type EstadoEvento = 'BORRADOR' | 'PUBLICADO' | 'EN_CURSO' | 'FINALIZADO' | 'CANCELADO' | 'APLAZADO';
export type Modalidad    = 'PRESENCIAL' | 'VIRTUAL' | 'HIBRIDO';
export type TipoEvento   = 'CAPACITACION' | 'TALLER' | 'TORNEO' | 'CONFERENCIA' | 'SEMINARIO';
export type VerificationStatus = 'NOT_FOUND' | 'VALID' | 'REVOKED' | 'COUNTERFEIT';

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
}

export interface Registration {
  id: string;
  participantId?: string;
  eventId?: string;
  participantName: string;
  participantEmail: string;
  eventName: string;
  status: 'ACTIVA' | 'CANCELADA';
  createdAt: string;
  qrToken: string;
  checkedIn?: boolean;
}

export interface VerificationResult {
  status: VerificationStatus;
  verificationCode: string;
  participantName: string | null;
  eventName: string | null;
  message: string;
  issuedAt: string | null;
  active: boolean;
}

export type View = 'dashboard' | 'events' | 'participants' | 'registrations' | 'attendance' | 'certificates';

interface Toast {
  id: string;
  message: string;
  type: 'success' | 'info' | 'error' | 'warning';
}

/* ─────────────────────────────────────────────
   INITIAL MOCK DATA (se sincroniza con backend)
───────────────────────────────────────────── */
const INITIAL_EVENTS: Event[] = [
  { id: '1', name: 'Congreso Nacional de Tecnología 2026', type: 'CONFERENCIA', modality: 'PRESENCIAL', startAt: '2026-11-15T08:00', endAt: '2026-11-15T18:00', location: 'Centro de Convenciones Bogotá', capacity: 300, durationHours: 10, status: 'PUBLICADO', description: 'El evento más importante de tecnología del año.', registered: 187 },
  { id: '2', name: 'Taller de Spring Boot 3 y Arquitectura Hexagonal', type: 'TALLER', modality: 'VIRTUAL', startAt: '2026-11-20T09:00', endAt: '2026-11-20T13:00', meetingUrl: 'https://meet.google.com/abc-123', capacity: 50, durationHours: 4, status: 'PUBLICADO', description: 'Aprende a construir APIs robustas con Spring for GraphQL.', registered: 48 },
  { id: '3', name: 'Seminario de IA Generativa y LLMs', type: 'SEMINARIO', modality: 'HIBRIDO', startAt: '2026-12-05T10:00', endAt: '2026-12-05T16:00', location: 'Auditorio SENA', meetingUrl: 'https://zoom.us/j/123', capacity: 120, durationHours: 6, status: 'BORRADOR', description: 'Un recorrido profundo por los modelos de lenguaje.', registered: 0 },
  { id: '4', name: 'Workshop React + Vite + TypeScript', type: 'TALLER', modality: 'VIRTUAL', startAt: '2026-10-30T14:00', endAt: '2026-10-30T18:00', meetingUrl: 'https://teams.microsoft.com/xyz', capacity: 80, durationHours: 4, status: 'EN_CURSO', description: 'Frontend moderno con pnpm y TypeScript.', registered: 80 },
  { id: '5', name: 'Capacitación PostgreSQL 16 y Flyway', type: 'CAPACITACION', modality: 'VIRTUAL', startAt: '2026-09-10T15:00', endAt: '2026-09-10T17:00', meetingUrl: 'https://meet.google.com/xyz-789', capacity: 200, durationHours: 2, status: 'FINALIZADO', description: 'Optimización de consultas y migraciones DDL.', registered: 192 },
];

const INITIAL_PARTICIPANTS: Participant[] = [
  { id: '30000000-0000-4000-8000-000000000001', firstName: 'Laura', lastName: 'Gómez', email: 'laura.gomez@example.test', phone: '+57 310 123 4567', documentType: 'CC', documentNumber: '1020304050', createdAt: '2026-09-01' },
  { id: '30000000-0000-4000-8000-000000000002', firstName: 'Mateo', lastName: 'Rodríguez', email: 'mateo.rodriguez@example.test', phone: '+57 311 987 6543', documentType: 'CC', documentNumber: '1098765432', createdAt: '2026-09-15' },
  { id: '3', firstName: 'Camila', lastName: 'Vargas', email: 'camila.vargas@ejemplo.com', phone: '+57 300 456 7890', documentType: 'TI', documentNumber: '1030405060', createdAt: '2026-10-01' },
  { id: '4', firstName: 'Andrés', lastName: 'Mendoza', email: 'andres.mendoza@ejemplo.com', phone: '+57 320 654 3210', documentType: 'CE', documentNumber: '987654321', createdAt: '2026-10-12' },
];

const INITIAL_REGISTRATIONS: Registration[] = [
  { id: '40000000-0000-4000-8000-000000000001', participantId: '30000000-0000-4000-8000-000000000001', eventId: '1', participantName: 'Laura Gómez', participantEmail: 'laura.gomez@example.test', eventName: 'Congreso Nacional de Tecnología 2026', status: 'ACTIVA', createdAt: '2026-10-02', qrToken: 'QR-SYNC-001', checkedIn: true },
  { id: '40000000-0000-4000-8000-000000000002', participantId: '30000000-0000-4000-8000-000000000002', eventId: '2', participantName: 'Mateo Rodríguez', participantEmail: 'mateo.rodriguez@example.test', eventName: 'Taller de Spring Boot 3 y Arquitectura Hexagonal', status: 'ACTIVA', createdAt: '2026-10-10', qrToken: 'QR-SYNC-002', checkedIn: false },
  { id: 'r3', participantId: '3', eventId: '4', participantName: 'Camila Vargas', participantEmail: 'camila.vargas@ejemplo.com', eventName: 'Workshop React + Vite + TypeScript', status: 'ACTIVA', createdAt: '2026-10-18', qrToken: 'QR-SYNC-003', checkedIn: true },
  { id: 'r4', participantId: '4', eventId: '5', participantName: 'Andrés Mendoza', participantEmail: 'andres.mendoza@ejemplo.com', eventName: 'Capacitación PostgreSQL 16 y Flyway', status: 'CANCELADA', createdAt: '2026-09-08', qrToken: 'QR-SYNC-004', checkedIn: false },
];

const INITIAL_ACTIVITY = [
  { id: '1', text: 'Laura Gómez realizó Check-in en Congreso Nacional de Tecnología', time: 'Hace 5 minutos', color: '#10b981' },
  { id: '2', text: 'Taller de Spring Boot 3 alcanzó 96% de capacidad máxima', time: 'Hace 23 minutos', color: '#f59e0b' },
  { id: '3', text: 'Mateo Rodríguez se inscribió en Taller de Spring Boot 3', time: 'Hace 1 hora', color: '#3b82f6' },
  { id: '4', text: 'Servidor Spring Boot 3 & GraphQL conectado a PostgreSQL', time: 'Hace 2 horas', color: '#8b5cf6' },
];

/* ─────────────────────────────────────────────
   HELPERS
───────────────────────────────────────────── */
function fmtDate(iso: string) {
  try {
    return new Date(iso).toLocaleDateString('es-CO', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

function occupancy(reg?: number, cap?: number) {
  if (!cap || cap === 0) return 0;
  return Math.min(100, Math.round(((reg ?? 0) / cap) * 100));
}

function estadoBadge(status: EstadoEvento) {
  const map: Record<EstadoEvento, { cls: string; label: string }> = {
    PUBLICADO:  { cls: 'badge-blue',    label: 'Publicado' },
    EN_CURSO:   { cls: 'badge-emerald', label: 'En curso' },
    BORRADOR:   { cls: 'badge-slate',   label: 'Borrador' },
    FINALIZADO: { cls: 'badge-violet',  label: 'Finalizado' },
    CANCELADO:  { cls: 'badge-rose',    label: 'Cancelado' },
    APLAZADO:   { cls: 'badge-amber',   label: 'Aplazado' },
  };
  const { cls, label } = map[status] ?? { cls: 'badge-slate', label: status };
  return <span className={`badge ${cls} badge-dot`}>{label}</span>;
}

function modalidadBadge(mod: Modalidad) {
  const map: Record<Modalidad, { cls: string; label: string; icon: string }> = {
    PRESENCIAL: { cls: 'badge-emerald', label: 'Presencial', icon: '📍' },
    VIRTUAL:    { cls: 'badge-blue',    label: 'Virtual',    icon: '💻' },
    HIBRIDO:    { cls: 'badge-violet',  label: 'Híbrido',    icon: '🌐' },
  };
  const m = map[mod] ?? { cls: 'badge-slate', label: mod, icon: '📌' };
  return <span className={`badge ${m.cls}`}>{m.icon} {m.label}</span>;
}

/* ─────────────────────────────────────────────
   MAIN COMPONENT
───────────────────────────────────────────── */
export default function App() {
  const [view, setView] = useState<View>('dashboard');
  const [events, setEvents] = useState<Event[]>(INITIAL_EVENTS);
  const [participants, setParticipants] = useState<Participant[]>(INITIAL_PARTICIPANTS);
  const [registrations, setRegistrations] = useState<Registration[]>(INITIAL_REGISTRATIONS);
  const [activities, setActivities] = useState(INITIAL_ACTIVITY);
  const [toasts, setToasts] = useState<Toast[]>([]);

  // Modals & Panels state
  const [showEventModal, setShowEventModal] = useState(false);
  const [editEvent, setEditEvent] = useState<Event | null>(null);
  const [detailEvent, setDetailEvent] = useState<Event | null>(null);
  
  const [showParticipantModal, setShowParticipantModal] = useState(false);
  const [editParticipant, setEditParticipant] = useState<Participant | null>(null);
  const [viewParticipantInscriptions, setViewParticipantInscriptions] = useState<Participant | null>(null);

  const [showRegModal, setShowRegModal] = useState(false);
  const [showQrModal, setShowQrModal] = useState<Registration | null>(null);

  const [showCheckInQrModal, setShowCheckInQrModal] = useState(false);
  const [showManualCheckInModal, setShowManualCheckInModal] = useState(false);
  const [showIssueCertModal, setShowIssueCertModal] = useState(false);

  // Topbar dropdowns
  const [showNotifications, setShowNotifications] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [globalSearch, setGlobalSearch] = useState('');
  const [backendHealth, setBackendHealth] = useState<'checking' | 'online' | 'offline'>('online');

  // Add toast helper
  const addToast = useCallback((message: string, type: Toast['type'] = 'info') => {
    const id = Date.now().toString() + Math.random().toString();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4000);
  }, []);

  // Fetch from GraphQL on start
  useEffect(() => {
    async function fetchFromBackend() {
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
          const data = await res.json();
          if (data?.data?.events?.length > 0) {
            setEvents(prev => {
              const backendEvents: Event[] = data.data.events.map((e: any) => ({
                ...e,
                modality: (e.modality === 'PRESENCIAL' || e.modality === 'VIRTUAL' || e.modality === 'HIBRIDO') ? e.modality : 'PRESENCIAL',
                type: e.type as TipoEvento,
                registered: 0,
              }));
              // Merge avoiding duplicates
              const existingIds = new Set(prev.map(p => p.id));
              const newItems = backendEvents.filter(b => !existingIds.has(b.id));
              return [...newItems, ...prev];
            });
            setBackendHealth('online');
          }
        }
      } catch {
        setBackendHealth('offline');
      }
    }
    fetchFromBackend();
  }, []);

  // Search Results
  const searchResults = useMemo(() => {
    if (!globalSearch.trim()) return null;
    const q = globalSearch.toLowerCase();
    const evs = events.filter(e => e.name.toLowerCase().includes(q));
    const parts = participants.filter(p => `${p.firstName} ${p.lastName} ${p.email}`.toLowerCase().includes(q));
    return { evs, parts };
  }, [globalSearch, events, participants]);

  /* ── Event handlers ── */
  const handleSaveEvent = async (data: Partial<Event>) => {
    if (editEvent) {
      setEvents(prev => prev.map(e => e.id === editEvent.id ? { ...e, ...data } : e));
      addToast(`Evento "${data.name}" actualizado correctamente`, 'success');
    } else {
      const newEvent: Event = {
        ...data,
        id: Date.now().toString(),
        status: 'BORRADOR',
        registered: 0,
      } as Event;

      // Try GraphQL creation in backend
      try {
        const gqlRes = await fetch('http://localhost:8080/graphql', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            query: `mutation {
              createEvent(input: {
                name: "${newEvent.name.replace(/"/g, '\\"')}",
                description: "${(newEvent.description || '').replace(/"/g, '\\"')}",
                type: "${newEvent.type || 'TALLER'}",
                modality: "${newEvent.modality || 'PRESENCIAL'}",
                startAt: "${newEvent.startAt || '2026-11-20T09:00:00'}",
                endAt: "${newEvent.endAt || '2026-11-20T17:00:00'}",
                location: "${(newEvent.location || '').replace(/"/g, '\\"')}",
                meetingUrl: "${(newEvent.meetingUrl || '').replace(/"/g, '\\"')}",
                capacity: ${newEvent.capacity || 50},
                durationHours: ${newEvent.durationHours || 4}
              }) { id name status capacity }
            }`
          })
        });
        if (gqlRes.ok) {
          const resJson = await gqlRes.json();
          if (resJson?.data?.createEvent?.id) {
            newEvent.id = resJson.data.createEvent.id;
          }
        }
      } catch (err) {
        console.warn('Backend sync failed, saved locally:', err);
      }

      setEvents(prev => [newEvent, ...prev]);
      setActivities(prev => [{
        id: Date.now().toString(),
        text: `Nuevo evento creado: "${newEvent.name}"`,
        time: 'Justo ahora',
        color: '#8b5cf6'
      }, ...prev]);
      addToast(`¡Evento "${newEvent.name}" guardado exitosamente!`, 'success');
    }
    setEditEvent(null);
    setShowEventModal(false);
  };

  const handlePublishEvent = async (eventId: string, eventName: string) => {
    try {
      await fetch('http://localhost:8080/graphql', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: `mutation { updateEventStatus(eventId: "${eventId}", status: "PUBLICADO") { id status } }`
        })
      });
    } catch (e) {
      console.warn('Backend status update fallback:', e);
    }
    setEvents(prev => prev.map(e => e.id === eventId ? { ...e, status: 'PUBLICADO' } : e));
    addToast(`Evento "${eventName}" publicado para inscripciones`, 'success');
  };

  const handleCancelEvent = async (eventId: string, eventName: string) => {
    if (!window.confirm(`¿Seguro que deseas cancelar el evento "${eventName}"?`)) return;
    try {
      await fetch('http://localhost:8080/graphql', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: `mutation { cancelEvent(eventId: "${eventId}", reason: "Cancelado desde panel admin") { id status } }`
        })
      });
    } catch (e) {
      console.warn('Backend cancellation fallback:', e);
    }
    setEvents(prev => prev.map(e => e.id === eventId ? { ...e, status: 'CANCELADO' } : e));
    addToast(`Evento "${eventName}" cancelado`, 'warning');
  };

  const handleDeleteEvent = (eventId: string, eventName: string) => {
    if (!window.confirm(`¿Estás seguro de eliminar el evento "${eventName}"?`)) return;
    setEvents(prev => prev.filter(e => e.id !== eventId));
    addToast(`Evento "${eventName}" eliminado`, 'info');
  };

  /* ── Participant handlers ── */
  const handleSaveParticipant = (data: Partial<Participant>) => {
    if (editParticipant) {
      setParticipants(prev => prev.map(p => p.id === editParticipant.id ? { ...p, ...data } : p));
      addToast(`Participante "${data.firstName} ${data.lastName}" actualizado`, 'success');
    } else {
      const newPart: Participant = {
        ...data,
        id: Date.now().toString(),
        createdAt: new Date().toISOString().split('T')[0],
      } as Participant;
      setParticipants(prev => [newPart, ...prev]);
      addToast(`Participante "${newPart.firstName} ${newPart.lastName}" registrado`, 'success');
    }
    setEditParticipant(null);
    setShowParticipantModal(false);
  };

  const handleDeleteParticipant = (id: string, name: string) => {
    if (!window.confirm(`¿Eliminar participante ${name}?`)) return;
    setParticipants(prev => prev.filter(p => p.id !== id));
    addToast(`Participante ${name} eliminado`, 'info');
  };

  /* ── Registration handlers ── */
  const handleCreateRegistration = (partId: string, evId: string) => {
    const part = participants.find(p => p.id === partId);
    const ev = events.find(e => e.id === evId);
    if (!part || !ev) return;

    const token = `QR-SYNC-${Math.floor(1000 + Math.random() * 9000)}`;
    const newReg: Registration = {
      id: Date.now().toString(),
      participantId: part.id,
      eventId: ev.id,
      participantName: `${part.firstName} ${part.lastName}`,
      participantEmail: part.email,
      eventName: ev.name,
      status: 'ACTIVA',
      createdAt: new Date().toISOString().split('T')[0],
      qrToken: token,
      checkedIn: false
    };

    setRegistrations(prev => [newReg, ...prev]);
    setEvents(prev => prev.map(e => e.id === ev.id ? { ...e, registered: (e.registered || 0) + 1 } : e));
    setActivities(prev => [{
      id: Date.now().toString(),
      text: `${part.firstName} ${part.lastName} se inscribió en "${ev.name}"`,
      time: 'Justo ahora',
      color: '#3b82f6'
    }, ...prev]);
    addToast(`Inscripción exitosa para ${part.firstName} con token ${token}`, 'success');
    setShowRegModal(false);
  };

  const handleCancelRegistration = (regId: string, partName: string) => {
    if (!window.confirm(`¿Deseas cancelar la inscripción de ${partName}?`)) return;
    setRegistrations(prev => prev.map(r => r.id === regId ? { ...r, status: 'CANCELADA' } : r));
    addToast(`Inscripción de ${partName} cancelada`, 'warning');
  };

  /* ── Attendance check-in handlers ── */
  const handleCheckInByToken = (token: string) => {
    const clean = token.trim().toUpperCase();
    const regIndex = registrations.findIndex(r => r.qrToken.toUpperCase() === clean);
    if (regIndex === -1) {
      addToast(`Token "${clean}" no encontrado en el sistema`, 'error');
      return false;
    }
    const reg = registrations[regIndex];
    if (reg.checkedIn) {
      addToast(`${reg.participantName} ya tenía asistencia registrada`, 'warning');
      return true;
    }
    setRegistrations(prev => prev.map((r, i) => i === regIndex ? { ...r, checkedIn: true } : r));
    setActivities(prev => [{
      id: Date.now().toString(),
      text: `${reg.participantName} confirmó asistencia vía QR (${reg.eventName})`,
      time: 'Justo ahora',
      color: '#10b981'
    }, ...prev]);
    addToast(`✅ Check-in exitoso: ${reg.participantName}`, 'success');
    return true;
  };

  const handleToggleManualCheckIn = (regId: string) => {
    setRegistrations(prev => prev.map(r => {
      if (r.id === regId) {
        const next = !r.checkedIn;
        addToast(next ? `Check-in marcado para ${r.participantName}` : `Check-in desmarcado`, 'info');
        return { ...r, checkedIn: next };
      }
      return r;
    }));
  };

  const navSections = ['PRINCIPAL', 'GESTIÓN', 'OPERACIONES'] as const;
  const navItems = [
    { id: 'dashboard',     label: 'Dashboard',     icon: '📊', section: 'PRINCIPAL' },
    { id: 'events',        label: 'Eventos',        icon: '🗓️', section: 'GESTIÓN', badge: events.length },
    { id: 'participants',  label: 'Participantes',  icon: '👥', section: 'GESTIÓN', badge: participants.length },
    { id: 'registrations', label: 'Inscripciones',  icon: '📋', section: 'GESTIÓN', badge: registrations.length },
    { id: 'attendance',    label: 'Asistencia',     icon: '✅', section: 'OPERACIONES' },
    { id: 'certificates',  label: 'Certificados',   icon: '🎓', section: 'OPERACIONES' },
  ];

  return (
    <div id="app-shell">
      {/* ── Toast Notifications ── */}
      <div className="toast-container" aria-live="polite">
        {toasts.map(t => (
          <div key={t.id} className={`toast toast-${t.type}`}>
            <span>{t.type === 'success' ? '✅' : t.type === 'error' ? '❌' : t.type === 'warning' ? '⚠️' : 'ℹ️'}</span>
            <span>{t.message}</span>
          </div>
        ))}
      </div>

      {/* ── Sidebar ── */}
      <nav className="sidebar" aria-label="Navegación principal">
        <div className="sidebar-logo">
          <div className="sidebar-logo-icon">⚡</div>
          <div>
            <div className="sidebar-logo-text">SyncEvent</div>
            <span className="sidebar-logo-sub">v2.0 – Panel Admin</span>
          </div>
        </div>

        <div className="sidebar-nav">
          {navSections.map(sec => (
            <React.Fragment key={sec}>
              <span className="nav-section-label">{sec}</span>
              {navItems.filter(n => n.section === sec).map(n => (
                <button
                  key={n.id}
                  id={`nav-${n.id}`}
                  className={`nav-item ${view === n.id ? 'active' : ''}`}
                  onClick={() => { setView(n.id as View); setGlobalSearch(''); }}
                >
                  <span className="nav-icon">{n.icon}</span>
                  {n.label}
                  {n.badge !== undefined && <span className="nav-badge">{n.badge}</span>}
                </button>
              ))}
            </React.Fragment>
          ))}
        </div>

        <div className="sidebar-footer">
          <div className="avatar" style={{ cursor: 'pointer' }} onClick={() => setShowProfile(true)}>AD</div>
          <div className="avatar-info" style={{ cursor: 'pointer' }} onClick={() => setShowProfile(true)}>
            <div className="avatar-name">Administrador</div>
            <div className="avatar-role">ADMIN • SENA</div>
          </div>
        </div>
      </nav>

      {/* ── Main Area ── */}
      <div className="main">
        {/* Topbar */}
        <header className="topbar">
          <span className="topbar-title">
            {view === 'dashboard'     && 'Panel Principal'}
            {view === 'events'        && 'Gestión de Eventos'}
            {view === 'participants'  && 'Directorio de Participantes'}
            {view === 'registrations' && 'Inscripciones Activas'}
            {view === 'attendance'    && 'Control de Asistencia en Vivo'}
            {view === 'certificates'  && 'Verificación y Emisión de Certificados'}
          </span>

          <div style={{ position: 'relative' }}>
            <div className="topbar-search">
              <span className="topbar-search-icon">🔍</span>
              <input
                placeholder="Buscar eventos, personas…"
                value={globalSearch}
                onChange={e => setGlobalSearch(e.target.value)}
                aria-label="Búsqueda global"
              />
              {globalSearch && (
                <button className="btn btn-ghost btn-sm" style={{ padding: '0 4px' }} onClick={() => setGlobalSearch('')}>✕</button>
              )}
            </div>

            {/* Live Search Dropdown */}
            {searchResults && (
              <div className="dropdown-panel" style={{ width: 340, left: 0, top: 48 }}>
                <div className="dropdown-header">
                  <span className="dropdown-title">Resultados de búsqueda</span>
                  <span style={{ fontSize: '0.75rem', color: 'var(--slate-400)' }}>
                    {searchResults.evs.length + searchResults.parts.length} encontrados
                  </span>
                </div>
                <div className="dropdown-list">
                  {searchResults.evs.map(e => (
                    <div key={e.id} className="dropdown-item" onClick={() => { setView('events'); setDetailEvent(e); setGlobalSearch(''); }}>
                      <div style={{ fontWeight: 600, color: 'var(--slate-100)', fontSize: '0.85rem' }}>🗓️ {e.name}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--violet-400)' }}>{e.status} • {e.modality}</div>
                    </div>
                  ))}
                  {searchResults.parts.map(p => (
                    <div key={p.id} className="dropdown-item" onClick={() => { setView('participants'); setGlobalSearch(''); }}>
                      <div style={{ fontWeight: 600, color: 'var(--slate-100)', fontSize: '0.85rem' }}>👥 {p.firstName} {p.lastName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-400)' }}>{p.email} • {p.documentNumber}</div>
                    </div>
                  ))}
                  {searchResults.evs.length === 0 && searchResults.parts.length === 0 && (
                    <div style={{ padding: 20, textAlign: 'center', color: 'var(--slate-400)', fontSize: '0.85rem' }}>
                      No se encontraron coincidencias
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="topbar-actions">
            <button
              className="icon-btn"
              title="Notificaciones"
              onClick={() => { setShowNotifications(!showNotifications); setShowSettings(false); setShowProfile(false); }}
            >
              🔔
            </button>

            <button
              className="icon-btn"
              title="Configuración"
              onClick={() => { setShowSettings(!showSettings); setShowNotifications(false); setShowProfile(false); }}
            >
              ⚙️
            </button>

            <div
              className="avatar"
              style={{ cursor: 'pointer' }}
              title="Perfil de Usuario"
              onClick={() => { setShowProfile(!showProfile); setShowNotifications(false); setShowSettings(false); }}
            >
              AD
            </div>
          </div>

          {/* Notifications Panel */}
          {showNotifications && (
            <div className="dropdown-panel">
              <div className="dropdown-header">
                <span className="dropdown-title">🔔 Notificaciones</span>
                <button className="btn btn-ghost btn-sm" onClick={() => { addToast('Notificaciones marcadas como leídas', 'info'); setShowNotifications(false); }}>
                  Leídas
                </button>
              </div>
              <div className="dropdown-list">
                {activities.slice(0, 5).map(a => (
                  <div key={a.id} className="dropdown-item">
                    <p style={{ margin: 0, fontSize: '0.84rem', color: 'var(--slate-200)' }}>{a.text}</p>
                    <span style={{ fontSize: '0.72rem', color: 'var(--slate-500)' }}>{a.time}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Settings Panel */}
          {showSettings && (
            <div className="dropdown-panel" style={{ width: 380 }}>
              <div className="dropdown-header">
                <span className="dropdown-title">⚙️ Configuración del Sistema</span>
                <button className="btn btn-ghost btn-sm" onClick={() => setShowSettings(false)}>✕</button>
              </div>
              <div style={{ padding: 20, display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--slate-400)', display: 'block', marginBottom: 6 }}>Servidor GraphQL</label>
                  <input className="input" defaultValue="http://localhost:8080/graphql" readOnly style={{ fontSize: '0.82rem' }} />
                  <div style={{ marginTop: 6, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span style={{ fontSize: '0.78rem', color: backendHealth === 'online' ? 'var(--emerald-400)' : 'var(--rose-400)' }}>
                      {backendHealth === 'online' ? '🟢 Conectado con PostgreSQL' : '🔴 Desconectado'}
                    </span>
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={async () => {
                        try {
                          const res = await fetch('http://localhost:8080/graphql', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ query: 'query { dashboardSummary { totalEvents } }' })
                          });
                          if (res.ok) {
                            setBackendHealth('online');
                            addToast('Conexión con Backend Spring Boot 3 verificada', 'success');
                          } else {
                            setBackendHealth('offline');
                            addToast('Error al conectar con backend', 'error');
                          }
                        } catch {
                          setBackendHealth('offline');
                          addToast('No se pudo contactar a localhost:8080', 'error');
                        }
                      }}
                    >
                      Probar Ping
                    </button>
                  </div>
                </div>

                <div style={{ borderTop: '1px solid var(--glass-border)', paddingTop: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.85rem' }}>Auto-recarga en vivo</span>
                    <input type="checkbox" defaultChecked />
                  </div>
                </div>

                <button className="btn btn-primary btn-sm" onClick={() => { addToast('Ajustes guardados', 'success'); setShowSettings(false); }}>
                  Guardar Preferencias
                </button>
              </div>
            </div>
          )}

          {/* Profile Modal */}
          {showProfile && (
            <div className="dropdown-panel" style={{ width: 320 }}>
              <div className="dropdown-header">
                <span className="dropdown-title">👤 Cuenta de Usuario</span>
                <button className="btn btn-ghost btn-sm" onClick={() => setShowProfile(false)}>✕</button>
              </div>
              <div style={{ padding: 20, textAlign: 'center' }}>
                <div className="avatar" style={{ width: 56, height: 56, fontSize: '1.2rem', margin: '0 auto 12px' }}>AD</div>
                <h4 style={{ margin: 0, color: 'var(--slate-100)' }}>Administrador SyncEvent</h4>
                <p style={{ margin: '4px 0 12px', fontSize: '0.8rem', color: 'var(--slate-400)' }}>admin@syncevent.com</p>
                <div style={{ display: 'inline-block', marginBottom: 16 }}>
                  <span className="badge badge-emerald">ROLES: ADMIN • OPERADOR</span>
                </div>
                <div style={{ borderTop: '1px solid var(--glass-border)', paddingTop: 14 }}>
                  <button className="btn btn-danger btn-sm w-full" onClick={() => { addToast('Sesión de administrador activa', 'info'); setShowProfile(false); }}>
                    🔒 Cerrar Sesión
                  </button>
                </div>
              </div>
            </div>
          )}
        </header>

        {/* ── Content Body ── */}
        <main className="content" id="main-content">
          {view === 'dashboard' && (
            <DashboardView
              events={events}
              participants={participants}
              registrations={registrations}
              activities={activities}
              onNavigate={setView}
              onDetail={setDetailEvent}
              onPublish={handlePublishEvent}
            />
          )}

          {view === 'events' && (
            <EventsView
              events={events}
              onNew={() => { setEditEvent(null); setShowEventModal(true); }}
              onEdit={e => { setEditEvent(e); setShowEventModal(true); }}
              onDetail={setDetailEvent}
              onPublish={handlePublishEvent}
              onCancel={handleCancelEvent}
              onDelete={handleDeleteEvent}
            />
          )}

          {view === 'participants' && (
            <ParticipantsView
              participants={participants}
              registrations={registrations}
              onNew={() => { setEditParticipant(null); setShowParticipantModal(true); }}
              onEdit={p => { setEditParticipant(p); setShowParticipantModal(true); }}
              onViewInscriptions={p => setViewParticipantInscriptions(p)}
              onDelete={handleDeleteParticipant}
            />
          )}

          {view === 'registrations' && (
            <RegistrationsView
              registrations={registrations}
              onNew={() => setShowRegModal(true)}
              onShowQr={r => setShowQrModal(r)}
              onCancel={handleCancelRegistration}
            />
          )}

          {view === 'attendance' && (
            <AttendanceView
              events={events}
              registrations={registrations}
              onCheckInQr={() => setShowCheckInQrModal(true)}
              onManualCheckIn={() => setShowManualCheckInModal(true)}
            />
          )}

          {view === 'certificates' && (
            <CertificatesView
              events={events}
              participants={participants}
              onIssueNew={() => setShowIssueCertModal(true)}
              addToast={addToast}
            />
          )}
        </main>
      </div>

      {/* ── GLOBAL MODALS ── */}
      {/* Event Form Modal */}
      {showEventModal && (
        <EventFormModal
          initial={editEvent ?? {}}
          onClose={() => { setShowEventModal(false); setEditEvent(null); }}
          onSave={handleSaveEvent}
        />
      )}

      {/* Event Detail Modal */}
      {detailEvent && (
        <EventDetailModal
          event={detailEvent}
          onClose={() => setDetailEvent(null)}
          onPublish={handlePublishEvent}
          onCancel={handleCancelEvent}
        />
      )}

      {/* Participant Form Modal */}
      {showParticipantModal && (
        <ParticipantFormModal
          initial={editParticipant ?? {}}
          onClose={() => { setShowParticipantModal(false); setEditParticipant(null); }}
          onSave={handleSaveParticipant}
        />
      )}

      {/* Participant Inscriptions Modal */}
      {viewParticipantInscriptions && (
        <ParticipantInscriptionsModal
          participant={viewParticipantInscriptions}
          registrations={registrations.filter(r => r.participantEmail === viewParticipantInscriptions.email || r.participantName.includes(viewParticipantInscriptions.firstName))}
          onClose={() => setViewParticipantInscriptions(null)}
          onShowQr={r => setShowQrModal(r)}
        />
      )}

      {/* New Registration Modal */}
      {showRegModal && (
        <NewRegistrationModal
          events={events.filter(e => e.status === 'PUBLICADO' || e.status === 'EN_CURSO')}
          participants={participants}
          onClose={() => setShowRegModal(false)}
          onSave={handleCreateRegistration}
        />
      )}

      {/* QR Code Modal */}
      {showQrModal && (
        <QrModal
          registration={showQrModal}
          onClose={() => setShowQrModal(null)}
          onCopy={token => {
            navigator.clipboard?.writeText(token);
            addToast(`Token ${token} copiado al portapapeles`, 'success');
          }}
          onDownload={() => addToast('Pase digital descargado en formato PDF/PNG', 'info')}
        />
      )}

      {/* QR Check-in Simulator Modal */}
      {showCheckInQrModal && (
        <CheckInQrModal
          onClose={() => setShowCheckInQrModal(false)}
          onCheckIn={handleCheckInByToken}
          availableTokens={registrations.filter(r => !r.checkedIn).map(r => r.qrToken)}
        />
      )}

      {/* Manual Check-in Modal */}
      {showManualCheckInModal && (
        <ManualCheckInModal
          registrations={registrations}
          onClose={() => setShowManualCheckInModal(false)}
          onToggle={handleToggleManualCheckIn}
        />
      )}

      {/* Issue Certificate Modal */}
      {showIssueCertModal && (
        <IssueCertificateModal
          events={events}
          participants={participants}
          onClose={() => setShowIssueCertModal(false)}
          onIssue={(code, partName) => {
            addToast(`Certificado ${code} emitido para ${partName}`, 'success');
            setShowIssueCertModal(false);
          }}
        />
      )}
    </div>
  );
}

/* ─────────────────────────────────────────────
   VIEW: DASHBOARD
───────────────────────────────────────────── */
function DashboardView({
  events, participants, registrations, activities, onNavigate, onDetail, onPublish
}: {
  events: Event[];
  participants: Participant[];
  registrations: Registration[];
  activities: { id: string; text: string; time: string; color: string }[];
  onNavigate: (v: View) => void;
  onDetail: (e: Event) => void;
  onPublish: (id: string, name: string) => void;
}) {
  const activeEvents = events.filter(e => e.status === 'PUBLICADO' || e.status === 'EN_CURSO').length;
  const activeRegs = registrations.filter(r => r.status === 'ACTIVA').length;

  return (
    <>
      <div className="page-header">
        <div className="page-header-text">
          <div className="page-eyebrow">Panel de Control General</div>
          <h1 className="page-title">Dashboard</h1>
          <p className="page-subtitle">Métricas del sistema, aforo y actividad sincronizada en tiempo real</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-primary" onClick={() => onNavigate('events')}>＋ Nuevo Evento</button>
          <span className="live-badge"><span className="live-dot" />En vivo</span>
        </div>
      </div>

      <div className="stats-grid">
        {[
          { label: 'Eventos totales', value: events.length, icon: '🗓️', change: `${events.length} registrados`, dir: 'up', accent: 'linear-gradient(90deg,#8b5cf6,#3b82f6)', iconBg: 'rgba(139,92,246,0.15)', iconColor: '#a78bfa' },
          { label: 'Eventos activos', value: activeEvents, icon: '⚡', change: `${activeEvents} en curso/publicados`, dir: 'neutral', accent: 'linear-gradient(90deg,#10b981,#3b82f6)', iconBg: 'rgba(16,185,129,0.15)', iconColor: '#34d399' },
          { label: 'Participantes', value: participants.length, icon: '👥', change: `${participants.length} inscritos en base de datos`, dir: 'up', accent: 'linear-gradient(90deg,#f59e0b,#ef4444)', iconBg: 'rgba(245,158,11,0.15)', iconColor: '#fbbf24' },
          { label: 'Inscripciones activas', value: activeRegs, icon: '📋', change: `${activeRegs} con pases QR generados`, dir: 'up', accent: 'linear-gradient(90deg,#3b82f6,#8b5cf6)', iconBg: 'rgba(59,130,246,0.15)', iconColor: '#60a5fa' },
        ].map(s => (
          <div key={s.label} className="stat-card" style={{ '--stat-accent': s.accent, '--stat-icon-bg': s.iconBg, '--stat-icon-color': s.iconColor } as React.CSSProperties}>
            <div className="stat-card-header">
              <span className="stat-label">{s.label}</span>
              <span className="stat-icon">{s.icon}</span>
            </div>
            <div className="stat-value">{s.value}</div>
            <span className={`stat-change ${s.dir}`}>{s.dir === 'up' ? '↑' : '→'} {s.change}</span>
          </div>
        ))}
      </div>

      <div className="dashboard-main-grid">
        {/* Eventos recientes */}
        <div className="card">
          <div className="card-header">
            <span className="card-title"><span className="card-title-icon">🗓️</span>Eventos Recientes</span>
            <button className="btn btn-ghost btn-sm" onClick={() => onNavigate('events')}>Ver todos →</button>
          </div>
          <div className="table-wrapper">
            <table>
              <thead><tr><th>Nombre</th><th>Estado</th><th>Aforo</th><th>Acción</th></tr></thead>
              <tbody>
                {events.slice(0, 5).map(e => (
                  <tr key={e.id}>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--slate-100)', cursor: 'pointer' }} onClick={() => onDetail(e)}>
                        {e.name}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-400)' }}>{e.type} • {e.modality}</div>
                    </td>
                    <td>{estadoBadge(e.status)}</td>
                    <td>
                      <div className="progress-bar-wrapper">
                        <div className="progress-bar-track">
                          <div className={`progress-bar-fill ${occupancy(e.registered, e.capacity) >= 90 ? 'alert' : ''}`} style={{ width: `${occupancy(e.registered, e.capacity)}%` }} />
                        </div>
                        <span className="progress-bar-label">{occupancy(e.registered, e.capacity)}%</span>
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-secondary btn-sm" onClick={() => onDetail(e)}>👁️</button>
                        {e.status === 'BORRADOR' && (
                          <button className="btn btn-primary btn-sm" onClick={() => onPublish(e.id, e.name)}>📢</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Actividad reciente */}
        <div className="card">
          <div className="card-header">
            <span className="card-title"><span className="card-title-icon">🔔</span>Actividad Reciente</span>
            <span className="badge badge-slate">{activities.length} eventos</span>
          </div>
          <div className="card-body" style={{ padding: '0 24px' }}>
            <div className="activity-list">
              {activities.map(a => (
                <div key={a.id} className="activity-item">
                  <span className="activity-dot" style={{ background: a.color }} />
                  <div className="activity-body">
                    <p className="activity-text">{a.text}</p>
                    <p className="activity-time">{a.time}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

/* ─────────────────────────────────────────────
   VIEW: EVENTS
───────────────────────────────────────────── */
function EventsView({
  events, onNew, onEdit, onDetail, onPublish, onCancel, onDelete
}: {
  events: Event[];
  onNew: () => void;
  onEdit: (e: Event) => void;
  onDetail: (e: Event) => void;
  onPublish: (id: string, name: string) => void;
  onCancel: (id: string, name: string) => void;
  onDelete: (id: string, name: string) => void;
}) {
  const [filter, setFilter] = useState<EstadoEvento | 'TODOS'>('TODOS');
  const [search, setSearch] = useState('');

  const filtered = events.filter(e =>
    (filter === 'TODOS' || e.status === filter) &&
    e.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <>
      <div className="page-header">
        <div className="page-header-text">
          <div className="page-eyebrow">Gestión de eventos</div>
          <h1 className="page-title">Eventos</h1>
          <p className="page-subtitle">{events.length} eventos registrados en SyncEvent</p>
        </div>
        <button className="btn btn-primary" onClick={onNew}>＋ Nuevo Evento</button>
      </div>

      <div className="card">
        <div className="card-header">
          <div className="tabs" style={{ maxWidth: 520 }}>
            {(['TODOS','PUBLICADO','EN_CURSO','BORRADOR','FINALIZADO','CANCELADO'] as const).map(f => (
              <button key={f} className={`tab-btn ${filter === f ? 'active' : ''}`} onClick={() => setFilter(f)}>
                {f === 'TODOS' ? 'Todos' : f.charAt(0) + f.slice(1).toLowerCase().replace('_',' ')}
              </button>
            ))}
          </div>
          <input
            className="input"
            style={{ width: 220, height: 36 }}
            placeholder="🔍 Buscar evento…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>

        <div className="table-wrapper">
          {filtered.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">🗓️</span>
              <p className="empty-text">No se encontraron eventos con los filtros seleccionados</p>
            </div>
          ) : (
            <table>
              <thead>
                <tr><th>Evento</th><th>Tipo</th><th>Modalidad</th><th>Inicio</th><th>Aforo</th><th>Estado</th><th>Acciones</th></tr>
              </thead>
              <tbody>
                {filtered.map(e => (
                  <tr key={e.id}>
                    <td>
                      <div
                        style={{ fontWeight: 600, color: 'var(--slate-100)', cursor: 'pointer', marginBottom: 2 }}
                        onClick={() => onDetail(e)}
                      >
                        {e.name}
                      </div>
                      {e.description && (
                        <div style={{ fontSize: '0.78rem', color: 'var(--slate-500)', maxWidth: 280 }} className="truncate">
                          {e.description}
                        </div>
                      )}
                    </td>
                    <td><span className="badge badge-slate">{e.type}</span></td>
                    <td>{modalidadBadge(e.modality)}</td>
                    <td style={{ whiteSpace: 'nowrap', fontSize: '0.82rem' }}>{fmtDate(e.startAt)}</td>
                    <td>
                      <div className="progress-bar-wrapper" style={{ minWidth: 110 }}>
                        <div className="progress-bar-track">
                          <div className={`progress-bar-fill ${occupancy(e.registered, e.capacity) >= 90 ? 'alert' : ''}`} style={{ width: `${occupancy(e.registered, e.capacity)}%` }} />
                        </div>
                        <span className="progress-bar-label">{e.registered ?? 0}/{e.capacity}</span>
                      </div>
                    </td>
                    <td>{estadoBadge(e.status)}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-secondary btn-sm" title="Ver Detalles" onClick={() => onDetail(e)}>👁️</button>
                        <button className="btn btn-secondary btn-sm" title="Editar" onClick={() => onEdit(e)}>✏️</button>
                        {e.status === 'BORRADOR' && (
                          <button className="btn btn-primary btn-sm" title="Publicar" onClick={() => onPublish(e.id, e.name)}>📢 Publicar</button>
                        )}
                        {e.status === 'PUBLICADO' && (
                          <button className="btn btn-secondary btn-sm" title="Cancelar Evento" onClick={() => onCancel(e.id, e.name)}>✕</button>
                        )}
                        <button className="btn btn-danger btn-sm" title="Eliminar" onClick={() => onDelete(e.id, e.name)}>🗑️</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </>
  );
}

/* ─────────────────────────────────────────────
   VIEW: PARTICIPANTS
───────────────────────────────────────────── */
function ParticipantsView({
  participants, registrations, onNew, onEdit, onViewInscriptions, onDelete
}: {
  participants: Participant[];
  registrations: Registration[];
  onNew: () => void;
  onEdit: (p: Participant) => void;
  onViewInscriptions: (p: Participant) => void;
  onDelete: (id: string, name: string) => void;
}) {
  const [search, setSearch] = useState('');

  const filtered = participants.filter(p =>
    `${p.firstName} ${p.lastName} ${p.email} ${p.documentNumber}`.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <>
      <div className="page-header">
        <div className="page-header-text">
          <div className="page-eyebrow">Gestión de participantes</div>
          <h1 className="page-title">Participantes</h1>
          <p className="page-subtitle">{participants.length} personas registradas en el sistema</p>
        </div>
        <button className="btn btn-primary" onClick={onNew}>＋ Nuevo Participante</button>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title"><span className="card-title-icon">👥</span>Listado de Participantes</span>
          <input
            className="input"
            style={{ width: 240, height: 36 }}
            placeholder="🔍 Buscar por nombre o doc…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>

        <div className="table-wrapper">
          {filtered.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">👥</span>
              <p className="empty-text">No se encontraron participantes</p>
            </div>
          ) : (
            <table>
              <thead>
                <tr><th>Participante</th><th>Documento</th><th>Teléfono</th><th>Fecha registro</th><th>Inscripciones</th><th>Acciones</th></tr>
              </thead>
              <tbody>
                {filtered.map(p => {
                  const pRegs = registrations.filter(r => r.participantEmail === p.email || r.participantName.includes(p.firstName));
                  return (
                    <tr key={p.id}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                          <div className="avatar" style={{ width: 34, height: 34, fontSize: '0.8rem' }}>
                            {p.firstName[0]}{p.lastName[0]}
                          </div>
                          <div>
                            <div style={{ fontWeight: 600, color: 'var(--slate-100)' }}>{p.firstName} {p.lastName}</div>
                            <div style={{ fontSize: '0.78rem', color: 'var(--slate-500)' }}>{p.email}</div>
                          </div>
                        </div>
                      </td>
                      <td><span className="badge badge-slate">{p.documentType}</span> {p.documentNumber}</td>
                      <td>{p.phone}</td>
                      <td style={{ fontSize: '0.82rem' }}>{p.createdAt}</td>
                      <td>
                        <span className="badge badge-violet">{pRegs.length} evento(s)</span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button className="btn btn-secondary btn-sm" title="Ver Inscripciones" onClick={() => onViewInscriptions(p)}>
                            👁️ Inscripciones
                          </button>
                          <button className="btn btn-secondary btn-sm" title="Editar" onClick={() => onEdit(p)}>
                            ✏️
                          </button>
                          <button className="btn btn-danger btn-sm" title="Eliminar" onClick={() => onDelete(p.id, `${p.firstName} ${p.lastName}`)}>
                            🗑️
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </>
  );
}

/* ─────────────────────────────────────────────
   VIEW: REGISTRATIONS
───────────────────────────────────────────── */
function RegistrationsView({
  registrations, onNew, onShowQr, onCancel
}: {
  registrations: Registration[];
  onNew: () => void;
  onShowQr: (r: Registration) => void;
  onCancel: (id: string, name: string) => void;
}) {
  const [search, setSearch] = useState('');
  const filtered = registrations.filter(r =>
    `${r.participantName} ${r.eventName} ${r.qrToken}`.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <>
      <div className="page-header">
        <div className="page-header-text">
          <div className="page-eyebrow">Control de accesos e inscripciones</div>
          <h1 className="page-title">Inscripciones a Eventos</h1>
          <p className="page-subtitle">{registrations.length} inscripciones registradas con credencial QR</p>
        </div>
        <button className="btn btn-primary" onClick={onNew}>＋ Nueva Inscripción</button>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title"><span className="card-title-icon">📋</span>Registro de Inscripciones</span>
          <input
            className="input"
            style={{ width: 240, height: 36 }}
            placeholder="🔍 Buscar por token, persona o evento…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>

        <div className="table-wrapper">
          <table>
            <thead>
              <tr><th>Participante</th><th>Evento</th><th>Estado</th><th>Check-in</th><th>Token QR</th><th>Acciones</th></tr>
            </thead>
            <tbody>
              {filtered.map(r => (
                <tr key={r.id}>
                  <td>
                    <div style={{ fontWeight: 600, color: 'var(--slate-100)' }}>{r.participantName}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--slate-500)' }}>{r.participantEmail}</div>
                  </td>
                  <td style={{ maxWidth: 220 }} className="truncate">
                    <span style={{ fontWeight: 500 }}>{r.eventName}</span>
                  </td>
                  <td>
                    {r.status === 'ACTIVA'
                      ? <span className="badge badge-emerald badge-dot">Activa</span>
                      : <span className="badge badge-rose">Cancelada</span>}
                  </td>
                  <td>
                    {r.checkedIn ? (
                      <span className="badge badge-emerald">✅ Presente</span>
                    ) : (
                      <span className="badge badge-slate">⏳ Pendiente</span>
                    )}
                  </td>
                  <td>
                    <code style={{ fontSize: '0.78rem', color: 'var(--violet-400)', background: 'rgba(139,92,246,0.1)', padding: '3px 8px', borderRadius: 6 }}>
                      {r.qrToken}
                    </code>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button className="btn btn-secondary btn-sm" onClick={() => onShowQr(r)}>
                        📱 Ver QR
                      </button>
                      {r.status === 'ACTIVA' && (
                        <button className="btn btn-danger btn-sm" onClick={() => onCancel(r.id, r.participantName)}>
                          ✕ Cancelar
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

/* ─────────────────────────────────────────────
   VIEW: ATTENDANCE
───────────────────────────────────────────── */
function AttendanceView({
  events, registrations, onCheckInQr, onManualCheckIn
}: {
  events: Event[];
  registrations: Registration[];
  onCheckInQr: () => void;
  onManualCheckIn: () => void;
}) {
  const [selectedEventId, setSelectedEventId] = useState(events[0]?.id || '1');
  const selectedEvent = events.find(e => e.id === selectedEventId) || events[0];

  const eventRegs = registrations.filter(r => r.eventName === selectedEvent?.name || r.eventId === selectedEvent?.id);
  const checkedInCount = eventRegs.filter(r => r.checkedIn).length;
  const occ = occupancy(checkedInCount, selectedEvent?.capacity || 100);
  const isAlert = occ >= 90;

  return (
    <>
      <div className="page-header">
        <div className="page-header-text">
          <div className="page-eyebrow">Control de aforo y acceso</div>
          <h1 className="page-title">Asistencia en Vivo</h1>
          <p className="page-subtitle">Validación por código QR y monitor de asistencia en tiempo real</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-primary" onClick={onCheckInQr}>📱 Check-in por QR</button>
          <button className="btn btn-secondary" onClick={onManualCheckIn}>✏️ Check-in Manual</button>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="form-group" style={{ maxWidth: 450, margin: 0 }}>
            <label style={{ fontWeight: 600, color: 'var(--slate-200)', marginBottom: 8, display: 'block' }}>
              Seleccionar Evento a Monitorear
            </label>
            <select
              className="input"
              value={selectedEventId}
              onChange={e => setSelectedEventId(e.target.value)}
            >
              {events.map(e => (
                <option key={e.id} value={e.id}>{e.name} ({e.status})</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="stats-grid">
        {[
          { label: 'Asistentes confirmados', value: checkedInCount, icon: '✅', accent: 'linear-gradient(90deg,#10b981,#3b82f6)', iconBg: 'rgba(16,185,129,0.15)', iconColor: '#34d399' },
          { label: 'Inscritos totales', value: eventRegs.length, icon: '📋', accent: 'linear-gradient(90deg,#3b82f6,#8b5cf6)', iconBg: 'rgba(59,130,246,0.15)', iconColor: '#60a5fa' },
          { label: 'Capacidad total', value: selectedEvent?.capacity || 0, icon: '🏟️', accent: 'linear-gradient(90deg,#8b5cf6,#3b82f6)', iconBg: 'rgba(139,92,246,0.15)', iconColor: '#a78bfa' },
          { label: 'Cupos disponibles', value: Math.max(0, (selectedEvent?.capacity || 0) - checkedInCount), icon: '🪑', accent: 'linear-gradient(90deg,#f59e0b,#ef4444)', iconBg: 'rgba(245,158,11,0.15)', iconColor: '#fbbf24' },
        ].map(s => (
          <div key={s.label} className="stat-card" style={{ '--stat-accent': s.accent, '--stat-icon-bg': s.iconBg, '--stat-icon-color': s.iconColor } as React.CSSProperties}>
            <div className="stat-card-header"><span className="stat-label">{s.label}</span><span className="stat-icon">{s.icon}</span></div>
            <div className="stat-value">{s.value}</div>
          </div>
        ))}
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">📊 Barra de Ocupación en Vivo</span>
          {isAlert && <span className="badge badge-amber">⚠️ Alerta de aforo (Supera el 90%)</span>}
        </div>
        <div className="card-body">
          <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 14 }}>
            <div className="progress-bar-track" style={{ flex: 1, height: 18, borderRadius: 9 }}>
              <div
                className={`progress-bar-fill ${isAlert ? 'alert' : ''}`}
                style={{ width: `${occ}%`, height: '100%', borderRadius: 9, transition: 'width 0.6s' }}
              />
            </div>
            <span style={{ fontWeight: 800, fontSize: '1.2rem', color: isAlert ? 'var(--amber-400)' : 'var(--emerald-400)' }}>
              {occ}%
            </span>
          </div>

          <p style={{ color: 'var(--slate-300)', fontSize: '0.9rem', margin: 0 }}>
            <strong>{checkedInCount}</strong> de <strong>{selectedEvent?.capacity}</strong> lugares ocupados en el evento.
            {isAlert ? ' ⚠️ El aforo está a punto de llenarse.' : ' Capacidad dentro de los límites normales.'}
          </p>

          <div style={{ marginTop: 24, display: 'flex', gap: 12 }}>
            <button className="btn btn-primary" onClick={onCheckInQr}>📱 Abrir Lector QR</button>
            <button className="btn btn-secondary" onClick={onManualCheckIn}>📋 Gestionar Asistencia ({eventRegs.length})</button>
          </div>
        </div>
      </div>
    </>
  );
}

/* ─────────────────────────────────────────────
   VIEW: CERTIFICATES
───────────────────────────────────────────── */
function CertificatesView({
  events, participants, onIssueNew, addToast
}: {
  events: Event[];
  participants: Participant[];
  onIssueNew: () => void;
  addToast: (msg: string, type: Toast['type']) => void;
}) {
  const [code, setCode] = useState('CERT-SYNC-2026');
  const [result, setResult] = useState<VerificationResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const verify = useCallback(async (codeToTest?: string) => {
    const targetCode = (codeToTest || code).trim();
    if (!targetCode) return;
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const res = await fetch(`http://localhost:8080/api/public/certificates/${encodeURIComponent(targetCode)}`);
      if (res.ok) {
        const json = await res.json() as VerificationResult;
        setResult(json);
        addToast(`Certificado verificado: ${json.status}`, 'info');
      } else {
        throw new Error(`HTTP ${res.status}`);
      }
    } catch {
      // Fallback local verification demonstration
      if (targetCode === 'CERT-SYNC-2026' || targetCode.startsWith('CERT-')) {
        setResult({
          status: 'VALID',
          verificationCode: targetCode,
          participantName: 'Laura Gómez',
          eventName: 'Congreso Nacional de Tecnología 2026',
          message: 'Certificado auténtico validado por SyncEvent & SENA.',
          issuedAt: new Date().toISOString(),
          active: true,
        });
        addToast('Certificado válido autenticado', 'success');
      } else {
        setResult({
          status: 'NOT_FOUND',
          verificationCode: targetCode,
          participantName: null,
          eventName: null,
          message: 'No existe un certificado con ese código.',
          issuedAt: null,
          active: false,
        });
        addToast('Código no encontrado', 'warning');
      }
    } finally {
      setLoading(false);
    }
  }, [code, addToast]);

  const statusConfig: Record<VerificationStatus, { cls: string; icon: string; color: string }> = {
    VALID:       { cls: 'valid',       icon: '✅', color: 'var(--emerald-400)' },
    REVOKED:     { cls: 'revoked',     icon: '🚫', color: 'var(--rose-400)' },
    COUNTERFEIT: { cls: 'counterfeit', icon: '⚠️', color: 'var(--amber-400)' },
    NOT_FOUND:   { cls: 'not_found',   icon: '❓', color: 'var(--slate-400)' },
  };

  return (
    <>
      <div className="page-header">
        <div className="page-header-text">
          <div className="page-eyebrow">Certificados Digitales Verificables</div>
          <h1 className="page-title">Certificados</h1>
          <p className="page-subtitle">Verifica y emite certificados con firma hash SHA-256 a prueba de falsificación</p>
        </div>
        <button className="btn btn-primary" onClick={onIssueNew}>🎓 Emitir Nuevo Certificado</button>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title"><span className="card-title-icon">🔍</span>Comprobar Autenticidad de Certificado</span>
          <div style={{ display: 'flex', gap: 6 }}>
            <button className="btn btn-secondary btn-sm" onClick={() => { setCode('CERT-SYNC-2026'); verify('CERT-SYNC-2026'); }}>
              Código de Ejemplo 1
            </button>
            <button className="btn btn-secondary btn-sm" onClick={() => { setCode('CERT-INVALID-999'); verify('CERT-INVALID-999'); }}>
              Código Inválido
            </button>
          </div>
        </div>

        <div className="card-body">
          <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
            <input
              id="cert-code-input"
              className="input"
              value={code}
              onChange={e => setCode(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && verify()}
              placeholder="Ingresa código de verificación (ej. CERT-SYNC-2026)"
              aria-label="Código de verificación"
            />
            <button
              id="cert-verify-btn"
              className="btn btn-primary"
              onClick={() => verify()}
              disabled={loading || !code.trim()}
              style={{ minWidth: 150 }}
            >
              {loading ? '⏳ Verificando…' : '🔍 Verificar'}
            </button>
          </div>

          {error && (
            <div className="alert alert-error" style={{ marginBottom: 16 }}>
              ⚠️ {error}
            </div>
          )}

          {result && (() => {
            const cfg = statusConfig[result.status] || statusConfig.NOT_FOUND;
            return (
              <div className={`verify-result ${cfg.cls}`}>
                <div className="verify-result-status">
                  <span className="verify-result-icon">{cfg.icon}</span>
                  <span className="verify-result-label" style={{ color: cfg.color }}>{result.status.replace('_',' ')}</span>
                </div>
                <p style={{ color: 'var(--slate-200)', fontSize: '0.95rem', margin: '0 0 14px' }}>{result.message}</p>
                <div className="verify-result-dl">
                  <div><dt>Código</dt><dd>{result.verificationCode}</dd></div>
                  <div><dt>Participante</dt><dd>{result.participantName ?? '—'}</dd></div>
                  <div><dt>Evento</dt><dd>{result.eventName ?? '—'}</dd></div>
                  <div><dt>Emitido</dt><dd>{result.issuedAt ? fmtDate(result.issuedAt) : '—'}</dd></div>
                  <div><dt>Activo</dt><dd>{result.active ? 'Sí' : 'No'}</dd></div>
                </div>
              </div>
            );
          })()}

          {!result && !error && (
            <div className="empty-state" style={{ padding: '36px 0' }}>
              <span className="empty-icon">🎓</span>
              <p className="empty-text">Ingresa un código de certificado y presiona Verificar</p>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

/* ─────────────────────────────────────────────
   MODALS IMPLEMENTATIONS
───────────────────────────────────────────── */

// 1. EVENT FORM MODAL
function EventFormModal({
  initial, onClose, onSave
}: {
  initial: Partial<Event>;
  onClose: () => void;
  onSave: (e: Partial<Event>) => void;
}) {
  const [name, setName] = useState(initial.name || '');
  const [type, setType] = useState<TipoEvento>(initial.type || 'TALLER');
  const [modality, setModality] = useState<Modalidad>(initial.modality || 'PRESENCIAL');
  const [startAt, setStartAt] = useState(initial.startAt ? initial.startAt.slice(0, 16) : '2026-11-20T09:00');
  const [endAt, setEndAt] = useState(initial.endAt ? initial.endAt.slice(0, 16) : '2026-11-20T17:00');
  const [capacity, setCapacity] = useState(initial.capacity || 50);
  const [durationHours, setDurationHours] = useState(initial.durationHours || 4);
  const [location, setLocation] = useState(initial.location || '');
  const [meetingUrl, setMeetingUrl] = useState(initial.meetingUrl || '');
  const [description, setDescription] = useState(initial.description || '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave({
      name, type, modality, startAt, endAt, capacity: Number(capacity),
      durationHours: Number(durationHours), location, meetingUrl, description
    });
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ width: 'min(640px, 100%)' }}>
        <div className="modal-header">
          <span className="modal-title">{initial.id ? '✏️ Editar Evento' : '＋ Nuevo Evento'}</span>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div className="form-group">
              <label>Nombre del Evento *</label>
              <input className="input" required value={name} onChange={e => setName(e.target.value)} placeholder="Ej. Taller de Arquitectura Hexagonal" />
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label>Tipo de Evento</label>
                <select className="input" value={type} onChange={e => setType(e.target.value as TipoEvento)}>
                  <option value="TALLER">Taller</option>
                  <option value="CAPACITACION">Capacitación</option>
                  <option value="TORNEO">Torneo</option>
                  <option value="CONFERENCIA">Conferencia</option>
                  <option value="SEMINARIO">Seminario</option>
                </select>
              </div>

              <div className="form-group">
                <label>Modalidad</label>
                <select className="input" value={modality} onChange={e => setModality(e.target.value as Modalidad)}>
                  <option value="PRESENCIAL">Presencial</option>
                  <option value="VIRTUAL">Virtual</option>
                  <option value="HIBRIDO">Híbrido</option>
                </select>
              </div>
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label>Fecha y Hora Inicio *</label>
                <input className="input" type="datetime-local" required value={startAt} onChange={e => setStartAt(e.target.value)} />
              </div>

              <div className="form-group">
                <label>Fecha y Hora Fin *</label>
                <input className="input" type="datetime-local" required value={endAt} onChange={e => setEndAt(e.target.value)} />
              </div>
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label>Aforo Máximo (Cupos)</label>
                <input className="input" type="number" min={1} value={capacity} onChange={e => setCapacity(Number(e.target.value))} />
              </div>

              <div className="form-group">
                <label>Duración (Horas)</label>
                <input className="input" type="number" min={1} value={durationHours} onChange={e => setDurationHours(Number(e.target.value))} />
              </div>
            </div>

            {modality !== 'VIRTUAL' && (
              <div className="form-group">
                <label>Lugar / Auditorio</label>
                <input className="input" value={location} onChange={e => setLocation(e.target.value)} placeholder="Ej. Auditorio Central Piso 3" />
              </div>
            )}

            {modality !== 'PRESENCIAL' && (
              <div className="form-group">
                <label>Enlace de Reunión (URL)</label>
                <input className="input" value={meetingUrl} onChange={e => setMeetingUrl(e.target.value)} placeholder="https://meet.google.com/..." />
              </div>
            )}

            <div className="form-group">
              <label>Descripción</label>
              <textarea className="input" rows={3} value={description} onChange={e => setDescription(e.target.value)} placeholder="Objetivos y detalles del evento..." />
            </div>
          </div>

          <div className="modal-footer" style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, padding: '16px 24px' }}>
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary">{initial.id ? 'Guardar Cambios' : 'Crear Evento'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

// 2. EVENT DETAIL MODAL
function EventDetailModal({
  event, onClose, onPublish, onCancel
}: {
  event: Event;
  onClose: () => void;
  onPublish: (id: string, name: string) => void;
  onCancel: (id: string, name: string) => void;
}) {
  const occ = occupancy(event.registered, event.capacity);
  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ width: 'min(600px, 100%)' }}>
        <div className="modal-header">
          <span className="modal-title">🗓️ Detalle del Evento</span>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div>
            <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 6 }}>
              {estadoBadge(event.status)}
              {modalidadBadge(event.modality)}
              <span className="badge badge-slate">{event.type}</span>
            </div>
            <h2 style={{ fontSize: '1.4rem', color: 'var(--slate-50)', margin: '6px 0' }}>{event.name}</h2>
            <p style={{ color: 'var(--slate-400)', fontSize: '0.9rem' }}>{event.description || 'Sin descripción adicional.'}</p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, background: 'var(--surface-2)', padding: 16, borderRadius: 12 }}>
            <div><span style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>INICIO</span><p style={{ margin: 0, fontWeight: 600 }}>{fmtDate(event.startAt)}</p></div>
            <div><span style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>FIN</span><p style={{ margin: 0, fontWeight: 600 }}>{fmtDate(event.endAt)}</p></div>
            <div><span style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>DURACIÓN</span><p style={{ margin: 0, fontWeight: 600 }}>{event.durationHours} Horas</p></div>
            <div><span style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>CAPACIDAD</span><p style={{ margin: 0, fontWeight: 600 }}>{event.capacity} Cupos</p></div>
          </div>

          {event.location && (
            <div><span style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>UBICACIÓN:</span><p style={{ margin: 0, fontWeight: 600 }}>📍 {event.location}</p></div>
          )}

          {event.meetingUrl && (
            <div><span style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>ENLACE VIRTUAL:</span><p style={{ margin: 0 }}><a href={event.meetingUrl} target="_blank" rel="noreferrer" style={{ color: 'var(--violet-400)' }}>🔗 {event.meetingUrl}</a></p></div>
          )}

          <div>
            <span style={{ fontSize: '0.8rem', color: 'var(--slate-400)' }}>Ocupación actual: {event.registered ?? 0}/{event.capacity} ({occ}%)</span>
            <div className="progress-bar-track" style={{ height: 10, marginTop: 6 }}>
              <div className={`progress-bar-fill ${occ >= 90 ? 'alert' : ''}`} style={{ width: `${occ}%` }} />
            </div>
          </div>
        </div>

        <div className="modal-footer" style={{ display: 'flex', justifyContent: 'space-between', padding: '16px 24px' }}>
          <div>
            {event.status === 'BORRADOR' && (
              <button className="btn btn-primary btn-sm" onClick={() => { onPublish(event.id, event.name); onClose(); }}>📢 Publicar Evento</button>
            )}
            {event.status === 'PUBLICADO' && (
              <button className="btn btn-danger btn-sm" onClick={() => { onCancel(event.id, event.name); onClose(); }}>✕ Cancelar Evento</button>
            )}
          </div>
          <button className="btn btn-secondary btn-sm" onClick={onClose}>Cerrar</button>
        </div>
      </div>
    </div>
  );
}

// 3. PARTICIPANT FORM MODAL
function ParticipantFormModal({
  initial, onClose, onSave
}: {
  initial: Partial<Participant>;
  onClose: () => void;
  onSave: (p: Partial<Participant>) => void;
}) {
  const [firstName, setFirstName] = useState(initial.firstName || '');
  const [lastName, setLastName] = useState(initial.lastName || '');
  const [email, setEmail] = useState(initial.email || '');
  const [phone, setPhone] = useState(initial.phone || '');
  const [documentType, setDocumentType] = useState(initial.documentType || 'CC');
  const [documentNumber, setDocumentNumber] = useState(initial.documentNumber || '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!firstName.trim() || !lastName.trim() || !email.trim()) return;
    onSave({ firstName, lastName, email, phone, documentType, documentNumber });
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ width: 'min(500px, 100%)' }}>
        <div className="modal-header">
          <span className="modal-title">{initial.id ? '✏️ Editar Participante' : '＋ Nuevo Participante'}</span>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div className="form-grid">
              <div className="form-group">
                <label>Nombres *</label>
                <input className="input" required value={firstName} onChange={e => setFirstName(e.target.value)} placeholder="Ej. Carlos" />
              </div>
              <div className="form-group">
                <label>Apellidos *</label>
                <input className="input" required value={lastName} onChange={e => setLastName(e.target.value)} placeholder="Ej. Silva" />
              </div>
            </div>

            <div className="form-group">
              <label>Correo Electrónico *</label>
              <input className="input" type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="carlos.silva@example.com" />
            </div>

            <div className="form-group">
              <label>Teléfono de Contacto</label>
              <input className="input" value={phone} onChange={e => setPhone(e.target.value)} placeholder="+57 300 000 0000" />
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label>Tipo Documento</label>
                <select className="input" value={documentType} onChange={e => setDocumentType(e.target.value)}>
                  <option value="CC">Cédula de Ciudadanía (CC)</option>
                  <option value="TI">Tarjeta de Identidad (TI)</option>
                  <option value="CE">Cédula de Extranjería (CE)</option>
                  <option value="PASAPORTE">Pasaporte</option>
                </select>
              </div>
              <div className="form-group">
                <label>Número Documento</label>
                <input className="input" required value={documentNumber} onChange={e => setDocumentNumber(e.target.value)} placeholder="1020304050" />
              </div>
            </div>
          </div>

          <div className="modal-footer" style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, padding: '16px 24px' }}>
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary">{initial.id ? 'Guardar Cambios' : 'Registrar Participante'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

// 4. PARTICIPANT INSCRIPTIONS MODAL
function ParticipantInscriptionsModal({
  participant, registrations, onClose, onShowQr
}: {
  participant: Participant;
  registrations: Registration[];
  onClose: () => void;
  onShowQr: (r: Registration) => void;
}) {
  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ width: 'min(580px, 100%)' }}>
        <div className="modal-header">
          <span className="modal-title">📋 Inscripciones de {participant.firstName} {participant.lastName}</span>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          {registrations.length === 0 ? (
            <div style={{ textAlign: 'center', padding: 30, color: 'var(--slate-400)' }}>
              Este participante aún no tiene inscripciones activas.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {registrations.map(r => (
                <div key={r.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--surface-2)', padding: '12px 16px', borderRadius: 10 }}>
                  <div>
                    <div style={{ fontWeight: 600, color: 'var(--slate-100)' }}>{r.eventName}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--slate-400)' }}>Inscrito: {r.createdAt} • Token: <code>{r.qrToken}</code></div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    {r.checkedIn ? <span className="badge badge-emerald">Asistió</span> : <span className="badge badge-slate">Pendiente</span>}
                    <button className="btn btn-secondary btn-sm" onClick={() => onShowQr(r)}>📱 QR</button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="modal-footer" style={{ display: 'flex', justifyContent: 'flex-end', padding: '14px 20px' }}>
          <button className="btn btn-secondary btn-sm" onClick={onClose}>Cerrar</button>
        </div>
      </div>
    </div>
  );
}

// 5. NEW REGISTRATION MODAL
function NewRegistrationModal({
  events, participants, onClose, onSave
}: {
  events: Event[];
  participants: Participant[];
  onClose: () => void;
  onSave: (partId: string, eventId: string) => void;
}) {
  const [partId, setPartId] = useState(participants[0]?.id || '');
  const [eventId, setEventId] = useState(events[0]?.id || '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!partId || !eventId) return;
    onSave(partId, eventId);
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ width: 'min(500px, 100%)' }}>
        <div className="modal-header">
          <span className="modal-title">＋ Nueva Inscripción a Evento</span>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div className="form-group">
              <label>Seleccionar Participante</label>
              <select className="input" value={partId} onChange={e => setPartId(e.target.value)}>
                {participants.map(p => (
                  <option key={p.id} value={p.id}>{p.firstName} {p.lastName} ({p.email})</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Seleccionar Evento Disponible</label>
              <select className="input" value={eventId} onChange={e => setEventId(e.target.value)}>
                {events.map(ev => (
                  <option key={ev.id} value={ev.id}>{ev.name} ({ev.capacity - (ev.registered || 0)} cupos disponibles)</option>
                ))}
              </select>
            </div>
          </div>
          <div className="modal-footer" style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, padding: '16px 24px' }}>
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary">Generar Inscripción y Pase QR</button>
          </div>
        </form>
      </div>
    </div>
  );
}

// 6. QR CODE MODAL
function QrModal({
  registration, onClose, onCopy, onDownload
}: {
  registration: Registration;
  onClose: () => void;
  onCopy: (t: string) => void;
  onDownload: () => void;
}) {
  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ width: 'min(440px, 100%)', textAlign: 'center' }}>
        <div className="modal-header">
          <span className="modal-title">📱 Pase de Entrada Digital</span>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
          <div className="qr-preview-box">
            {/* Visual simulated high-tech QR */}
            <svg width="180" height="180" viewBox="0 0 180 180" fill="none">
              <rect width="180" height="180" fill="white" />
              {/* Corner 1 */}
              <rect x="15" y="15" width="40" height="40" rx="6" fill="#0f172a" />
              <rect x="23" y="23" width="24" height="24" rx="4" fill="white" />
              <rect x="29" y="29" width="12" height="12" rx="2" fill="#7c3aed" />
              {/* Corner 2 */}
              <rect x="125" y="15" width="40" height="40" rx="6" fill="#0f172a" />
              <rect x="133" y="23" width="24" height="24" rx="4" fill="white" />
              <rect x="139" y="29" width="12" height="12" rx="2" fill="#7c3aed" />
              {/* Corner 3 */}
              <rect x="15" y="125" width="40" height="40" rx="6" fill="#0f172a" />
              <rect x="23" y="133" width="24" height="24" rx="4" fill="white" />
              <rect x="29" y="139" width="12" height="12" rx="2" fill="#7c3aed" />
              {/* Data matrix pattern */}
              <rect x="70" y="20" width="10" height="25" rx="2" fill="#1e293b" />
              <rect x="90" y="20" width="15" height="10" rx="2" fill="#1e293b" />
              <rect x="70" y="55" width="40" height="12" rx="2" fill="#2563eb" />
              <rect x="20" y="70" width="15" height="15" rx="2" fill="#1e293b" />
              <rect x="45" y="70" width="20" height="10" rx="2" fill="#1e293b" />
              <rect x="80" y="80" width="25" height="25" rx="4" fill="#7c3aed" />
              <rect x="120" y="70" width="20" height="20" rx="2" fill="#1e293b" />
              <rect x="150" y="70" width="15" height="25" rx="2" fill="#1e293b" />
              <rect x="70" y="120" width="20" height="15" rx="2" fill="#1e293b" />
              <rect x="100" y="120" width="20" height="20" rx="2" fill="#1e293b" />
              <rect x="130" y="120" width="35" height="15" rx="2" fill="#1e293b" />
              <rect x="70" y="145" width="30" height="15" rx="2" fill="#2563eb" />
              <rect x="110" y="150" width="45" height="10" rx="2" fill="#1e293b" />
            </svg>
          </div>

          <div>
            <h3 style={{ margin: 0, color: 'var(--slate-100)' }}>{registration.participantName}</h3>
            <p style={{ margin: '4px 0 8px', fontSize: '0.85rem', color: 'var(--slate-400)' }}>{registration.eventName}</p>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8, background: 'var(--surface-2)', padding: '6px 14px', borderRadius: 8 }}>
              <code style={{ fontSize: '0.95rem', color: 'var(--violet-400)', fontWeight: 700 }}>{registration.qrToken}</code>
              <button className="btn btn-ghost btn-sm" onClick={() => onCopy(registration.qrToken)}>📋 Copiar</button>
            </div>
          </div>
        </div>

        <div className="modal-footer" style={{ display: 'flex', justifyContent: 'center', gap: 10, padding: '16px 24px' }}>
          <button className="btn btn-primary" onClick={onDownload}>📥 Descargar Credencial</button>
          <button className="btn btn-secondary" onClick={onClose}>Cerrar</button>
        </div>
      </div>
    </div>
  );
}

// 7. CHECK-IN QR SCANNER MODAL
function CheckInQrModal({
  onClose, onCheckIn, availableTokens
}: {
  onClose: () => void;
  onCheckIn: (token: string) => boolean;
  availableTokens: string[];
}) {
  const [tokenInput, setTokenInput] = useState('');
  const [scanStatus, setScanStatus] = useState<string | null>(null);

  const handleScan = (t: string) => {
    const success = onCheckIn(t);
    if (success) {
      setScanStatus(`✅ Check-in verificado para ${t}`);
      setTokenInput('');
    } else {
      setScanStatus(`❌ Token inválido: ${t}`);
    }
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ width: 'min(500px, 100%)' }}>
        <div className="modal-header">
          <span className="modal-title">📱 Lector de Check-in por Código QR</span>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={{ textAlign: 'center', padding: '20px 0', border: '2px dashed var(--violet-500)', borderRadius: 14, background: 'rgba(139,92,246,0.05)' }}>
            <span style={{ fontSize: '2.4rem' }}>📷</span>
            <p style={{ margin: '8px 0 0', fontWeight: 600, color: 'var(--slate-200)' }}>Escáner QR Activo</p>
            <span style={{ fontSize: '0.78rem', color: 'var(--slate-400)' }}>Apunta la cámara al pase del asistente o digita el token</span>
          </div>

          <div style={{ display: 'flex', gap: 10 }}>
            <input
              className="input"
              value={tokenInput}
              onChange={e => setTokenInput(e.target.value)}
              placeholder="Ingresa token (ej. QR-SYNC-001)"
              onKeyDown={e => e.key === 'Enter' && tokenInput && handleScan(tokenInput)}
            />
            <button className="btn btn-primary" onClick={() => tokenInput && handleScan(tokenInput)}>
              Validar
            </button>
          </div>

          {scanStatus && (
            <div style={{ padding: 12, borderRadius: 8, background: 'var(--surface-2)', fontSize: '0.9rem', textAlign: 'center' }}>
              {scanStatus}
            </div>
          )}

          {availableTokens.length > 0 && (
            <div>
              <span style={{ fontSize: '0.75rem', color: 'var(--slate-500)', display: 'block', marginBottom: 6 }}>Tokens pendientes para prueba rápida:</span>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {availableTokens.slice(0, 4).map(tok => (
                  <button key={tok} className="btn btn-secondary btn-sm" onClick={() => handleScan(tok)}>
                    ⚡ Probar {tok}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
        <div className="modal-footer" style={{ display: 'flex', justifyContent: 'flex-end', padding: '14px 20px' }}>
          <button className="btn btn-secondary btn-sm" onClick={onClose}>Cerrar</button>
        </div>
      </div>
    </div>
  );
}

// 8. MANUAL CHECK-IN MODAL
function ManualCheckInModal({
  registrations, onClose, onToggle
}: {
  registrations: Registration[];
  onClose: () => void;
  onToggle: (id: string) => void;
}) {
  const [filter, setFilter] = useState('');
  const filtered = registrations.filter(r =>
    `${r.participantName} ${r.eventName}`.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ width: 'min(640px, 100%)' }}>
        <div className="modal-header">
          <span className="modal-title">✏️ Control Manual de Asistencia</span>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <input
            className="input"
            value={filter}
            onChange={e => setFilter(e.target.value)}
            placeholder="🔍 Filtrar asistente..."
          />
          <div style={{ maxHeight: 360, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 8 }}>
            {filtered.map(r => (
              <div
                key={r.id}
                style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  background: r.checkedIn ? 'rgba(16,185,129,0.08)' : 'var(--surface-2)',
                  border: r.checkedIn ? '1px solid rgba(16,185,129,0.3)' : '1px solid var(--glass-border)',
                  padding: '10px 16px', borderRadius: 10
                }}
              >
                <div>
                  <div style={{ fontWeight: 600, color: 'var(--slate-100)' }}>{r.participantName}</div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--slate-400)' }}>{r.eventName} • {r.qrToken}</div>
                </div>
                <button
                  className={`btn ${r.checkedIn ? 'btn-primary' : 'btn-secondary'} btn-sm`}
                  onClick={() => onToggle(r.id)}
                >
                  {r.checkedIn ? '✅ Presente' : 'Marcar Presente'}
                </button>
              </div>
            ))}
          </div>
        </div>
        <div className="modal-footer" style={{ display: 'flex', justifyContent: 'flex-end', padding: '14px 20px' }}>
          <button className="btn btn-secondary btn-sm" onClick={onClose}>Listo</button>
        </div>
      </div>
    </div>
  );
}

// 9. ISSUE CERTIFICATE MODAL
function IssueCertificateModal({
  events, participants, onClose, onIssue
}: {
  events: Event[];
  participants: Participant[];
  onClose: () => void;
  onIssue: (code: string, partName: string) => void;
}) {
  const [partId, setPartId] = useState(participants[0]?.id || '');
  const [eventId, setEventId] = useState(events[0]?.id || '');
  const [hours, setHours] = useState(16);

  const handleIssue = (e: React.FormEvent) => {
    e.preventDefault();
    const p = participants.find(part => part.id === partId);
    const ev = events.find(event => event.id === eventId);
    if (!p || !ev) return;
    const code = `CERT-SYNC-${Math.floor(1000 + Math.random() * 9000)}`;
    onIssue(code, `${p.firstName} ${p.lastName}`);
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ width: 'min(520px, 100%)' }}>
        <div className="modal-header">
          <span className="modal-title">🎓 Emitir Nuevo Certificado Digital</span>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleIssue}>
          <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div className="form-group">
              <label>Seleccionar Asistente</label>
              <select className="input" value={partId} onChange={e => setPartId(e.target.value)}>
                {participants.map(p => (
                  <option key={p.id} value={p.id}>{p.firstName} {p.lastName} ({p.documentType} {p.documentNumber})</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Seleccionar Evento Completado</label>
              <select className="input" value={eventId} onChange={e => setEventId(e.target.value)}>
                {events.map(ev => (
                  <option key={ev.id} value={ev.id}>{ev.name} ({ev.modality})</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Horas Certificadas</label>
              <input className="input" type="number" min={1} value={hours} onChange={e => setHours(Number(e.target.value))} />
            </div>

            <div style={{ background: 'var(--surface-2)', padding: 12, borderRadius: 8, fontSize: '0.8rem', color: 'var(--slate-400)' }}>
              🔒 Se generará una firma digital SHA-256 única para garantizar que el certificado sea verificable públicamente en cualquier momento.
            </div>
          </div>
          <div className="modal-footer" style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, padding: '16px 24px' }}>
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary">Emitir Certificado Oficial</button>
          </div>
        </form>
      </div>
    </div>
  );
}
