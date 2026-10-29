import React, { useState } from 'react';
import { Event, Participant, Registration, CertificateRecord, EstadoEvento, Modalidad, TipoEvento } from '../types';
import { Icon } from '../icons';

interface OperatorScreenProps {
  events: Event[];
  participants: Participant[];
  registrations: Registration[];
  certificates: CertificateRecord[];
  onSaveEvent: (data: Partial<Event>) => void;
  onUpdateEventStatus: (id: string, status: EstadoEvento) => void;
  onCancelEvent: (id: string, reason: string) => void;
  onPostponeEvent: (id: string, newStart: string, newEnd: string, reason: string) => void;
  onAssignMonitors: (eventId: string, monitorEmails: string[]) => void;
  onAddParticipant: (data: Omit<Participant, 'id' | 'createdAt'>) => void;
  onRegisterParticipantToEvent: (participantId: string, eventId: string) => void;
  onBulkImportParticipants: (items: Omit<Participant, 'id' | 'createdAt'>[]) => void;
  onIssueEventCertificates: (eventId: string) => void;
  addToast: (msg: string, type?: 'success' | 'info' | 'error' | 'warning') => void;
}

export function OperatorScreen({
  events,
  participants,
  registrations,
  certificates,
  onSaveEvent,
  onUpdateEventStatus,
  onCancelEvent,
  onPostponeEvent,
  onAssignMonitors,
  onAddParticipant,
  onRegisterParticipantToEvent,
  onBulkImportParticipants,
  onIssueEventCertificates,
  addToast,
}: OperatorScreenProps) {
  const [activeTab, setActiveTab] = useState<'events' | 'participants' | 'import' | 'certificates'>('events');

  // Event modal states
  const [showEventModal, setShowEventModal] = useState(false);
  const [editingEvent, setEditingEvent] = useState<Event | null>(null);
  const [eventForm, setEventForm] = useState({
    name: '',
    type: 'TALLER' as TipoEvento,
    modality: 'PRESENCIAL' as Modalidad,
    startAt: '2026-11-20T09:00',
    endAt: '2026-11-20T17:00',
    location: '',
    meetingUrl: '',
    capacity: 50,
    durationHours: 8,
    description: '',
  });

  // Cancel modal
  const [cancellingEvent, setCancellingEvent] = useState<Event | null>(null);
  const [cancelReason, setCancelReason] = useState('');

  // Postpone modal
  const [postponingEvent, setPostponingEvent] = useState<Event | null>(null);
  const [postponeNewStart, setPostponeNewStart] = useState('');
  const [postponeNewEnd, setPostponeNewEnd] = useState('');
  const [postponeReason, setPostponeReason] = useState('');

  // Assign monitors modal
  const [assigningMonitorsEvent, setAssigningMonitorsEvent] = useState<Event | null>(null);
  const [selectedMonitors, setSelectedMonitors] = useState<string[]>([]);

  // Participant form modal
  const [showPartModal, setShowPartModal] = useState(false);
  const [partForm, setPartForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    documentType: 'CC',
    documentNumber: '',
  });

  // Direct registration modal
  const [showDirectRegModal, setShowDirectRegModal] = useState(false);
  const [selectedPartId, setSelectedPartId] = useState('');
  const [selectedEventId, setSelectedEventId] = useState('');

  // CSV Import State
  const [csvContent, setCsvContent] = useState<string>(
    `CC,1099887766,Daniela,Sánchez,daniela.sanchez@ejemplo.com,+57 312 456 7890\n` +
    `CC,1088776655,Felipe,Castro,felipe.castro@ejemplo.com,+57 315 987 6543\n` +
    `TI,1077665544,Valeria,Mora,valeria.mora@ejemplo.com,+57 301 234 5678\n` +
    `CC,1020304050,Laura,Gómez,laura.gomez@example.test,+57 310 123 4567` // intentional duplicate to demonstrate validation!
  );
  const [parsedPreview, setParsedPreview] = useState<{
    valid: Omit<Participant, 'id' | 'createdAt'>[];
    errors: { line: number; raw: string; error: string }[];
  } | null>(null);

  // Available monitors in system
  const availableMonitors = [
    { email: 'monitor1@syncevent.com', name: 'Carlos Díaz (Monitor Sala 1)' },
    { email: 'monitor2@syncevent.com', name: 'Andrea Ruiz (Monitor Auditorio)' },
    { email: 'monitor3@syncevent.com', name: 'Esteban Morales (Monitor Virtual)' },
  ];

  /* ── Save Event ── */
  const handleSaveEventSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!eventForm.name.trim()) {
      addToast('El nombre del evento es requerido', 'warning');
      return;
    }
    // RN-21: Check overlapping venue and time with other active events
    if (eventForm.modality === 'PRESENCIAL' && eventForm.location.trim()) {
      const overlap = events.find(ev => 
        ev.id !== editingEvent?.id &&
        ev.status !== 'CANCELADO' &&
        ev.location?.trim().toLowerCase() === eventForm.location.trim().toLowerCase() &&
        ev.startAt.slice(0, 10) === eventForm.startAt.slice(0, 10)
      );
      if (overlap) {
        if (!window.confirm(`Alerta Regla RN-21: Ya existe el evento "${overlap.name}" en el mismo lugar (${eventForm.location}) en esta fecha. ¿Deseas continuar bajo tu autorización?`)) {
          return;
        }
      }
    }

    onSaveEvent({
      ...editingEvent,
      ...eventForm,
    });
    addToast(`Evento "${eventForm.name}" guardado exitosamente`, 'success');
    setShowEventModal(false);
  };

  /* ── Save Participant ── */
  const handleSaveParticipantSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!partForm.firstName.trim() || !partForm.email.trim() || !partForm.documentNumber.trim()) {
      addToast('Nombre, correo y documento son requeridos', 'warning');
      return;
    }
    // RN-02: Document is unique
    if (participants.some(p => p.documentNumber === partForm.documentNumber)) {
      addToast(`Regla RN-02: El documento ${partForm.documentNumber} ya está registrado para otro participante`, 'error');
      return;
    }

    onAddParticipant(partForm);
    addToast(`Participante ${partForm.firstName} ${partForm.lastName} registrado con éxito`, 'success');
    setShowPartModal(false);
    setPartForm({ firstName: '', lastName: '', email: '', phone: '', documentType: 'CC', documentNumber: '' });
  };

  /* ── Direct Registration ── */
  const handleDirectRegistrationSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPartId || !selectedEventId) {
      addToast('Selecciona el participante y el evento', 'warning');
      return;
    }

    const ev = events.find(e => e.id === selectedEventId);
    if (!ev) return;

    // RN-05: Check capacity
    const regCount = registrations.filter(r => r.eventId === ev.id && r.status === 'ACTIVA').length;
    if (regCount >= ev.capacity) {
      addToast(`Regla RN-05: Aforo máximo alcanzado (${ev.capacity}/${ev.capacity}). No hay cupos disponibles.`, 'error');
      return;
    }

    // RN-20: Check schedule collision with other registered events
    const partRegs = registrations.filter(r => r.participantId === selectedPartId && r.status === 'ACTIVA');
    const hasCollision = partRegs.some(r => {
      const otherEv = events.find(o => o.id === r.eventId);
      return otherEv && otherEv.id !== ev.id && otherEv.startAt.slice(0, 10) === ev.startAt.slice(0, 10);
    });
    if (hasCollision) {
      addToast(`Regla RN-20: Cruce de horarios detectado con otro evento activo para este participante en la misma fecha`, 'error');
      return;
    }

    onRegisterParticipantToEvent(selectedPartId, selectedEventId);
    addToast('Participante inscrito exitosamente', 'success');
    setShowDirectRegModal(false);
  };

  /* ── Parse CSV ── */
  const handleParseCsv = () => {
    const lines = csvContent.split('\n').map(l => l.trim()).filter(Boolean);
    const valid: Omit<Participant, 'id' | 'createdAt'>[] = [];
    const errors: { line: number; raw: string; error: string }[] = [];
    const seenDocs = new Set(participants.map(p => p.documentNumber));

    lines.forEach((line, idx) => {
      const parts = line.split(',').map(p => p.trim());
      if (parts.length < 5) {
        errors.push({ line: idx + 1, raw: line, error: 'Formato inválido. Se requieren al menos: TipoDoc, Doc, Nombre, Apellido, Correo' });
        return;
      }
      const [docType, docNum, fName, lName, email, phone = ''] = parts;
      if (!docNum || !fName || !email) {
        errors.push({ line: idx + 1, raw: line, error: 'Campos requeridos faltantes (documento, nombre o correo)' });
        return;
      }
      if (seenDocs.has(docNum)) {
        errors.push({ line: idx + 1, raw: line, error: `Documento ${docNum} duplicado (ya existe en BD o en este archivo)` });
        return;
      }
      seenDocs.add(docNum);
      valid.push({
        documentType: docType || 'CC',
        documentNumber: docNum,
        firstName: fName,
        lastName: lName || '',
        email,
        phone,
      });
    });

    setParsedPreview({ valid, errors });
    addToast(`Análisis completado: ${valid.length} válidos, ${errors.length} con incidencias`, valid.length > 0 ? 'info' : 'warning');
  };

  const handleConfirmImport = () => {
    if (!parsedPreview || parsedPreview.valid.length === 0) return;
    onBulkImportParticipants(parsedPreview.valid);
    addToast(`¡Éxito! Se importaron ${parsedPreview.valid.length} participantes al directorio`, 'success');
    setParsedPreview(null);
    setCsvContent('');
  };

  return (
    <div className="actor-screen operator-screen animate-fade-in">
      {/* ── Actor Banner ── */}
      <div className="actor-header-banner" style={{ background: 'linear-gradient(135deg, rgba(154,129,103,0.18), rgba(60,49,39,0.3))', border: '1px solid rgba(179,157,125,0.25)', borderRadius: 'var(--radius-lg)', padding: '24px 28px', marginBottom: 28 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{ width: 52, height: 52, borderRadius: 14, background: 'linear-gradient(135deg,#9a8167,#604e3e)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', boxShadow: '0 8px 24px rgba(96,78,62,0.4)' }}>
              <span style={{ width: 26, height: 26 }}><Icon.Settings /></span>
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <h2 style={{ margin: 0, fontSize: '1.4rem', color: 'var(--sand-100)', fontFamily: 'var(--font-display)' }}>Centro Operativo del Operador</h2>
                <span className="badge badge-amber" style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: '0.72rem' }}>Gestión Integral</span>
              </div>
              <p style={{ margin: '4px 0 0', fontSize: '0.86rem', color: 'var(--stone-400)' }}>
                Ciclo de vida de eventos (RN-16), monitores (RN-15/22), importación masiva CSV y emisión de certificados automáticos (RN-26).
              </p>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className={`btn btn-sm ${activeTab === 'events' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('events')}>
              <span style={{ width: 14, height: 14 }}><Icon.Calendar /></span> Eventos ({events.length})
            </button>
            <button className={`btn btn-sm ${activeTab === 'participants' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('participants')}>
              <span style={{ width: 14, height: 14 }}><Icon.Users /></span> Participantes ({participants.length})
            </button>
            <button className={`btn btn-sm ${activeTab === 'import' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('import')}>
              <span style={{ width: 14, height: 14 }}><Icon.Upload /></span> Carga Masiva CSV
            </button>
            <button className={`btn btn-sm ${activeTab === 'certificates' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('certificates')}>
              <span style={{ width: 14, height: 14 }}><Icon.Award /></span> Emisión de Certificados
            </button>
          </div>
        </div>
      </div>

      {/* ── TAB 1: GESTIÓN DE EVENTOS Y CICLO DE VIDA (RN-16) ── */}
      {activeTab === 'events' && (
        <div className="tab-pane animate-fade-in">
          <div className="card">
            <div className="card-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <h3 className="card-title">Gestión y Ciclo de Vida de Eventos (RN-16)</h3>
                <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                  Secuencia estricta de estados: <strong>Borrador → Publicado → En Curso → Finalizado</strong>. Cancelaciones y aplazamientos con motivo auditable.
                </p>
              </div>
              <button
                className="btn btn-primary"
                onClick={() => {
                  setEditingEvent(null);
                  setEventForm({
                    name: '',
                    type: 'TALLER',
                    modality: 'PRESENCIAL',
                    startAt: '2026-11-20T09:00',
                    endAt: '2026-11-20T17:00',
                    location: '',
                    meetingUrl: '',
                    capacity: 50,
                    durationHours: 8,
                    description: '',
                  });
                  setShowEventModal(true);
                }}
              >
                <span style={{ width: 16, height: 16 }}><Icon.Plus /></span> Crear Nuevo Evento
              </button>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Evento / Nombre</th>
                    <th>Tipo / Modalidad</th>
                    <th>Fechas y Horario</th>
                    <th>Aforo / Ocupación (RN-05)</th>
                    <th>Monitores Asignados</th>
                    <th>Estado Actual (RN-16)</th>
                    <th style={{ textAlign: 'right' }}>Acciones del Ciclo</th>
                  </tr>
                </thead>
                <tbody>
                  {events.map(ev => {
                    const regCount = registrations.filter(r => r.eventId === ev.id && r.status === 'ACTIVA').length;
                    const pct = Math.round((regCount / (ev.capacity || 1)) * 100);
                    return (
                      <tr key={ev.id}>
                        <td>
                          <div style={{ fontWeight: 600, color: 'var(--sand-100)' }}>{ev.name}</div>
                          <div style={{ fontSize: '0.74rem', color: 'var(--stone-400)' }}>
                            {ev.modality === 'PRESENCIAL' ? (ev.location || 'Lugar por definir') : (ev.meetingUrl || 'Enlace virtual')}
                          </div>
                        </td>
                        <td>
                          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                            <span className="badge badge-accent" style={{ fontSize: '0.72rem' }}>{ev.type}</span>
                            <span className="badge badge-slate" style={{ fontSize: '0.72rem' }}>{ev.modality}</span>
                          </div>
                        </td>
                        <td style={{ fontSize: '0.8rem', color: 'var(--stone-300)' }}>
                          <div>{ev.startAt.replace('T', ' ')}</div>
                          <div style={{ color: 'var(--stone-500)', fontSize: '0.74rem' }}>{ev.durationHours} horas programadas</div>
                        </td>
                        <td>
                          <div style={{ minWidth: 100 }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.76rem', marginBottom: 3 }}>
                              <span>{regCount} / {ev.capacity}</span>
                              <span style={{ color: pct >= 90 ? 'var(--error-400)' : 'var(--sand-400)' }}>{pct}%</span>
                            </div>
                            <div className="progress-bar">
                              <div className="progress-bar-fill" style={{ width: `${Math.min(pct, 100)}%`, background: pct >= 90 ? 'var(--error-500)' : 'var(--accent-400)' }} />
                            </div>
                          </div>
                        </td>
                        <td>
                          {ev.assignedMonitors && ev.assignedMonitors.length > 0 ? (
                            <span className="badge badge-emerald" style={{ fontSize: '0.72rem' }}>
                              {ev.assignedMonitors.length} monitor(es)
                            </span>
                          ) : (
                            <span className="badge badge-amber" style={{ fontSize: '0.72rem' }}>Sin asignar</span>
                          )}
                        </td>
                        <td>
                          <span className={`badge ${
                            ev.status === 'PUBLICADO' ? 'badge-blue' :
                            ev.status === 'EN_CURSO' ? 'badge-emerald' :
                            ev.status === 'FINALIZADO' ? 'badge-violet' :
                            ev.status === 'APLAZADO' ? 'badge-amber' :
                            ev.status === 'CANCELADO' ? 'badge-rose' : 'badge-slate'
                          }`}>
                            {ev.status}
                          </span>
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'inline-flex', gap: 6, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                            {/* Transición Borrador -> Publicado */}
                            {ev.status === 'BORRADOR' && (
                              <button
                                className="btn btn-secondary btn-sm"
                                title="Publicar evento"
                                onClick={() => {
                                  onUpdateEventStatus(ev.id, 'PUBLICADO');
                                  addToast(`Evento "${ev.name}" publicado para inscripciones`, 'success');
                                }}
                              >
                                Publicar
                              </button>
                            )}

                            {/* Transición Publicado -> En Curso */}
                            {ev.status === 'PUBLICADO' && (
                              <>
                                <button
                                  className="btn btn-primary btn-sm"
                                  title="Iniciar evento (pasar a En curso)"
                                  onClick={() => {
                                    onUpdateEventStatus(ev.id, 'EN_CURSO');
                                    addToast(`Evento "${ev.name}" en curso (Check-in habilitado)`, 'info');
                                  }}
                                >
                                  Iniciar
                                </button>
                                <button
                                  className="btn btn-ghost btn-sm"
                                  title="Aplazar fecha"
                                  onClick={() => {
                                    setPostponingEvent(ev);
                                    setPostponeNewStart(ev.startAt);
                                    setPostponeNewEnd(ev.endAt);
                                    setPostponeReason('');
                                  }}
                                >
                                  Aplazar
                                </button>
                              </>
                            )}

                            {/* Transición En Curso -> Finalizado */}
                            {ev.status === 'EN_CURSO' && (
                              <button
                                className="btn btn-primary btn-sm"
                                style={{ background: '#7c3aed', borderColor: '#7c3aed' }}
                                title="Finalizar evento"
                                onClick={() => {
                                  onUpdateEventStatus(ev.id, 'FINALIZADO');
                                  addToast(`Evento "${ev.name}" finalizado. Habilitada emisión de certificados.`, 'success');
                                }}
                              >
                                Finalizar
                              </button>
                            )}

                            {/* Asignar Monitores (RN-15, RN-22) */}
                            {ev.status !== 'FINALIZADO' && ev.status !== 'CANCELADO' && (
                              <button
                                className="btn btn-ghost btn-sm"
                                title="Asignar Monitores"
                                onClick={() => {
                                  setAssigningMonitorsEvent(ev);
                                  setSelectedMonitors(ev.assignedMonitors || []);
                                }}
                              >
                                Monitores
                              </button>
                            )}

                            {/* Cancelar Evento (RN-16, RN-23) */}
                            {ev.status !== 'FINALIZADO' && ev.status !== 'CANCELADO' && (
                              <button
                                className="btn btn-danger btn-sm"
                                title="Cancelar evento con motivo"
                                onClick={() => {
                                  setCancellingEvent(ev);
                                  setCancelReason('');
                                }}
                              >
                                Cancelar
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ── TAB 2: PARTICIPANTES E INSCRIPCIONES (RN-02, RN-05, RN-20) ── */}
      {activeTab === 'participants' && (
        <div className="tab-pane animate-fade-in">
          <div className="card">
            <div className="card-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <h3 className="card-title">Directorio de Participantes e Inscripciones</h3>
                <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                  Gestión individual de participantes, inscripción directa con control de cupos y verificación de cruce de horarios.
                </p>
              </div>
              <div style={{ display: 'flex', gap: 10 }}>
                <button className="btn btn-secondary" onClick={() => setShowDirectRegModal(true)}>
                  <span style={{ width: 16, height: 16 }}><Icon.ClipboardList /></span> Inscribir a Evento
                </button>
                <button className="btn btn-primary" onClick={() => setShowPartModal(true)}>
                  <span style={{ width: 16, height: 16 }}><Icon.Plus /></span> Nuevo Participante
                </button>
              </div>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Documento (RN-02)</th>
                    <th>Nombres y Apellidos</th>
                    <th>Correo Electrónico</th>
                    <th>Teléfono</th>
                    <th>Inscripciones Activas</th>
                    <th>Estado de Multas</th>
                  </tr>
                </thead>
                <tbody>
                  {participants.map(p => {
                    const userRegs = registrations.filter(r => r.participantEmail === p.email && r.status === 'ACTIVA');
                    return (
                      <tr key={p.id}>
                        <td style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--accent-400)' }}>
                          {p.documentType} {p.documentNumber}
                        </td>
                        <td style={{ fontWeight: 600, color: 'var(--sand-100)' }}>{p.firstName} {p.lastName}</td>
                        <td style={{ color: 'var(--sand-300)' }}>{p.email}</td>
                        <td style={{ color: 'var(--stone-400)' }}>{p.phone || '—'}</td>
                        <td>
                          <span className="badge badge-accent" style={{ fontSize: '0.74rem' }}>
                            {userRegs.length} evento(s)
                          </span>
                        </td>
                        <td>
                          {p.activeFines && p.activeFines > 0 ? (
                            <span className="badge badge-rose" style={{ fontSize: '0.74rem' }}>
                              {p.activeFines} multa(s) activa(s)
                            </span>
                          ) : (
                            <span className="badge badge-emerald" style={{ fontSize: '0.74rem' }}>Sin sanciones</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ── TAB 3: IMPORTACIÓN MASIVA CSV (RF-016 / Reto 5) ── */}
      {activeTab === 'import' && (
        <div className="tab-pane animate-fade-in">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Carga Masiva de Participantes en Formato CSV</h3>
              <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                Sube o pega un listado de participantes. El motor valida sintaxis, campos requeridos y detección de documentos duplicados antes de confirmar.
              </p>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div className="form-group">
                <label className="form-label">
                  Registros CSV (Formato: <code>TipoDoc, Documento, Nombres, Apellidos, Correo, Celular</code>):
                </label>
                <textarea
                  className="input"
                  rows={6}
                  style={{ fontFamily: 'monospace', fontSize: '0.82rem', lineHeight: 1.6 }}
                  value={csvContent}
                  onChange={e => {
                    setCsvContent(e.target.value);
                    setParsedPreview(null);
                  }}
                  placeholder="CC, 1020304050, Juan, Pérez, juan@ejemplo.com, +57 300 123 4567"
                />
              </div>

              <div style={{ display: 'flex', gap: 12 }}>
                <button className="btn btn-secondary" onClick={handleParseCsv}>
                  <span style={{ width: 16, height: 16 }}><Icon.CheckSquare /></span> Validar y Previsualizar Datos
                </button>
                {parsedPreview && parsedPreview.valid.length > 0 && (
                  <button className="btn btn-primary" onClick={handleConfirmImport}>
                    <span style={{ width: 16, height: 16 }}><Icon.Upload /></span> Confirmar e Importar {parsedPreview.valid.length} Registros Válidos
                  </button>
                )}
              </div>

              {parsedPreview && (
                <div style={{ marginTop: 20, display: 'flex', flexDirection: 'column', gap: 20 }}>
                  {/* Resumen */}
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                    <div style={{ background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.3)', borderRadius: 'var(--radius-sm)', padding: 16 }}>
                      <div style={{ color: 'var(--success-400)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ width: 18, height: 18 }}><Icon.CheckCircle /></span> Registros Válidos ({parsedPreview.valid.length})
                      </div>
                      <p style={{ margin: '4px 0 0', fontSize: '0.8rem', color: 'var(--stone-300)' }}>
                        Listos para ser guardados en la base de datos sin errores ni duplicados.
                      </p>
                    </div>
                    <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 'var(--radius-sm)', padding: 16 }}>
                      <div style={{ color: 'var(--error-400)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ width: 18, height: 18 }}><Icon.AlertTriangle /></span> Registros con Error / Rechazados ({parsedPreview.errors.length})
                      </div>
                      <p style={{ margin: '4px 0 0', fontSize: '0.8rem', color: 'var(--stone-300)' }}>
                        Filas que no cumplen las reglas de negocio (ej. documento repetido o formato inválido).
                      </p>
                    </div>
                  </div>

                  {/* Tabla de Válidos */}
                  {parsedPreview.valid.length > 0 && (
                    <div>
                      <h4 style={{ margin: '0 0 10px', color: 'var(--sand-100)', fontSize: '0.92rem' }}>Previsualización de Registros Válidos:</h4>
                      <div className="table-wrapper">
                        <table>
                          <thead>
                            <tr>
                              <th>Tipo / Documento</th>
                              <th>Nombre Completo</th>
                              <th>Correo</th>
                              <th>Teléfono</th>
                              <th>Estado</th>
                            </tr>
                          </thead>
                          <tbody>
                            {parsedPreview.valid.map((v, i) => (
                              <tr key={i}>
                                <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{v.documentType} {v.documentNumber}</td>
                                <td>{v.firstName} {v.lastName}</td>
                                <td>{v.email}</td>
                                <td>{v.phone}</td>
                                <td><span className="badge badge-emerald" style={{ fontSize: '0.72rem' }}>Válido</span></td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}

                  {/* Tabla de Errores */}
                  {parsedPreview.errors.length > 0 && (
                    <div>
                      <h4 style={{ margin: '0 0 10px', color: 'var(--error-400)', fontSize: '0.92rem' }}>Reporte de Incidencias / Errores:</h4>
                      <div className="table-wrapper">
                        <table>
                          <thead>
                            <tr>
                              <th>Línea</th>
                              <th>Texto Original</th>
                              <th>Causa de Rechazo</th>
                            </tr>
                          </thead>
                          <tbody>
                            {parsedPreview.errors.map((err, i) => (
                              <tr key={i}>
                                <td style={{ fontWeight: 600 }}>Línea {err.line}</td>
                                <td style={{ fontFamily: 'monospace', fontSize: '0.78rem', color: 'var(--stone-400)' }}>{err.raw}</td>
                                <td><span className="badge badge-rose" style={{ fontSize: '0.72rem' }}>{err.error}</span></td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── TAB 4: EMISIÓN DE CERTIFICADOS (RN-07, RN-26) ── */}
      {activeTab === 'certificates' && (
        <div className="tab-pane animate-fade-in">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Emisión Masiva de Certificados (RN-07, RN-26)</h3>
              <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                Regla RN-07: El certificado solo se emite a inscripciones con asistencia registrada (check-in) en eventos en estado FINALIZADO.
                Regla RN-26: Generación automática de certificados en un tiempo máximo de 20 minutos.
              </p>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Evento</th>
                    <th>Estado Evento</th>
                    <th>Horas Certificables</th>
                    <th>Total Inscritos</th>
                    <th>Asistentes con Check-in (RN-07)</th>
                    <th>Certificados Emitidos</th>
                    <th style={{ textAlign: 'right' }}>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {events.map(ev => {
                    const evRegs = registrations.filter(r => r.eventId === ev.id && r.status === 'ACTIVA');
                    const checkedInCount = evRegs.filter(r => r.checkedIn).length;
                    const certsCount = certificates.filter(c => c.eventId === ev.id && c.status === 'VALIDO').length;

                    return (
                      <tr key={ev.id}>
                        <td style={{ fontWeight: 600, color: 'var(--sand-100)' }}>{ev.name}</td>
                        <td>
                          <span className={`badge ${ev.status === 'FINALIZADO' ? 'badge-violet' : 'badge-slate'}`}>
                            {ev.status}
                          </span>
                        </td>
                        <td>{ev.durationHours} horas</td>
                        <td>{evRegs.length}</td>
                        <td>
                          <span className="badge badge-emerald" style={{ fontSize: '0.74rem' }}>
                            {checkedInCount} presentes
                          </span>
                        </td>
                        <td>
                          <span className="badge badge-accent" style={{ fontSize: '0.74rem' }}>
                            {certsCount} emitidos
                          </span>
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          {ev.status === 'FINALIZADO' ? (
                            <button
                              className="btn btn-primary btn-sm"
                              onClick={() => {
                                onIssueEventCertificates(ev.id);
                                addToast(`Certificados generados para los ${checkedInCount} asistentes del evento "${ev.name}"`, 'success');
                              }}
                            >
                              <span style={{ width: 14, height: 14 }}><Icon.Award /></span> Generar Certificados (RN-26)
                            </button>
                          ) : (
                            <span style={{ fontSize: '0.78rem', color: 'var(--stone-500)' }}>Requiere evento Finalizado</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ── MODAL: CREAR / EDITAR EVENTO (RN-16, RN-21) ── */}
      {showEventModal && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 600 }}>
            <div className="modal-header">
              <h3 className="modal-title">{editingEvent ? 'Editar Evento' : 'Crear Nuevo Evento'}</h3>
              <button className="btn-close" onClick={() => setShowEventModal(false)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <form onSubmit={handleSaveEventSubmit}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div className="form-group">
                  <label className="form-label">Nombre del Evento</label>
                  <input
                    type="text"
                    required
                    className="input"
                    placeholder="Ej. Taller de Arquitectura Hexagonal y Microservicios"
                    value={eventForm.name}
                    onChange={e => setEventForm({ ...eventForm, name: e.target.value })}
                  />
                </div>

                <div className="form-grid">
                  <div className="form-group">
                    <label className="form-label">Tipo de Evento</label>
                    <select
                      className="select"
                      value={eventForm.type}
                      onChange={e => setEventForm({ ...eventForm, type: e.target.value as TipoEvento })}
                    >
                      <option value="TALLER">Taller</option>
                      <option value="CAPACITACION">Capacitación</option>
                      <option value="CONFERENCIA">Conferencia</option>
                      <option value="SEMINARIO">Seminario</option>
                      <option value="TORNEO">Torneo</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Modalidad</label>
                    <select
                      className="select"
                      value={eventForm.modality}
                      onChange={e => setEventForm({ ...eventForm, modality: e.target.value as Modalidad })}
                    >
                      <option value="PRESENCIAL">Presencial</option>
                      <option value="VIRTUAL">Virtual</option>
                      <option value="HIBRIDO">Híbrido</option>
                    </select>
                  </div>
                </div>

                <div className="form-grid">
                  <div className="form-group">
                    <label className="form-label">Fecha y Hora de Inicio</label>
                    <input
                      type="datetime-local"
                      required
                      className="input"
                      value={eventForm.startAt}
                      onChange={e => setEventForm({ ...eventForm, startAt: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Fecha y Hora de Cierre</label>
                    <input
                      type="datetime-local"
                      required
                      className="input"
                      value={eventForm.endAt}
                      onChange={e => setEventForm({ ...eventForm, endAt: e.target.value })}
                    />
                  </div>
                </div>

                {eventForm.modality !== 'VIRTUAL' && (
                  <div className="form-group">
                    <label className="form-label">Lugar / Salón (Regla RN-21: Control de choques)</label>
                    <input
                      type="text"
                      className="input"
                      placeholder="Ej. Auditorio Central SENA"
                      value={eventForm.location}
                      onChange={e => setEventForm({ ...eventForm, location: e.target.value })}
                    />
                  </div>
                )}

                {eventForm.modality !== 'PRESENCIAL' && (
                  <div className="form-group">
                    <label className="form-label">Enlace de Conexión Virtual</label>
                    <input
                      type="url"
                      className="input"
                      placeholder="https://meet.google.com/xyz-123"
                      value={eventForm.meetingUrl}
                      onChange={e => setEventForm({ ...eventForm, meetingUrl: e.target.value })}
                    />
                  </div>
                )}

                <div className="form-grid">
                  <div className="form-group">
                    <label className="form-label">Aforo Máximo (Cupos)</label>
                    <input
                      type="number"
                      required
                      min="1"
                      className="input"
                      value={eventForm.capacity}
                      onChange={e => setEventForm({ ...eventForm, capacity: parseInt(e.target.value) || 1 })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Duración Certificable (Horas)</label>
                    <input
                      type="number"
                      required
                      min="0.5"
                      step="0.5"
                      className="input"
                      value={eventForm.durationHours}
                      onChange={e => setEventForm({ ...eventForm, durationHours: parseFloat(e.target.value) || 1 })}
                    />
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-ghost" onClick={() => setShowEventModal(false)}>Cancelar</button>
                <button type="submit" className="btn btn-primary">Guardar Evento</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL: APLAZAR EVENTO (RN-16) ── */}
      {postponingEvent && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 500 }}>
            <div className="modal-header">
              <h3 className="modal-title">Aplazar Evento (RN-16)</h3>
              <button className="btn-close" onClick={() => setPostponingEvent(null)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--stone-300)' }}>
                Se moverá el evento <strong>"{postponingEvent.name}"</strong> a una nueva fecha. Las inscripciones actuales se conservarán automáticamente.
              </p>
              <div className="form-group">
                <label className="form-label">Nueva Fecha y Hora de Inicio:</label>
                <input
                  type="datetime-local"
                  required
                  className="input"
                  value={postponeNewStart}
                  onChange={e => setPostponeNewStart(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Nueva Fecha y Hora de Cierre:</label>
                <input
                  type="datetime-local"
                  required
                  className="input"
                  value={postponeNewEnd}
                  onChange={e => setPostponeNewEnd(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Motivo Obligatorio del Aplazamiento:</label>
                <textarea
                  required
                  rows={3}
                  className="input"
                  placeholder="Ej. Solicitud de reprogramación por disponibilidad de expositor internacional."
                  value={postponeReason}
                  onChange={e => setPostponeReason(e.target.value)}
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setPostponingEvent(null)}>Cancelar</button>
              <button
                className="btn btn-primary"
                disabled={!postponeReason.trim()}
                onClick={() => {
                  onPostponeEvent(postponingEvent.id, postponeNewStart, postponeNewEnd, postponeReason);
                  addToast(`Evento aplazado correctamente. Fecha actualizada.`, 'info');
                  setPostponingEvent(null);
                }}
              >
                Confirmar Aplazamiento
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── MODAL: CANCELAR EVENTO (RN-16, RN-23) ── */}
      {cancellingEvent && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 480 }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ color: 'var(--error-400)' }}>Cancelar Evento</h3>
              <button className="btn-close" onClick={() => setCancellingEvent(null)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--sand-200)' }}>
                ¿Estás seguro de cancelar el evento <strong>"{cancellingEvent.name}"</strong>?
                <br /><small style={{ color: 'var(--stone-400)' }}>Regla RN-17: Se liberarán los cupos y los códigos QR de los inscritos quedarán invalidados.</small>
              </p>
              <div className="form-group">
                <label className="form-label">Motivo Obligatorio de Cancelación (RN-23):</label>
                <textarea
                  required
                  rows={3}
                  className="input"
                  placeholder="Ej. Motivos de fuerza mayor o mantenimiento de sede."
                  value={cancelReason}
                  onChange={e => setCancelReason(e.target.value)}
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setCancellingEvent(null)}>Regresar</button>
              <button
                className="btn btn-danger"
                disabled={!cancelReason.trim()}
                onClick={() => {
                  onCancelEvent(cancellingEvent.id, cancelReason);
                  addToast(`Evento cancelado. Se registraron los motivos en auditoría.`, 'warning');
                  setCancellingEvent(null);
                }}
              >
                Confirmar Cancelación
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── MODAL: ASIGNAR MONITORES (RN-15, RN-22) ── */}
      {assigningMonitorsEvent && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 500 }}>
            <div className="modal-header">
              <h3 className="modal-title">Asignación de Monitores al Evento (RN-15)</h3>
              <button className="btn-close" onClick={() => setAssigningMonitorsEvent(null)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--stone-300)' }}>
                Selecciona los monitores autorizados para el control de acceso en <strong>"{assigningMonitorsEvent.name}"</strong> ({assigningMonitorsEvent.startAt.slice(0, 10)}).
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {availableMonitors.map(m => {
                  const isChecked = selectedMonitors.includes(m.email);
                  // RN-22: check if this monitor already has overlapping event
                  const hasOverlap = events.some(other => 
                    other.id !== assigningMonitorsEvent.id &&
                    other.status !== 'CANCELADO' &&
                    other.assignedMonitors?.includes(m.email) &&
                    other.startAt.slice(0, 10) === assigningMonitorsEvent.startAt.slice(0, 10)
                  );

                  return (
                    <label
                      key={m.email}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '12px 14px',
                        background: isChecked ? 'rgba(90,143,179,0.15)' : 'var(--surface-2)',
                        border: `1px solid ${isChecked ? 'var(--accent-400)' : 'var(--glass-border)'}`,
                        borderRadius: 'var(--radius-sm)',
                        cursor: 'pointer',
                        fontSize: '0.85rem'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => {
                            if (isChecked) {
                              setSelectedMonitors(prev => prev.filter(x => x !== m.email));
                            } else {
                              if (hasOverlap) {
                                addToast(`Alerta RN-22: ${m.name} ya está asignado a otro evento en este mismo horario`, 'warning');
                              }
                              setSelectedMonitors(prev => [...prev, m.email]);
                            }
                          }}
                        />
                        <div>
                          <div style={{ fontWeight: 600, color: 'var(--sand-100)' }}>{m.name}</div>
                          <div style={{ fontSize: '0.74rem', color: 'var(--stone-400)' }}>{m.email}</div>
                        </div>
                      </div>
                      {hasOverlap && (
                        <span className="badge badge-amber" style={{ fontSize: '0.7rem' }}>Cruce de Horario</span>
                      )}
                    </label>
                  );
                })}
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setAssigningMonitorsEvent(null)}>Cancelar</button>
              <button
                className="btn btn-primary"
                onClick={() => {
                  onAssignMonitors(assigningMonitorsEvent.id, selectedMonitors);
                  addToast(`Monitores asignados a "${assigningMonitorsEvent.name}"`, 'success');
                  setAssigningMonitorsEvent(null);
                }}
              >
                Guardar Asignación
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── MODAL: NUEVO PARTICIPANTE (RN-02) ── */}
      {showPartModal && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 500 }}>
            <div className="modal-header">
              <h3 className="modal-title">Registrar Nuevo Participante</h3>
              <button className="btn-close" onClick={() => setShowPartModal(false)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <form onSubmit={handleSaveParticipantSubmit}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div className="form-grid">
                  <div className="form-group">
                    <label className="form-label">Tipo Documento</label>
                    <select
                      className="select"
                      value={partForm.documentType}
                      onChange={e => setPartForm({ ...partForm, documentType: e.target.value })}
                    >
                      <option value="CC">Cédula de Ciudadanía (CC)</option>
                      <option value="TI">Tarjeta de Identidad (TI)</option>
                      <option value="CE">Cédula de Extranjería (CE)</option>
                      <option value="PAS">Pasaporte</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Número de Documento (RN-02)</label>
                    <input
                      type="text"
                      required
                      className="input"
                      placeholder="1020304050"
                      value={partForm.documentNumber}
                      onChange={e => setPartForm({ ...partForm, documentNumber: e.target.value })}
                    />
                  </div>
                </div>
                <div className="form-grid">
                  <div className="form-group">
                    <label className="form-label">Nombres</label>
                    <input
                      type="text"
                      required
                      className="input"
                      placeholder="Laura"
                      value={partForm.firstName}
                      onChange={e => setPartForm({ ...partForm, firstName: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Apellidos</label>
                    <input
                      type="text"
                      className="input"
                      placeholder="Gómez"
                      value={partForm.lastName}
                      onChange={e => setPartForm({ ...partForm, lastName: e.target.value })}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">Correo Electrónico</label>
                  <input
                    type="email"
                    required
                    className="input"
                    placeholder="laura.gomez@ejemplo.com"
                    value={partForm.email}
                    onChange={e => setPartForm({ ...partForm, email: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Teléfono Móvil</label>
                  <input
                    type="tel"
                    className="input"
                    placeholder="+57 310 123 4567"
                    value={partForm.phone}
                    onChange={e => setPartForm({ ...partForm, phone: e.target.value })}
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-ghost" onClick={() => setShowPartModal(false)}>Cancelar</button>
                <button type="submit" className="btn btn-primary">Registrar Participante</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL: INSCRIBIR A EVENTO DIRECTAMENTE ── */}
      {showDirectRegModal && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 500 }}>
            <div className="modal-header">
              <h3 className="modal-title">Inscribir Participante a Evento Activo</h3>
              <button className="btn-close" onClick={() => setShowDirectRegModal(false)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <form onSubmit={handleDirectRegistrationSubmit}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div className="form-group">
                  <label className="form-label">Seleccionar Participante:</label>
                  <select
                    required
                    className="select"
                    value={selectedPartId}
                    onChange={e => setSelectedPartId(e.target.value)}
                  >
                    <option value="">-- Elige un participante registrado --</option>
                    {participants.map(p => (
                      <option key={p.id} value={p.id}>
                        {p.firstName} {p.lastName} ({p.documentType} {p.documentNumber})
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Seleccionar Evento Disponible:</label>
                  <select
                    required
                    className="select"
                    value={selectedEventId}
                    onChange={e => setSelectedEventId(e.target.value)}
                  >
                    <option value="">-- Elige un evento con cupos --</option>
                    {events.filter(e => e.status === 'PUBLICADO' || e.status === 'EN_CURSO').map(e => (
                      <option key={e.id} value={e.id}>
                        {e.name} ({e.status}) · Cupos: {e.capacity}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-ghost" onClick={() => setShowDirectRegModal(false)}>Cancelar</button>
                <button type="submit" className="btn btn-primary">Inscribir y Generar QR</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
