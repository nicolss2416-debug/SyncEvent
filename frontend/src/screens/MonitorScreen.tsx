import React, { useState, useMemo } from 'react';
import { Event, Registration } from '../types';
import { Icon } from '../icons';

interface MonitorScreenProps {
  events: Event[];
  registrations: Registration[];
  userEmail: string;
  onCheckInByToken: (token: string, eventId: string) => { success: boolean; message: string; reg?: Registration };
  onToggleManualCheckIn: (regId: string) => void;
  addToast: (msg: string, type?: 'success' | 'info' | 'error' | 'warning') => void;
}

export function MonitorScreen({
  events,
  registrations,
  userEmail,
  onCheckInByToken,
  onToggleManualCheckIn,
  addToast,
}: MonitorScreenProps) {
  // Filter active events where monitor can operate
  const operableEvents = useMemo(() => {
    return events.filter(e => e.status === 'EN_CURSO' || e.status === 'PUBLICADO');
  }, [events]);

  const [selectedEventId, setSelectedEventId] = useState<string>(operableEvents[0]?.id || '');
  const [tokenInput, setTokenInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [lastScanResult, setLastScanResult] = useState<{
    status: 'success' | 'duplicate' | 'error';
    title: string;
    message: string;
    reg?: Registration;
    time: string;
  } | null>(null);

  const currentEvent = events.find(e => e.id === selectedEventId) || operableEvents[0];

  // Event specific registrations
  const eventRegs = useMemo(() => {
    if (!currentEvent) return [];
    return registrations.filter(r => r.eventId === currentEvent.id && r.status === 'ACTIVA');
  }, [registrations, currentEvent]);

  const checkedInCount = eventRegs.filter(r => r.checkedIn).length;
  const totalCapacity = currentEvent?.capacity || 100;
  const occupancyPct = Math.round((checkedInCount / totalCapacity) * 100);

  // Filtered registrations for manual check-in search
  const filteredRegs = useMemo(() => {
    if (!searchQuery.trim()) return eventRegs;
    const q = searchQuery.toLowerCase();
    return eventRegs.filter(r => 
      r.participantName.toLowerCase().includes(q) ||
      r.participantEmail.toLowerCase().includes(q) ||
      r.qrToken.toLowerCase().includes(q)
    );
  }, [eventRegs, searchQuery]);

  const handleProcessScan = (tokenToScan?: string) => {
    const raw = (tokenToScan || tokenInput).trim();
    if (!raw) {
      addToast('Ingresa o escanea un código QR válido', 'warning');
      return;
    }

    if (!currentEvent) {
      addToast('No hay evento activo seleccionado', 'error');
      return;
    }

    const res = onCheckInByToken(raw, currentEvent.id);
    const now = new Date().toLocaleTimeString('es-CO');

    if (res.success) {
      setLastScanResult({
        status: 'success',
        title: '¡Acceso Permitido!',
        message: res.message,
        reg: res.reg,
        time: now,
      });
      addToast(res.message, 'success');
      setTokenInput('');
    } else if (res.message.includes('ya tiene asistencia') || res.message.includes('duplicad')) {
      setLastScanResult({
        status: 'duplicate',
        title: 'Asistencia Ya Registrada',
        message: res.message,
        reg: res.reg,
        time: now,
      });
      addToast(res.message, 'warning');
    } else {
      setLastScanResult({
        status: 'error',
        title: 'Acceso Denegado',
        message: res.message,
        time: now,
      });
      addToast(res.message, 'error');
    }
  };

  return (
    <div className="actor-screen monitor-screen animate-fade-in">
      {/* ── Actor Header Banner ── */}
      <div className="actor-header-banner" style={{ background: 'linear-gradient(135deg, #FFFFFF 0%, #FAF5ED 100%)', border: '1px solid rgba(186,160,130,0.35)', borderRadius: 'var(--radius-lg)', padding: '24px 28px', marginBottom: 28, boxShadow: 'var(--shadow-sm)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{ width: 52, height: 52, borderRadius: 14, background: 'linear-gradient(135deg,#16a34a,#14532d)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', boxShadow: '0 8px 24px rgba(20,83,45,0.4)' }}>
              <span style={{ width: 26, height: 26 }}><Icon.Scan /></span>
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <h2 style={{ margin: 0, fontSize: '1.4rem', color: 'var(--sand-100)', fontFamily: 'var(--font-display)' }}>Terminal de Acceso y Check-In en Vivo</h2>
                <span className="badge badge-emerald" style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: '0.72rem' }}>Monitor de Entrada</span>
              </div>
              <p style={{ margin: '4px 0 0', fontSize: '0.86rem', color: 'var(--stone-400)' }}>
                Validación de códigos QR en tiempo real, prevención de ingresos duplicados (RN-04) y telemetría de aforo en vivo.
              </p>
            </div>
          </div>

          {/* Event Selector */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontSize: '0.82rem', color: 'var(--stone-300)', fontWeight: 500 }}>Evento en control:</span>
            <select
              className="select"
              style={{ width: 'auto', minWidth: 260, fontSize: '0.85rem' }}
              value={selectedEventId}
              onChange={e => {
                setSelectedEventId(e.target.value);
                setLastScanResult(null);
              }}
            >
              {operableEvents.map(e => (
                <option key={e.id} value={e.id}>
                  {e.name} ({e.status})
                </option>
              ))}
              {operableEvents.length === 0 && (
                <option value="">No hay eventos activos disponibles</option>
              )}
            </select>
          </div>
        </div>
      </div>

      {currentEvent ? (
        <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 24 }}>
          {/* ── LEFT COLUMN: QR Scanner & Live Feedback ── */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
            {/* Live Stats Pill Banner */}
            <div className="card" style={{ padding: 20 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span className="pulse-dot" style={{ width: 10, height: 10, borderRadius: '50%', background: 'var(--success-400)' }} />
                  <span style={{ fontSize: '0.88rem', fontWeight: 600, color: 'var(--sand-100)' }}>Ocupación de Sala en Tiempo Real</span>
                </div>
                <span className="badge badge-slate" style={{ fontSize: '0.75rem' }}>
                  {currentEvent.modality} · {currentEvent.location || 'Sala General'}
                </span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, textAlign: 'center', marginBottom: 16 }}>
                <div style={{ background: 'var(--surface-2)', padding: 12, borderRadius: 'var(--radius-sm)' }}>
                  <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--sand-100)', fontFamily: 'var(--font-display)' }}>{checkedInCount}</div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--success-400)', textTransform: 'uppercase' }}>Ingresados</div>
                </div>
                <div style={{ background: 'var(--surface-2)', padding: 12, borderRadius: 'var(--radius-sm)' }}>
                  <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--sand-100)', fontFamily: 'var(--font-display)' }}>{eventRegs.length}</div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--accent-400)', textTransform: 'uppercase' }}>Inscritos</div>
                </div>
                <div style={{ background: 'var(--surface-2)', padding: 12, borderRadius: 'var(--radius-sm)' }}>
                  <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--sand-100)', fontFamily: 'var(--font-display)' }}>{totalCapacity}</div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--stone-400)', textTransform: 'uppercase' }}>Capacidad Max</div>
                </div>
              </div>

              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', color: 'var(--stone-400)', marginBottom: 6 }}>
                  <span>Porcentaje de Ocupación</span>
                  <strong style={{ color: occupancyPct >= 90 ? 'var(--error-400)' : 'var(--sand-200)' }}>{occupancyPct}%</strong>
                </div>
                <div className="progress-bar" style={{ height: 10 }}>
                  <div
                    className="progress-bar-fill"
                    style={{
                      width: `${Math.min(occupancyPct, 100)}%`,
                      background: occupancyPct >= 90 ? 'linear-gradient(90deg, #f97316, #ef4444)' : 'linear-gradient(90deg, #22c55e, #16a34a)'
                    }}
                  />
                </div>
                {occupancyPct >= 90 && (
                  <div style={{ marginTop: 8, fontSize: '0.76rem', color: 'var(--error-400)', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span style={{ width: 14, height: 14 }}><Icon.AlertTriangle /></span>
                    Alerta: Sala cercana a su capacidad máxima permitida (Regla RN-05).
                  </div>
                )}
              </div>
            </div>

            {/* Simulated Camera & QR Scanner View */}
            <div className="card">
              <div className="card-header">
                <h3 className="card-title">
                  <span style={{ width: 18, height: 18, color: 'var(--success-400)' }}><Icon.QrCode /></span>
                  Lector de Código QR
                </h3>
                <span className="badge badge-emerald badge-dot">Cámara Lista</span>
              </div>

              {/* Viewfinder Mockup */}
              <div style={{ position: 'relative', height: 200, background: '#0a0a0a', borderRadius: 'var(--radius-md)', overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center', border: '2px solid var(--glass-border)' }}>
                {/* Corner reticles */}
                <div style={{ position: 'absolute', top: 16, left: 16, width: 28, height: 28, borderTop: '3px solid #22c55e', borderLeft: '3px solid #22c55e' }} />
                <div style={{ position: 'absolute', top: 16, right: 16, width: 28, height: 28, borderTop: '3px solid #22c55e', borderRight: '3px solid #22c55e' }} />
                <div style={{ position: 'absolute', bottom: 16, left: 16, width: 28, height: 28, borderBottom: '3px solid #22c55e', borderLeft: '3px solid #22c55e' }} />
                <div style={{ position: 'absolute', bottom: 16, right: 16, width: 28, height: 28, borderBottom: '3px solid #22c55e', borderRight: '3px solid #22c55e' }} />

                {/* Animated laser line */}
                <div style={{ position: 'absolute', width: '80%', height: 2, background: 'rgba(34,197,94,0.9)', boxShadow: '0 0 12px #22c55e', animation: 'scanLaser 2.2s ease-in-out infinite' }} />

                <div style={{ textAlign: 'center', color: 'rgba(255,255,255,0.7)', zIndex: 2 }}>
                  <span style={{ width: 42, height: 42, display: 'inline-block', opacity: 0.8 }}><Icon.QrCode /></span>
                  <div style={{ fontSize: '0.82rem', marginTop: 8 }}>Apunta el código QR del participante al recuadro</div>
                </div>
              </div>

              {/* Fast Input Field */}
              <div style={{ marginTop: 18 }}>
                <label className="form-label">Entrada Rápida / Token de Inscripción:</label>
                <div style={{ display: 'flex', gap: 8 }}>
                  <input
                    type="text"
                    className="input"
                    placeholder="Ej. QR-SYNC-001"
                    style={{ fontFamily: 'monospace', textTransform: 'uppercase', letterSpacing: '0.05em' }}
                    value={tokenInput}
                    onChange={e => setTokenInput(e.target.value)}
                    onKeyDown={e => {
                      if (e.key === 'Enter') handleProcessScan();
                    }}
                  />
                  <button className="btn btn-primary" onClick={() => handleProcessScan()}>
                    Validar
                  </button>
                </div>
              </div>

              {/* Quick test tokens buttons */}
              <div style={{ marginTop: 14 }}>
                <span style={{ fontSize: '0.74rem', color: 'var(--stone-500)', display: 'block', marginBottom: 6 }}>Tokens de prueba disponibles:</span>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                  {eventRegs.slice(0, 4).map(r => (
                    <button
                      key={r.id}
                      className="btn btn-ghost btn-sm"
                      style={{ fontSize: '0.74rem', padding: '4px 8px', fontFamily: 'monospace' }}
                      onClick={() => handleProcessScan(r.qrToken)}
                    >
                      {r.qrToken} {r.checkedIn ? '✓' : ''}
                    </button>
                  ))}
                  <button
                    className="btn btn-ghost btn-sm"
                    style={{ fontSize: '0.74rem', padding: '4px 8px', fontFamily: 'monospace', color: 'var(--error-400)' }}
                    onClick={() => handleProcessScan('QR-INEXISTENTE-999')}
                  >
                    Token Inválido ✕
                  </button>
                </div>
              </div>
            </div>

            {/* Scan Result Feedback Card */}
            {lastScanResult && (
              <div
                className="card animate-fade-in"
                style={{
                  border: `2px solid ${
                    lastScanResult.status === 'success' ? 'var(--success-500)' :
                    lastScanResult.status === 'duplicate' ? 'var(--warning-500)' : 'var(--error-500)'
                  }`,
                  background: `linear-gradient(135deg, ${
                    lastScanResult.status === 'success' ? 'rgba(34,197,94,0.1)' :
                    lastScanResult.status === 'duplicate' ? 'rgba(234,179,8,0.1)' : 'rgba(239,68,68,0.1)'
                  }, var(--surface-1))`
                }}
              >
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      background: lastScanResult.status === 'success' ? 'var(--success-500)' :
                                  lastScanResult.status === 'duplicate' ? 'var(--warning-500)' : 'var(--error-500)',
                      color: '#fff',
                      flexShrink: 0
                    }}
                  >
                    <span style={{ width: 22, height: 22 }}>
                      {lastScanResult.status === 'success' ? <Icon.Check /> :
                       lastScanResult.status === 'duplicate' ? <Icon.Clock /> : <Icon.Ban />}
                    </span>
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <h4 style={{ margin: 0, color: 'var(--sand-100)', fontSize: '1.05rem' }}>{lastScanResult.title}</h4>
                      <span style={{ fontSize: '0.74rem', color: 'var(--stone-400)' }}>{lastScanResult.time}</span>
                    </div>
                    <p style={{ margin: '4px 0 10px', fontSize: '0.85rem', color: 'var(--stone-300)' }}>{lastScanResult.message}</p>

                    {lastScanResult.reg && (
                      <div style={{ background: 'var(--surface-2)', padding: 12, borderRadius: 'var(--radius-sm)', fontSize: '0.82rem' }}>
                        <div><strong>Participante:</strong> {lastScanResult.reg.participantName}</div>
                        <div><strong>Correo:</strong> {lastScanResult.reg.participantEmail}</div>
                        <div><strong>Token:</strong> <code>{lastScanResult.reg.qrToken}</code></div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* ── RIGHT COLUMN: Manual Check-In List ── */}
          <div className="card" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <div className="card-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
              <div>
                <h3 className="card-title" style={{ margin: 0 }}>Listado y Búsqueda Manual</h3>
                <p style={{ margin: '4px 0 0', fontSize: '0.78rem', color: 'var(--stone-400)' }}>
                  En caso de falla del dispositivo móvil del participante.
                </p>
              </div>
              <span className="badge badge-accent" style={{ fontSize: '0.75rem' }}>{eventRegs.length} Registrados</span>
            </div>

            {/* Search Box */}
            <div style={{ position: 'relative', marginBottom: 16 }}>
              <input
                type="text"
                className="input"
                placeholder="Buscar por nombre, correo o token..."
                style={{ paddingLeft: 36, fontSize: '0.84rem' }}
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
              />
              <span style={{ position: 'absolute', left: 12, top: 11, width: 16, height: 16, color: 'var(--stone-500)' }}><Icon.Search /></span>
            </div>

            {/* List */}
            <div style={{ flex: 1, overflowY: 'auto', maxHeight: 520, display: 'flex', flexDirection: 'column', gap: 8 }}>
              {filteredRegs.map(reg => (
                <div
                  key={reg.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 14px',
                    background: reg.checkedIn ? 'rgba(34,197,94,0.06)' : 'var(--surface-2)',
                    border: `1px solid ${reg.checkedIn ? 'rgba(34,197,94,0.25)' : 'var(--glass-border)'}`,
                    borderRadius: 'var(--radius-sm)',
                    transition: 'all 0.2s ease'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div
                      style={{
                        width: 32,
                        height: 32,
                        borderRadius: '50%',
                        background: reg.checkedIn ? 'var(--success-500)' : 'var(--stone-700)',
                        color: '#fff',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '0.8rem',
                        fontWeight: 600
                      }}
                    >
                      {reg.checkedIn ? <span style={{ width: 16, height: 16 }}><Icon.Check /></span> : reg.participantName.slice(0, 1)}
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--sand-100)', fontSize: '0.85rem' }}>{reg.participantName}</div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--stone-400)', fontFamily: 'monospace' }}>{reg.qrToken} · {reg.participantEmail}</div>
                    </div>
                  </div>

                  <button
                    className={`btn btn-sm ${reg.checkedIn ? 'btn-ghost' : 'btn-primary'}`}
                    style={{ fontSize: '0.76rem', padding: '6px 12px' }}
                    onClick={() => {
                      onToggleManualCheckIn(reg.id);
                      addToast(reg.checkedIn ? `Desmarcado: ${reg.participantName}` : `Ingreso manual: ${reg.participantName}`, 'info');
                    }}
                  >
                    {reg.checkedIn ? 'Desmarcar' : 'Ingresar'}
                  </button>
                </div>
              ))}

              {filteredRegs.length === 0 && (
                <div style={{ textAlign: 'center', padding: 30, color: 'var(--stone-500)', fontSize: '0.85rem' }}>
                  No se encontraron participantes que coincidan con la búsqueda.
                </div>
              )}
            </div>
          </div>
        </div>
      ) : (
        <div className="card" style={{ textAlign: 'center', padding: 50 }}>
          <p style={{ color: 'var(--stone-400)' }}>No hay eventos disponibles en estado Publicado o En Curso para supervisión.</p>
        </div>
      )}
    </div>
  );
}
