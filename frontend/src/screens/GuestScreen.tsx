import React, { useState } from 'react';
import { Event, Registration, CertificateRecord, Participant, Modalidad, TipoEvento } from '../types/types';
import { Icon } from '../components/icons';
import { API_BASE_URL } from '../config/config';

interface GuestScreenProps {
  events: Event[];
  registrations: Registration[];
  certificates: CertificateRecord[];
  currentParticipant: Participant | null;
  onSelfRegister: (eventId: string) => Promise<boolean>;
  onSelfCancelRegistration: (regId: string, reason: string) => Promise<boolean>;
  onCompleteProfile: (profile: {
    documentType: string;
    documentNumber: string;
    firstName: string;
    lastName: string;
    phone: string;
  }) => Promise<boolean>;
  onDownloadCertificate: (certificateId: string) => void;
  addToast: (msg: string, type?: 'success' | 'info' | 'error' | 'warning') => void;
}

export function GuestScreen({
  events,
  registrations,
  certificates,
  currentParticipant,
  onSelfRegister,
  onSelfCancelRegistration,
  onCompleteProfile,
  onDownloadCertificate,
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
  const [profileForm, setProfileForm] = useState({
    documentType: 'CC', documentNumber: '', firstName: '', lastName: '', phone: '',
  });
  const [isSavingProfile, setIsSavingProfile] = useState(false);

  // Certificate Public Verification state
  const [verifyCodeInput, setVerifyCodeInput] = useState<string>('');
  const [verificationResult, setVerificationResult] = useState<{
    found: boolean;
    cert?: CertificateRecord;
  } | null>(null);

  // My registrations
  const myRegistrations = currentParticipant
    ? registrations.filter(r => r.participantId === currentParticipant.id && r.status === 'ACTIVA')
    : [];

  // My certificates
  const myCertificates = certificates;

  // Catalog events (Publicados o En Curso)
  const catalogEvents = events.filter(e => {
    const isPublic = e.status === 'PUBLICADO' || e.status === 'EN_CURSO';
    const matchMod = filterModality === 'ALL' || e.modality === filterModality;
    const matchType = filterType === 'ALL' || e.type === filterType;
    const matchSearch = searchQuery === '' ||
      `${e.name} ${e.description || ''} ${e.location || ''}`.toLowerCase().includes(searchQuery.toLowerCase());
    return isPublic && matchMod && matchType && matchSearch;
  });

  const handleRegisterClick = async (ev: Event) => {
    if (!currentParticipant) {
      addToast('Tu cuenta aún no está vinculada a una ficha de participante. Contacta al Administrador.', 'error');
      return;
    }
    // RN-24: Check if participant has active fines
    if (currentParticipant.activeFines && currentParticipant.activeFines > 0) {
      addToast('Tienes multas pendientes y no puedes inscribirte hasta que se resuelvan.', 'error');
      return;
    }

    // Check if already registered
    const alreadyReg = myRegistrations.some(r => r.eventId === ev.id);
    if (alreadyReg) {
      addToast('Ya te encuentras formalmente inscrito en este evento', 'info');
      return;
    }

    // RN-05: Check capacity
    const currentRegs = Math.max(
      ev.registered ?? 0,
      registrations.filter(r => r.eventId === ev.id && r.status === 'ACTIVA').length,
    );
    if (currentRegs >= ev.capacity) {
      addToast('El evento alcanzó su aforo máximo y no tiene cupos disponibles.', 'error');
      return;
    }

    if (!await onSelfRegister(ev.id)) return;
    addToast(`¡Inscripción confirmada para "${ev.name}"! Tu pase QR está disponible en "Mis Inscripciones".`, 'success');
  };

  const handleConfirmCancel = async () => {
    if (!cancellingReg) return;
    if (!cancelReason.trim()) {
      addToast('Indica el motivo de la cancelación.', 'warning');
      return;
    }

    if (!await onSelfCancelRegistration(cancellingReg.id, cancelReason)) return;
    addToast('Inscripción cancelada. El cupo quedó disponible nuevamente.', 'info');
    setCancellingReg(null);
    setCancelReason('');
  };

  const handleCompleteProfile = async (event: React.FormEvent) => {
    event.preventDefault();
    const cleanPhone = profileForm.phone.replace(/\D/g, '');
    if (cleanPhone.length !== 10) {
      addToast('El número de celular debe tener exactamente 10 dígitos numéricos.', 'warning');
      return;
    }
    const cleanDoc = profileForm.documentType === 'CC' || profileForm.documentType === 'TI'
      ? profileForm.documentNumber.replace(/\D/g, '')
      : profileForm.documentNumber.replace(/[.,]/g, '').trim();
    if ((profileForm.documentType === 'CC' || profileForm.documentType === 'TI') && (cleanDoc.length < 6 || cleanDoc.length > 10)) {
      addToast('La cédula de ciudadanía debe contener entre 6 y 10 dígitos numéricos (sin puntos ni comas).', 'warning');
      return;
    }
    setIsSavingProfile(true);
    try {
      if (await onCompleteProfile({ ...profileForm, phone: cleanPhone, documentNumber: cleanDoc })) {
        addToast('Perfil completado. Ya puedes usar las funciones de Invitado.', 'success');
      }
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleVerifyCertificate = async (e: React.FormEvent) => {
    e.preventDefault();
    const clean = verifyCodeInput.trim().toUpperCase();
    if (!clean) return;

    try {
      const response = await fetch(`${API_BASE_URL}/api/public/certificates/${encodeURIComponent(clean)}`);
      const data = await response.json();
      const found = data.status !== 'NOT_FOUND';
      const matchingEvent = events.find(ev => ev.name === data.eventName);
      const fallbackHours = data.eventStartAt && data.eventEndAt
        ? Math.max(0, Math.round(((Date.parse(data.eventEndAt) - Date.parse(data.eventStartAt)) / 3_600_000) * 10) / 10)
        : 0;
      const docDisplay = data.participantDocumentNumber
        ? `${data.participantDocumentType ? `${data.participantDocumentType} ` : ''}${data.participantDocumentNumber}`
        : '';
      const certificate: CertificateRecord | undefined = found ? {
        id: clean,
        code: data.verificationCode,
        eventId: matchingEvent?.id || '',
        eventName: data.eventName || '',
        participantName: data.participantName || '',
        participantDocument: docDisplay,
        issuedAt: data.issuedAt || '',
        durationHours: matchingEvent?.durationHours || fallbackHours,
        status: data.status === 'VALID' ? 'VALIDO' : 'REVOCADO',
        revocationReason: data.status === 'REVOKED' || data.status === 'COUNTERFEIT' ? data.message : undefined,
      } : undefined;
      setVerificationResult({ found, cert: certificate });
      addToast(data.message || (data.active ? 'Certificado válido y vigente.' : 'El certificado no es válido.'),
        data.status === 'VALID' ? 'success' : 'warning');
    } catch {
      setVerificationResult({ found: false });
      addToast('No se pudo consultar el servicio de verificación. Inténtalo nuevamente.', 'error');
    }
  };

  return (
    <div className="actor-screen guest-screen animate-fade-in">
      {/* ── Actor Banner ── */}
      <div className="actor-header-banner" style={{ background: 'var(--banner-bg)', border: '1px solid rgba(186,160,130,0.35)', borderRadius: 'var(--radius-lg)', padding: '24px 28px', marginBottom: 28, boxShadow: 'var(--shadow-sm)' }}>
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
                  {currentParticipant ? `${currentParticipant.firstName} ${currentParticipant.lastName}` : 'Perfil pendiente'}
                </span>
              </div>
              <p style={{ margin: '4px 0 0', fontSize: '0.86rem', color: 'var(--stone-400)' }}>
                Explora eventos, gestiona tus inscripciones y consulta tus certificados.
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

      {!currentParticipant && (
        <div className="card" style={{ marginBottom: 24, padding: '24px 28px', border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-lg)' }}>
          <h3 className="card-title" style={{ margin: '0 0 6px', fontSize: '1.1rem' }}>
            Completa tu perfil para inscribirte
          </h3>
          <p style={{ color: 'var(--stone-400)', margin: '0 0 20px', fontSize: '0.86rem' }}>
            Necesitamos estos datos para crear tu ficha de participante y asociarla a tu cuenta.
          </p>

          <form onSubmit={handleCompleteProfile}>
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
              gap: '16px 20px',
              marginBottom: 20
            }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Nombres</label>
                <input required maxLength={100} className="input" placeholder="Nombres" value={profileForm.firstName}
                  onChange={e => setProfileForm({ ...profileForm, firstName: e.target.value })} />
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Apellidos</label>
                <input required maxLength={100} className="input" placeholder="Apellidos" value={profileForm.lastName}
                  onChange={e => setProfileForm({ ...profileForm, lastName: e.target.value })} />
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Tipo de documento</label>
                <select className="select" value={profileForm.documentType}
                  onChange={e => {
                    const newType = e.target.value;
                    setProfileForm({
                      ...profileForm,
                      documentType: newType,
                      documentNumber: newType === 'CC' || newType === 'TI'
                        ? profileForm.documentNumber.replace(/\D/g, '').slice(0, 10)
                        : profileForm.documentNumber.slice(0, 20)
                    });
                  }}>
                  <option value="CC">Cédula de ciudadanía</option>
                  <option value="CE">Cédula de extranjería</option>
                  <option value="TI">Tarjeta de identidad</option>
                  <option value="PASAPORTE">Pasaporte</option>
                </select>
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">
                  Número de documento {profileForm.documentType === 'CC' ? '(solo números, hasta 10 dígitos)' : ''}
                </label>
                <input
                  required
                  maxLength={profileForm.documentType === 'CC' || profileForm.documentType === 'TI' ? 10 : 20}
                  inputMode={profileForm.documentType === 'CC' || profileForm.documentType === 'TI' ? 'numeric' : 'text'}
                  placeholder={profileForm.documentType === 'CC' ? 'Ej. 1025664889' : 'Número de documento'}
                  className="input"
                  value={profileForm.documentNumber}
                  onChange={e => {
                    if (profileForm.documentType === 'CC' || profileForm.documentType === 'TI') {
                      setProfileForm({ ...profileForm, documentNumber: e.target.value.replace(/\D/g, '').slice(0, 10) });
                    } else {
                      setProfileForm({ ...profileForm, documentNumber: e.target.value.replace(/[.,]/g, '').slice(0, 20) });
                    }
                  }}
                />
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Número de celular (10 dígitos)</label>
                <input
                  required
                  maxLength={10}
                  type="tel"
                  inputMode="numeric"
                  placeholder="Ej. 3001234567"
                  className="input"
                  value={profileForm.phone}
                  onChange={e => setProfileForm({ ...profileForm, phone: e.target.value.replace(/\D/g, '').slice(0, 10) })}
                />
              </div>
            </div>

            <button type="submit" className="btn btn-primary" disabled={isSavingProfile} style={{ minWidth: 160 }}>
              {isSavingProfile ? 'Guardando…' : 'Guardar perfil'}
            </button>
          </form>
        </div>
      )}

      {/* Warning if participant has fines */}
      {currentParticipant?.activeFines && currentParticipant.activeFines > 0 ? (
        <div style={{ background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 'var(--radius-md)', padding: '16px 20px', marginBottom: 24, display: 'flex', alignItems: 'center', gap: 14 }}>
          <span style={{ width: 24, height: 24, color: 'var(--error-400)' }}><Icon.AlertTriangle /></span>
          <div>
            <strong style={{ color: 'var(--error-400)', fontSize: '0.9rem' }}>Multa pendiente por inasistencias:</strong>
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
              const occupiedCount = Math.max(ev.registered ?? 0, evRegs.length);
              const isRegistered = myRegistrations.some(r => r.eventId === ev.id);
              const availableSpots = Math.max(0, ev.capacity - occupiedCount);
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
              <h3 className="card-title">Mis inscripciones</h3>
              <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                Consulta el estado de tus inscripciones y cancela las que ya no puedas atender.
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
              <h3 className="card-title">Mis certificados</h3>
              <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                Consulta y descarga los certificados emitidos a tu nombre.
              </p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: 20 }}>
              {myCertificates.map(cert => (
                <div
                  key={cert.id}
                  style={{
                    background: 'var(--banner-bg)',
                    border: '1.5px solid rgba(186, 140, 90, 0.35)',
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
                    onClick={() => onDownloadCertificate(cert.id)}
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
          <div className="card" style={{ maxWidth: 640, margin: '0 auto', boxShadow: 'var(--shadow-sm)' }}>
            {/* Header: Escudo a la izquierda, título y subtítulo alineados a su derecha */}
            <div
              className="card-header"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 16,
                padding: '20px 24px',
                borderBottom: '1px solid var(--glass-border)'
              }}
            >
              <div
                style={{
                  width: 48,
                  height: 48,
                  borderRadius: '50%',
                  background: 'rgba(90, 143, 179, 0.14)',
                  color: 'var(--accent-500)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '1px solid rgba(90, 143, 179, 0.28)',
                  flexShrink: 0
                }}
              >
                <span style={{ width: 24, height: 24 }}><Icon.Shield /></span>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <h3
                  style={{
                    margin: 0,
                    fontSize: '1.12rem',
                    fontWeight: 700,
                    color: 'var(--sand-100)',
                    fontFamily: 'var(--font-display)',
                    lineHeight: 1.3
                  }}
                >
                  Portal Público de Verificación de Autenticidad
                </h3>
                <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)', lineHeight: 1.4 }}>
                  Ingresa el código del certificado para consultar su estado de autenticidad.
                </p>
              </div>
            </div>

            {/* Body: Formulario ordenado con campo y botón de altura uniforme */}
            <div className="card-body" style={{ padding: '24px' }}>
              <form onSubmit={handleVerifyCertificate}>
                <div style={{ marginBottom: 4 }}>
                  <label
                    className="form-label"
                    style={{
                      display: 'block',
                      marginBottom: 8,
                      fontSize: '0.84rem',
                      fontWeight: 600,
                      color: 'var(--sand-200)'
                    }}
                  >
                    Código de Verificación:
                  </label>
                  <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                    <input
                      type="text"
                      required
                      className="input"
                      placeholder="Ingresa el código del certificado"
                      style={{
                        flex: 1,
                        height: 42,
                        fontFamily: 'monospace',
                        textTransform: 'uppercase',
                        letterSpacing: '0.04em',
                        fontSize: '0.88rem'
                      }}
                      value={verifyCodeInput}
                      onChange={e => setVerifyCodeInput(e.target.value)}
                    />
                    <button
                      type="submit"
                      className="btn btn-primary"
                      style={{
                        height: 42,
                        padding: '0 24px',
                        fontSize: '0.88rem',
                        fontWeight: 600,
                        whiteSpace: 'nowrap'
                      }}
                    >
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
                        {verificationResult.cert.status === 'VALIDO' ? 'Certificado válido y vigente' : 'Certificado revocado'}
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
                Puedes cancelar tu propia inscripción para liberar el cupo. La cancelación requiere un motivo.
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
