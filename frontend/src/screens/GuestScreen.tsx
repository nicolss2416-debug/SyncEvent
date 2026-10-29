import React, { useState } from 'react';
import { Event, Registration, CertificateRecord, Participant, Modalidad, TipoEvento } from '../types';
import { Icon } from '../icons';

interface GuestScreenProps {
  events: Event[];
  registrations: Registration[];
  certificates: CertificateRecord[];
  currentParticipant: Participant;
  onSelfRegister: (eventId: string) => void;
  onSelfCancelRegistration: (regId: string, reason: string) => void;
  addToast: (msg: string, type?: 'success' | 'info' | 'error' | 'warning') => void;
}

export function GuestScreen({
  events,
  registrations,
  certificates,
  currentParticipant,
  onSelfRegister,
  onSelfCancelRegistration,
  addToast,
}: GuestScreenProps) {
  const [activeTab, setActiveTab] = useState<'catalog' | 'registrations' | 'certificates' | 'verify'>('catalog');

  // Filters for catalog
  const [filterModality, setFilterModality] = useState<string>('ALL');
  const [filterType, setFilterType] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Modals
  const [selectedQrReg, setSelectedQrReg] = useState<Registration | null>(null);
  const [cancellingReg, setCancellingReg] = useState<Registration | null>(null);
  const [cancelReason, setCancelReason] = useState<string>('');

  // Certificate Public Verification state
  const [verifyCodeInput, setVerifyCodeInput] = useState<string>('');
  const [verificationResult, setVerificationResult] = useState<{
    found: boolean;
    cert?: CertificateRecord;
  } | null>(null);

  // My registrations
  const myRegistrations = registrations.filter(
    r => r.participantEmail.toLowerCase() === currentParticipant.email.toLowerCase() && r.status === 'ACTIVA'
  );

  // My certificates
  const myCertificates = certificates.filter(
    c => c.participantName.toLowerCase() === `${currentParticipant.firstName} ${currentParticipant.lastName}`.toLowerCase() ||
         c.participantDocument === currentParticipant.documentNumber
  );

  // Catalog events (Publicados o En Curso)
  const catalogEvents = events.filter(e => {
    const isPublic = e.status === 'PUBLICADO' || e.status === 'EN_CURSO';
    const matchMod = filterModality === 'ALL' || e.modality === filterModality;
    const matchType = filterType === 'ALL' || e.type === filterType;
    const matchSearch = searchQuery === '' ||
      `${e.name} ${e.description || ''} ${e.location || ''}`.toLowerCase().includes(searchQuery.toLowerCase());
    return isPublic && matchMod && matchType && matchSearch;
  });

  const handleRegisterClick = (ev: Event) => {
    // RN-24: Check if participant has active fines
    if (currentParticipant.activeFines && currentParticipant.activeFines > 0) {
      addToast('Regla RN-24: Tienes multas activas por inasistencias anteriores. No puedes inscribirte hasta que el Administrador las resuelva.', 'error');
      return;
    }

    // Check if already registered
    const alreadyReg = myRegistrations.some(r => r.eventId === ev.id);
    if (alreadyReg) {
      addToast('Ya te encuentras formalmente inscrito en este evento', 'info');
      return;
    }

    // RN-05: Check capacity
    const currentRegs = registrations.filter(r => r.eventId === ev.id && r.status === 'ACTIVA').length;
    if (currentRegs >= ev.capacity) {
      addToast('Regla RN-05: Aforo máximo alcanzado para este evento. No hay cupos disponibles.', 'error');
      return;
    }

    // RN-20: Check schedule collision with other events I am registered for
    const collision = myRegistrations.some(r => {
      const other = events.find(o => o.id === r.eventId);
      return other && other.id !== ev.id && other.startAt.slice(0, 10) === ev.startAt.slice(0, 10);
    });
    if (collision) {
      addToast('Regla RN-20: Tienes otro evento programado en el mismo horario/fecha. Operación rechazada.', 'error');
      return;
    }

    onSelfRegister(ev.id);
    addToast(`¡Inscripción confirmada para "${ev.name}"! Tu pase QR está disponible en "Mis Inscripciones".`, 'success');
  };

  const handleConfirmCancel = () => {
    if (!cancellingReg) return;
    if (!cancelReason.trim()) {
      addToast('El motivo de cancelación es obligatorio (RN-23)', 'warning');
      return;
    }

    onSelfCancelRegistration(cancellingReg.id, cancelReason);
    addToast(`Inscripción cancelada. Se liberó el cupo y el código QR fue invalidado (RN-17).`, 'info');
    setCancellingReg(null);
    setCancelReason('');
  };

  const handleVerifyCertificate = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = verifyCodeInput.trim().toUpperCase();
    if (!clean) return;

    const cert = certificates.find(c => c.code.toUpperCase() === clean);
    if (cert) {
      setVerificationResult({ found: true, cert });
      addToast(cert.status === 'VALIDO' ? 'Certificado auténtico y válido' : 'Certificado REVOCADO por el Administrador', cert.status === 'VALIDO' ? 'success' : 'warning');
    } else {
      setVerificationResult({ found: false });
      addToast('Código no encontrado en el registro nacional de SyncEvent', 'error');
    }
  };

  return (
    <div className="actor-screen guest-screen animate-fade-in">
      {/* ── Actor Banner ── */}
      <div className="actor-header-banner" style={{ background: 'linear-gradient(135deg, rgba(179,157,125,0.18), rgba(60,45,30,0.3))', border: '1px solid rgba(179,157,125,0.25)', borderRadius: 'var(--radius-lg)', padding: '24px 28px', marginBottom: 28 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{ width: 52, height: 52, borderRadius: 14, background: 'linear-gradient(135deg,#b39d7d,#7d6754)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', boxShadow: '0 8px 24px rgba(125,103,84,0.4)' }}>
              <span style={{ width: 26, height: 26 }}><Icon.User /></span>
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <h2 style={{ margin: 0, fontSize: '1.4rem', color: 'var(--sand-100)', fontFamily: 'var(--font-display)' }}>
                  Portal del Participante
                </h2>
                <span className="badge badge-accent" style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: '0.72rem' }}>
                  {currentParticipant.firstName} {currentParticipant.lastName}
                </span>
              </div>
              <p style={{ margin: '4px 0 0', fontSize: '0.86rem', color: 'var(--stone-400)' }}>
                Exploración de eventos, auto-inscripción sin cruces de horario (RN-20), pases QR digitales y descarga de certificados legítimos.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <button className={`btn btn-sm ${activeTab === 'catalog' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('catalog')}>
              <span style={{ width: 14, height: 14 }}><Icon.Calendar /></span> Catálogo ({catalogEvents.length})
            </button>
            <button className={`btn btn-sm ${activeTab === 'registrations' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('registrations')}>
              <span style={{ width: 14, height: 14 }}><Icon.ClipboardList /></span> Mis Inscripciones ({myRegistrations.length})
            </button>
            <button className={`btn btn-sm ${activeTab === 'certificates' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('certificates')}>
              <span style={{ width: 14, height: 14 }}><Icon.Award /></span> Mis Certificados ({myCertificates.length})
            </button>
            <button className={`btn btn-sm ${activeTab === 'verify' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('verify')}>
              <span style={{ width: 14, height: 14 }}><Icon.Shield /></span> Verificador Público
            </button>
          </div>
        </div>
      </div>

      {/* Warning if participant has fines */}
      {currentParticipant.activeFines && currentParticipant.activeFines > 0 ? (
        <div style={{ background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 'var(--radius-md)', padding: '16px 20px', marginBottom: 24, display: 'flex', alignItems: 'center', gap: 14 }}>
          <span style={{ width: 24, height: 24, color: 'var(--error-400)' }}><Icon.AlertTriangle /></span>
          <div>
            <strong style={{ color: 'var(--error-400)', fontSize: '0.9rem' }}>Sanción Activa por Inasistencias (Regla RN-24):</strong>
            <p style={{ margin: '2px 0 0', fontSize: '0.84rem', color: 'var(--stone-300)' }}>
              Has acumulado inasistencias en eventos finalizados. Tu cuenta está temporalmente inhabilitada para nuevas inscripciones hasta que el Administrador resuelva la sanción.
            </p>
          </div>
        </div>
      ) : null}

      {/* ── TAB 1: CATÁLOGO DE EVENTOS PÚBLICOS ── */}
      {activeTab === 'catalog' && (
        <div className="tab-pane animate-fade-in">
          {/* Filters Bar */}
          <div className="card" style={{ padding: '16px 20px', marginBottom: 24 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--stone-300)', fontWeight: 500 }}>Filtros:</span>
                <select
                  className="select"
                  style={{ width: 'auto', fontSize: '0.82rem' }}
                  value={filterModality}
                  onChange={e => setFilterModality(e.target.value)}
                >
                  <option value="ALL">Todas las Modalidades</option>
                  <option value="PRESENCIAL">Presencial</option>
                  <option value="VIRTUAL">Virtual</option>
                  <option value="HIBRIDO">Híbrido</option>
                </select>

                <select
                  className="select"
                  style={{ width: 'auto', fontSize: '0.82rem' }}
                  value={filterType}
                  onChange={e => setFilterType(e.target.value)}
                >
                  <option value="ALL">Todos los Tipos</option>
                  <option value="CAPACITACION">Capacitación</option>
                  <option value="TALLER">Taller</option>
                  <option value="CONFERENCIA">Conferencia</option>
                  <option value="SEMINARIO">Seminario</option>
                  <option value="TORNEO">Torneo</option>
                </select>
              </div>

              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  placeholder="Buscar en el catálogo..."
                  className="input"
                  style={{ fontSize: '0.82rem', paddingLeft: 34, width: 240 }}
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                />
                <span style={{ position: 'absolute', left: 10, top: 10, width: 16, height: 16, color: 'var(--stone-500)' }}><Icon.Search /></span>
              </div>
            </div>
          </div>

          {/* Events Grid */}
          <div className="events-grid">
            {catalogEvents.map(ev => {
              const evRegs = registrations.filter(r => r.eventId === ev.id && r.status === 'ACTIVA');
              const isRegistered = myRegistrations.some(r => r.eventId === ev.id);
              const availableSpots = Math.max(0, ev.capacity - evRegs.length);
              const isFull = availableSpots === 0;

              return (
                <div key={ev.id} className="event-card animate-fade-in" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                      <span className="badge badge-accent" style={{ fontSize: '0.74rem' }}>{ev.type}</span>
                      <span className={`badge ${ev.modality === 'PRESENCIAL' ? 'badge-emerald' : ev.modality === 'VIRTUAL' ? 'badge-blue' : 'badge-violet'}`} style={{ fontSize: '0.74rem' }}>
                        {ev.modality}
                      </span>
                    </div>

                    <h3 className="event-title" style={{ fontSize: '1.08rem', margin: '0 0 8px' }}>{ev.name}</h3>
                    <p style={{ fontSize: '0.83rem', color: 'var(--stone-400)', margin: '0 0 14px', lineHeight: 1.5 }}>
                      {ev.description || 'Sin descripción detallada disponible.'}
                    </p>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, fontSize: '0.8rem', color: 'var(--stone-300)', marginBottom: 16 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ width: 14, height: 14, color: 'var(--accent-400)' }}><Icon.Calendar /></span>
                        {ev.startAt.replace('T', ' ')}
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ width: 14, height: 14, color: 'var(--accent-400)' }}><Icon.Clock /></span>
                        {ev.durationHours} horas certificadas
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ width: 14, height: 14, color: 'var(--accent-400)' }}><Icon.MapPin /></span>
                        {ev.modality === 'PRESENCIAL' ? (ev.location || 'Sede Central') : (ev.meetingUrl || 'Sesión Remota')}
                      </div>
                    </div>
                  </div>

                  <div>
                    {/* Capacity Indicator */}
                    <div style={{ marginBottom: 14 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.76rem', color: 'var(--stone-400)', marginBottom: 4 }}>
                        <span>Cupos disponibles:</span>
                        <strong style={{ color: isFull ? 'var(--error-400)' : 'var(--success-400)' }}>
                          {isFull ? 'Aforo completo' : `${availableSpots} de ${ev.capacity}`}
                        </strong>
                      </div>
                      <div className="progress-bar">
                        <div
                          className="progress-bar-fill"
                          style={{
                            width: `${Math.round(((ev.capacity - availableSpots) / ev.capacity) * 100)}%`,
                            background: isFull ? 'var(--error-500)' : 'var(--accent-400)'
                          }}
                        />
                      </div>
                    </div>

                    {/* Registration Action */}
                    {isRegistered ? (
                      <button className="btn btn-secondary w-full" disabled style={{ opacity: 0.85, cursor: 'default' }}>
                        <span style={{ width: 16, height: 16, color: 'var(--success-400)' }}><Icon.Check /></span> Ya estás inscrito
                      </button>
                    ) : (
                      <button
                        className="btn btn-primary w-full"
                        disabled={isFull}
                        onClick={() => handleRegisterClick(ev)}
                      >
                        <span style={{ width: 16, height: 16 }}><Icon.ClipboardList /></span>
                        {isFull ? 'Aforo Agotado' : 'Inscribirme Gratis'}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {catalogEvents.length === 0 && (
            <div className="card" style={{ textAlign: 'center', padding: 50, color: 'var(--stone-400)' }}>
              No se encontraron eventos disponibles con los filtros aplicados.
            </div>
          )}
        </div>
      )}

      {/* ── TAB 2: MIS INSCRIPCIONES Y PASE QR DIGITAL (HU-010, HU-011) ── */}
      {activeTab === 'registrations' && (
        <div className="tab-pane animate-fade-in">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Mis Pases de Entrada y Códigos QR (HU-010)</h3>
              <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                Presenta este código QR al Monitor de acceso al llegar al evento. Puedes cancelar tu inscripción voluntariamente para liberar el cupo (RN-23).
              </p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 20 }}>
              {myRegistrations.map(reg => {
                const ev = events.find(e => e.id === reg.eventId);
                return (
                  <div
                    key={reg.id}
                    style={{
                      background: 'var(--surface-2)',
                      border: '1px solid var(--glass-border)',
                      borderRadius: 'var(--radius-md)',
                      padding: 20,
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      boxShadow: 'var(--shadow-sm)'
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
                        <span className={`badge ${reg.checkedIn ? 'badge-emerald' : 'badge-blue'}`}>
                          {reg.checkedIn ? 'Asistencia Confirmada' : 'Inscripción Vigente'}
                        </span>
                        <span style={{ fontSize: '0.74rem', color: 'var(--stone-500)' }}>{reg.createdAt}</span>
                      </div>

                      <h4 style={{ margin: '0 0 8px', color: 'var(--sand-100)', fontSize: '1.05rem' }}>{reg.eventName}</h4>
                      {ev && (
                        <div style={{ fontSize: '0.8rem', color: 'var(--stone-400)', marginBottom: 14 }}>
                          <div>{ev.startAt.replace('T', ' ')}</div>
                          <div>{ev.modality === 'PRESENCIAL' ? (ev.location || 'Sede presencial') : 'Virtual'}</div>
                        </div>
                      )}

                      {/* Mini QR badge preview */}
                      <div
                        style={{
                          background: '#fff',
                          color: '#000',
                          padding: 12,
                          borderRadius: 'var(--radius-sm)',
                          display: 'flex',
                          alignItems: 'center',
                          gap: 12,
                          marginBottom: 16
                        }}
                      >
                        <div style={{ width: 44, height: 44 }}>
                          <Icon.QrCode />
                        </div>
                        <div>
                          <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#555', fontWeight: 600 }}>Pase QR de Acceso</div>
                          <div style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: '0.92rem', color: '#111' }}>{reg.qrToken}</div>
                        </div>
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: 8 }}>
                      <button
                        className="btn btn-secondary btn-sm"
                        style={{ flex: 1 }}
                        onClick={() => setSelectedQrReg(reg)}
                      >
                        <span style={{ width: 14, height: 14 }}><Icon.QrCode /></span> Ver Credencial
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        style={{ color: 'var(--error-400)' }}
                        onClick={() => {
                          setCancellingReg(reg);
                          setCancelReason('');
                        }}
                      >
                        Cancelar
                      </button>
                    </div>
                  </div>
                );
              })}

              {myRegistrations.length === 0 && (
                <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: 40, color: 'var(--stone-400)' }}>
                  Aún no estás inscrito en ningún evento. Explora el catálogo para unirte.
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── TAB 3: MIS CERTIFICADOS OFICIALES (RN-08) ── */}
      {activeTab === 'certificates' && (
        <div className="tab-pane animate-fade-in">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Mis Certificados Oficiales Emitidos (RN-08)</h3>
              <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                Certificados con firma digital, marca de agua e identificador único e impredecible (RN-09).
              </p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: 20 }}>
              {myCertificates.map(cert => (
                <div
                  key={cert.id}
                  style={{
                    background: 'linear-gradient(135deg, rgba(38,34,29,0.95), rgba(26,23,20,0.98))',
                    border: '1px solid rgba(197,178,153,0.25)',
                    borderRadius: 'var(--radius-md)',
                    padding: 22,
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    boxShadow: 'var(--shadow-md)',
                    position: 'relative',
                    overflow: 'hidden'
                  }}
                >
                  <div style={{ position: 'absolute', right: -15, top: -15, width: 80, height: 80, opacity: 0.08, pointerEvents: 'none' }}>
                    <Icon.Award />
                  </div>

                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                      <span className="badge badge-accent">Certificado de Aprobación</span>
                      <span className="badge badge-emerald" style={{ fontSize: '0.72rem' }}>Oficial</span>
                    </div>

                    <h4 style={{ margin: '0 0 6px', color: 'var(--sand-100)', fontSize: '1.1rem' }}>{cert.eventName}</h4>
                    <p style={{ margin: '0 0 12px', fontSize: '0.82rem', color: 'var(--stone-400)' }}>
                      Otorgado a: <strong>{cert.participantName}</strong>
                    </p>

                    <div style={{ background: 'var(--surface-0)', padding: 12, borderRadius: 'var(--radius-sm)', fontSize: '0.78rem', color: 'var(--stone-300)', marginBottom: 16 }}>
                      <div><strong>Código Único:</strong> <code style={{ color: 'var(--accent-400)' }}>{cert.code}</code></div>
                      <div><strong>Intensidad:</strong> {cert.durationHours} horas académicas</div>
                      <div><strong>Fecha de Emisión:</strong> {cert.issuedAt}</div>
                    </div>
                  </div>

                  <button
                    className="btn btn-primary btn-sm w-full"
                    onClick={() => addToast(`Descargando certificado oficial ${cert.code}...`, 'success')}
                  >
                    <span style={{ width: 14, height: 14 }}><Icon.Download /></span> Descargar en PDF
                  </button>
                </div>
              ))}

              {myCertificates.length === 0 && (
                <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: 40, color: 'var(--stone-400)' }}>
                  Aún no tienes certificados emitidos. Para obtenerlos, asiste a tus eventos y el Operador los generará al finalizar.
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── TAB 4: VERIFICADOR PÚBLICO (RN-09, RN-10) ── */}
      {activeTab === 'verify' && (
        <div className="tab-pane animate-fade-in">
          <div className="card" style={{ maxWidth: 640, margin: '0 auto' }}>
            <div className="card-header" style={{ textAlign: 'center' }}>
              <div style={{ width: 50, height: 50, borderRadius: '50%', background: 'rgba(90,143,179,0.15)', color: 'var(--accent-400)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <span style={{ width: 26, height: 26 }}><Icon.Shield /></span>
              </div>
              <h3 className="card-title" style={{ margin: 0 }}>Portal Público de Verificación de Autenticidad</h3>
              <p style={{ margin: '6px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                Ingresa el código único del certificado para verificar su legitimidad ante la entidad emisora (Regla RN-09).
              </p>
            </div>

            <form onSubmit={handleVerifyCertificate} style={{ marginTop: 10 }}>
              <div className="form-group" style={{ marginBottom: 16 }}>
                <label className="form-label">Código de Verificación:</label>
                <div style={{ display: 'flex', gap: 10 }}>
                  <input
                    type="text"
                    required
                    className="input"
                    placeholder="Ej. SYNC-CERT-2026-X89F"
                    style={{ fontFamily: 'monospace', textTransform: 'uppercase', letterSpacing: '0.05em' }}
                    value={verifyCodeInput}
                    onChange={e => setVerifyCodeInput(e.target.value)}
                  />
                  <button type="submit" className="btn btn-primary">
                    Verificar
                  </button>
                </div>
              </div>
            </form>

            {verificationResult && (
              <div style={{ marginTop: 20 }}>
                {verificationResult.found && verificationResult.cert ? (
                  <div
                    style={{
                      background: verificationResult.cert.status === 'VALIDO' ? 'rgba(34,197,94,0.08)' : 'rgba(239,68,68,0.08)',
                      border: `1px solid ${verificationResult.cert.status === 'VALIDO' ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)'}`,
                      borderRadius: 'var(--radius-md)',
                      padding: 20
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                      <span style={{ width: 20, height: 20, color: verificationResult.cert.status === 'VALIDO' ? 'var(--success-400)' : 'var(--error-400)' }}>
                        {verificationResult.cert.status === 'VALIDO' ? <Icon.CheckCircle /> : <Icon.Ban />}
                      </span>
                      <strong style={{ color: verificationResult.cert.status === 'VALIDO' ? 'var(--success-400)' : 'var(--error-400)', fontSize: '0.95rem' }}>
                        {verificationResult.cert.status === 'VALIDO' ? 'Certificado Oficial Válido y Vigente' : 'Certificado REVOCADO (Regla RN-10)'}
                      </strong>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, fontSize: '0.84rem', color: 'var(--sand-200)' }}>
                      <div><strong>Participante Titular:</strong> {verificationResult.cert.participantName}</div>
                      <div><strong>Documento Identidad:</strong> {verificationResult.cert.participantDocument}</div>
                      <div><strong>Evento Acreditado:</strong> {verificationResult.cert.eventName}</div>
                      <div><strong>Duración Certificada:</strong> {verificationResult.cert.durationHours} horas</div>
                      <div><strong>Fecha de Expedición:</strong> {verificationResult.cert.issuedAt}</div>
                      {verificationResult.cert.status === 'REVOCADO' && (
                        <div style={{ color: 'var(--error-400)', marginTop: 8, padding: 10, background: 'rgba(239,68,68,0.1)', borderRadius: 'var(--radius-xs)' }}>
                          <strong>Motivo de Revocación:</strong> {verificationResult.cert.revocationReason || 'Anulado por autoridad académica'}
                        </div>
                      )}
                    </div>
                  </div>
                ) : (
                  <div style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 'var(--radius-md)', padding: 20, textAlign: 'center' }}>
                    <span style={{ width: 32, height: 32, color: 'var(--error-400)', display: 'inline-block', marginBottom: 8 }}><Icon.AlertTriangle /></span>
                    <div style={{ color: 'var(--error-400)', fontWeight: 600 }}>Certificado No Encontrado</div>
                    <p style={{ margin: '4px 0 0', fontSize: '0.82rem', color: 'var(--stone-400)' }}>
                      El código ingresado no corresponde a ningún certificado legítimo registrado en la plataforma.
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── MODAL: PASE QR COMPLETO DE ACCESO ── */}
      {selectedQrReg && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 440, textAlign: 'center' }}>
            <div className="modal-header">
              <h3 className="modal-title">Credencial Digital de Acceso</h3>
              <button className="btn-close" onClick={() => setSelectedQrReg(null)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14 }}>
              <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--sand-200)' }}>
                {selectedQrReg.eventName}
              </p>

              {/* Big QR Display */}
              <div
                style={{
                  background: '#ffffff',
                  padding: 24,
                  borderRadius: 'var(--radius-md)',
                  boxShadow: '0 8px 30px rgba(0,0,0,0.5)',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 12
                }}
              >
                <div style={{ width: 150, height: 150, color: '#000' }}>
                  <Icon.QrCode />
                </div>
                <div style={{ fontFamily: 'monospace', fontWeight: 800, fontSize: '1.25rem', color: '#111', letterSpacing: '0.08em' }}>
                  {selectedQrReg.qrToken}
                </div>
                <span style={{ fontSize: '0.72rem', color: '#666' }}>Válido exclusivamente para check-in oficial</span>
              </div>

              <div style={{ fontSize: '0.82rem', color: 'var(--stone-400)' }}>
                Titular: <strong>{selectedQrReg.participantName}</strong> ({selectedQrReg.participantEmail})
              </div>
            </div>
            <div className="modal-footer" style={{ justifyContent: 'center' }}>
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  navigator.clipboard?.writeText(selectedQrReg.qrToken);
                  addToast(`Código ${selectedQrReg.qrToken} copiado al portapapeles`, 'success');
                }}
              >
                <span style={{ width: 14, height: 14 }}><Icon.Copy /></span> Copiar Código
              </button>
              <button
                className="btn btn-primary btn-sm"
                onClick={() => {
                  addToast('Credencial QR guardada en imágenes', 'success');
                  setSelectedQrReg(null);
                }}
              >
                <span style={{ width: 14, height: 14 }}><Icon.Download /></span> Guardar Credencial
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── MODAL: AUTO-CANCELAR INSCRIPCIÓN (RN-17, RN-23) ── */}
      {cancellingReg && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 460 }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ color: 'var(--error-400)' }}>Cancelar mi Inscripción</h3>
              <button className="btn-close" onClick={() => setCancellingReg(null)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--sand-200)' }}>
                Regla RN-23: Como invitado puedes cancelar tus propias inscripciones sin requerir aprobación. Esto liberará el cupo e invalidará tu código QR (RN-17).
              </p>
              <div className="form-group">
                <label className="form-label">Motivo Obligatorio de Cancelación:</label>
                <textarea
                  required
                  rows={3}
                  className="input"
                  placeholder="Ej. Cruce de compromisos laborales o académicos."
                  value={cancelReason}
                  onChange={e => setCancelReason(e.target.value)}
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setCancellingReg(null)}>Regresar</button>
              <button
                className="btn btn-danger"
                disabled={!cancelReason.trim()}
                onClick={handleConfirmCancel}
              >
                Confirmar y Liberar Cupo
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
