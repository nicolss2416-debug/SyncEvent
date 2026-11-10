import React from 'react';
import { Event } from '../types/types';
import { Icon } from '../components/icons';
import { ThemeToggle } from '../components/ThemeToggle';

export function LandingScreen({
  events,
  onLogin,
  onRegister,
}: {
  events: Event[];
  onLogin: () => void;
  onRegister: () => void;
}) {
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
          <ThemeToggle />
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
          <div className="card" onClick={onLogin} style={{ padding: 22, textAlign: 'left', borderTop: '3px solid var(--accent-500)', cursor: 'pointer' }}>
            <div style={{ width: 38, height: 38, borderRadius: 10, background: 'rgba(166,99,57,0.12)', color: 'var(--accent-500)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 12 }}>
              <span style={{ width: 20, height: 20 }}><Icon.Shield /></span>
            </div>
            <h4 style={{ margin: '0 0 6px', color: 'var(--sand-100)', fontSize: '1.05rem' }}>Administrador</h4>
            <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--stone-400)', lineHeight: 1.5 }}>
              Administra las cuentas, revisa la actividad, resuelve multas y supervisa los certificados.
            </p>
          </div>

          <div className="card" onClick={onLogin} style={{ padding: 22, textAlign: 'left', borderTop: '3px solid #b39d7d', cursor: 'pointer' }}>
            <div style={{ width: 38, height: 38, borderRadius: 10, background: 'rgba(179,157,125,0.15)', color: '#7d6754', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 12 }}>
              <span style={{ width: 20, height: 20 }}><Icon.Settings /></span>
            </div>
            <h4 style={{ margin: '0 0 6px', color: 'var(--sand-100)', fontSize: '1.05rem' }}>Operador</h4>
            <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--stone-400)', lineHeight: 1.5 }}>
              Organiza eventos, gestiona participantes e inscripciones y asigna monitores.
            </p>
          </div>

          <div className="card" onClick={onLogin} style={{ padding: 22, textAlign: 'left', borderTop: '3px solid #16a34a', cursor: 'pointer' }}>
            <div style={{ width: 38, height: 38, borderRadius: 10, background: 'rgba(22,163,74,0.12)', color: '#16a34a', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 12 }}>
              <span style={{ width: 20, height: 20 }}><Icon.Scan /></span>
            </div>
            <h4 style={{ margin: '0 0 6px', color: 'var(--sand-100)', fontSize: '1.05rem' }}>Monitor</h4>
            <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--stone-400)', lineHeight: 1.5 }}>
              Registra asistencias y consulta la ocupación de los eventos asignados.
            </p>
          </div>

          <div className="card" onClick={onLogin} style={{ padding: 22, textAlign: 'left', borderTop: '3px solid var(--accent-400)', cursor: 'pointer' }}>
            <div style={{ width: 38, height: 38, borderRadius: 10, background: 'rgba(191,123,80,0.12)', color: 'var(--accent-400)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 12 }}>
              <span style={{ width: 20, height: 20 }}><Icon.User /></span>
            </div>
            <h4 style={{ margin: '0 0 6px', color: 'var(--sand-100)', fontSize: '1.05rem' }}>Invitado</h4>
            <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--stone-400)', lineHeight: 1.5 }}>
              Explora eventos, auto-inscríbete, gestiona tus credenciales QR y descarga certificados.
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
