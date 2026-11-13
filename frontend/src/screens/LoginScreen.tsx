import React, { useState } from 'react';
import { AuthUser, UserRole } from '../types/types';
import { Icon } from '../components/icons';
import { ThemeToggle } from '../components/ThemeToggle';
import { API_BASE_URL } from '../config/config';

export function LoginScreen({
  onLogin,
  onGoRegister,
  onBack,
}: {
  onLogin: (u: AuthUser) => void;
  onGoRegister: () => void;
  onBack: () => void;
}) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successNotice, setSuccessNotice] = useState('');

  // Estado del modal coordinado de recuperación de contraseña (Opción 2.A: OTP)
  const [showForgotModal, setShowForgotModal] = useState(false);
  const [forgotStep, setForgotStep] = useState<1 | 2>(1);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotOtp, setForgotOtp] = useState('');
  const [forgotNewPassword, setForgotNewPassword] = useState('');
  const [forgotConfirmPassword, setForgotConfirmPassword] = useState('');
  const [showForgotPwdText, setShowForgotPwdText] = useState(false);
  const [forgotLoading, setForgotLoading] = useState(false);
  const [forgotError, setForgotError] = useState('');
  const [devOtpHolder, setDevOtpHolder] = useState<string | null>(null);

  const handleOpenForgotModal = () => {
    setForgotEmail(email || '');
    setForgotStep(1);
    setForgotOtp('');
    setForgotNewPassword('');
    setForgotConfirmPassword('');
    setShowForgotPwdText(false);
    setForgotLoading(false);
    setForgotError('');
    setDevOtpHolder(null);
    setShowForgotModal(true);
  };

  // Paso 1: Solicitar código OTP al backend
  const handleRequestOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!forgotEmail.trim()) return;

    setForgotLoading(true);
    setForgotError('');
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/forgot-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: forgotEmail.trim() }),
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        setForgotError(errorData.message || 'No se pudo generar el código. Inténtalo nuevamente.');
        return;
      }

      const data = await res.json().catch(() => ({}));
      if (data.devOtp) {
        setDevOtpHolder(data.devOtp);
      }
      setForgotStep(2);
    } catch {
      setForgotError('No se pudo contactar al servidor. Revisa tu conexión.');
    } finally {
      setForgotLoading(false);
    }
  };

  // Paso 2: Validar OTP y aplicar nueva contraseña
  const handleConfirmReset = async (e: React.FormEvent) => {
    e.preventDefault();
    if (forgotOtp.length !== 6) {
      setForgotError('El código de verificación debe contener 6 dígitos.');
      return;
    }
    if (forgotNewPassword.length < 8 || forgotNewPassword.length > 128) {
      setForgotError('La nueva contraseña debe tener entre 8 y 128 caracteres.');
      return;
    }
    if (forgotNewPassword !== forgotConfirmPassword) {
      setForgotError('Las contraseñas no coinciden.');
      return;
    }

    setForgotLoading(true);
    setForgotError('');
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: forgotEmail.trim(),
          otp: forgotOtp.trim(),
          newPassword: forgotNewPassword,
        }),
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        setForgotError(errorData.message || 'Código de verificación inválido o expirado.');
        return;
      }

      // Éxito: cerrar modal, completar email en pantalla principal y avisar
      setShowForgotModal(false);
      setEmail(forgotEmail.trim());
      setPassword('');
      setSuccessNotice('¡Contraseña restablecida exitosamente! Ya puedes ingresar con tu nueva clave.');
      setErrorMessage('');
    } catch {
      setForgotError('Error de red al intentar restablecer la contraseña.');
    } finally {
      setForgotLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage('');

    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });

      if (!res.ok) {
        setErrorMessage('No se pudo iniciar sesión. Revisa tus credenciales o el estado de la cuenta.');
        return;
      }
      const data = await res.json();
      const allRoles = (data.roles || []) as UserRole[];
      if (!data.accessToken || allRoles.length === 0) {
        setErrorMessage('El servidor no devolvió una sesión válida. Inténtalo nuevamente.');
        return;
      }
      const signedInUser: AuthUser = {
        email,
        name: email.split('@')[0],
        role: allRoles[0],
        token: data.accessToken,
        allRoles,
      };
      onLogin(signedInUser);
    } catch {
      setErrorMessage('No hay conexión con el servidor. No se inició una sesión local.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div style={{ position: 'fixed', top: 20, right: 24, zIndex: 20 }}>
        <ThemeToggle />
      </div>
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
        <p className="auth-subtitle">Inicia sesión con las credenciales de tu cuenta.</p>

        {errorMessage && (
          <div className="auth-error animate-fade-in" style={{ marginBottom: 16 }}>
            <span style={{ width: 16, height: 16 }}><Icon.AlertTriangle /></span>
            {errorMessage}
          </div>
        )}

        {successNotice && (
          <div
            className="animate-fade-in"
            style={{
              marginBottom: 16,
              background: 'rgba(52, 211, 153, 0.15)',
              border: '1px solid var(--success-400)',
              color: 'var(--success-400)',
              padding: '10px 14px',
              borderRadius: 'var(--radius-sm)',
              fontSize: '0.82rem',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
          >
            <span style={{ width: 16, height: 16, flexShrink: 0 }}><Icon.CheckCircle /></span>
            <span>{successNotice}</span>
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
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <label className="form-label" style={{ marginBottom: 0 }}>Contraseña</label>
              <button
                type="button"
                onClick={handleOpenForgotModal}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--accent-400)',
                  fontSize: '0.78rem',
                  cursor: 'pointer',
                  padding: '2px 0',
                  fontWeight: 500,
                }}
              >
                ¿Olvidaste tu contraseña?
              </button>
            </div>
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

      {/* ── MODAL: RECUPERACIÓN AUTÓNOMA DE CONTRASEÑA (FLUJO OTP 2 PASOS) ── */}
      {showForgotModal && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 460 }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span style={{ width: 20, height: 20, color: 'var(--accent-400)' }}><Icon.Key /></span>
                <h3 className="modal-title">
                  {forgotStep === 1 ? 'Recuperación de Contraseña' : 'Ingresa tu Código y Nueva Contraseña'}
                </h3>
              </div>
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => setShowForgotModal(false)}
                style={{ padding: 4 }}
              >
                <span style={{ width: 16, height: 16 }}><Icon.X /></span>
              </button>
            </div>

            {forgotError && (
              <div
                className="animate-fade-in"
                style={{
                  margin: '12px 20px 0',
                  background: 'rgba(239, 68, 68, 0.12)',
                  border: '1px solid rgba(239, 68, 68, 0.3)',
                  color: 'var(--error-400)',
                  padding: '8px 12px',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.8rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                <span style={{ width: 15, height: 15, flexShrink: 0 }}><Icon.AlertTriangle /></span>
                <span>{forgotError}</span>
              </div>
            )}

            {/* PASO 1: Ingreso de correo y solicitud del código */}
            {forgotStep === 1 ? (
              <form onSubmit={handleRequestOtp}>
                <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  <p style={{ fontSize: '0.82rem', color: 'var(--stone-300)', margin: 0 }}>
                    Ingresa el correo electrónico asociado a tu cuenta. Te generaremos un código de verificación de 6 dígitos válido por 15 minutos.
                  </p>

                  <div className="form-group">
                    <label className="form-label">Correo Electrónico Registrado</label>
                    <input
                      type="email"
                      required
                      className="input"
                      placeholder="ejemplo@correo.com"
                      value={forgotEmail}
                      onChange={e => setForgotEmail(e.target.value)}
                    />
                  </div>
                </div>

                <div className="modal-footer">
                  <button type="button" className="btn btn-ghost" onClick={() => setShowForgotModal(false)}>
                    Cancelar
                  </button>
                  <button type="submit" className="btn btn-primary" disabled={forgotLoading || !forgotEmail.trim()}>
                    {forgotLoading ? (
                      <>
                        <span style={{ width: 14, height: 14, marginRight: 6 }}><Icon.Loader /></span>
                        Generando código...
                      </>
                    ) : (
                      <>
                        <span style={{ width: 14, height: 14, marginRight: 6 }}><Icon.Mail /></span>
                        Enviar Código de Verificación
                      </>
                    )}
                  </button>
                </div>
              </form>
            ) : (
              /* PASO 2: Ingreso de OTP y nueva contraseña */
              <form onSubmit={handleConfirmReset}>
                <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  <div style={{ fontSize: '0.82rem', color: 'var(--stone-300)' }}>
                    Código generado para: <strong style={{ color: 'var(--sand-100)' }}>{forgotEmail}</strong>
                    <button
                      type="button"
                      onClick={() => { setForgotStep(1); setForgotError(''); }}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: 'var(--accent-400)',
                        fontSize: '0.78rem',
                        cursor: 'pointer',
                        marginLeft: 8,
                        textDecoration: 'underline',
                      }}
                    >
                      (Cambiar correo)
                    </button>
                  </div>

                  {/* Banner de código de desarrollo si está disponible */}
                  {devOtpHolder && (
                    <div
                      style={{
                        background: 'rgba(90, 143, 179, 0.14)',
                        border: '1px dashed var(--accent-400)',
                        borderRadius: 'var(--radius-sm)',
                        padding: '10px 14px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                      }}
                    >
                      <div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--stone-400)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                          Código de prueba (Modo Local/Dev):
                        </div>
                        <div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--accent-400)', letterSpacing: '3px' }}>
                          {devOtpHolder}
                        </div>
                      </div>
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        style={{ fontSize: '0.74rem' }}
                        onClick={() => setForgotOtp(devOtpHolder)}
                      >
                        Autocompletar
                      </button>
                    </div>
                  )}

                  <div className="form-group">
                    <label className="form-label">Código de Verificación (6 dígitos)</label>
                    <input
                      type="text"
                      required
                      maxLength={6}
                      pattern="[0-9]{6}"
                      className="input"
                      placeholder="Ej: 123456"
                      value={forgotOtp}
                      onChange={e => setForgotOtp(e.target.value.replace(/[^0-9]/g, '').slice(0, 6))}
                      style={{
                        fontSize: '1.15rem',
                        letterSpacing: '4px',
                        textAlign: 'center',
                        fontWeight: 600,
                      }}
                    />
                  </div>

                  <div className="form-group">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <label className="form-label">Nueva Contraseña</label>
                      <button
                        type="button"
                        tabIndex={-1}
                        onClick={() => setShowForgotPwdText(!showForgotPwdText)}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: 'var(--accent-400)',
                          fontSize: '0.74rem',
                          cursor: 'pointer',
                        }}
                      >
                        {showForgotPwdText ? 'Ocultar' : 'Mostrar'}
                      </button>
                    </div>
                    <input
                      type={showForgotPwdText ? 'text' : 'password'}
                      required
                      minLength={8}
                      maxLength={128}
                      className="input"
                      placeholder="Mínimo 8 caracteres"
                      value={forgotNewPassword}
                      onChange={e => setForgotNewPassword(e.target.value)}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Confirmar Nueva Contraseña</label>
                    <input
                      type={showForgotPwdText ? 'text' : 'password'}
                      required
                      minLength={8}
                      maxLength={128}
                      className="input"
                      placeholder="Repite la contraseña"
                      value={forgotConfirmPassword}
                      onChange={e => setForgotConfirmPassword(e.target.value)}
                    />
                  </div>

                  {/* Checklist interactivo */}
                  <div style={{ fontSize: '0.76rem', display: 'flex', flexDirection: 'column', gap: 4 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: forgotNewPassword.length >= 8 ? 'var(--success-400)' : 'var(--stone-500)' }}>
                      <span style={{ width: 13, height: 13 }}>
                        {forgotNewPassword.length >= 8 ? <Icon.Check /> : <Icon.Ban />}
                      </span>
                      Mínimo 8 caracteres
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: forgotNewPassword && forgotNewPassword === forgotConfirmPassword ? 'var(--success-400)' : 'var(--stone-500)' }}>
                      <span style={{ width: 13, height: 13 }}>
                        {forgotNewPassword && forgotNewPassword === forgotConfirmPassword ? <Icon.Check /> : <Icon.Ban />}
                      </span>
                      Las contraseñas coinciden
                    </div>
                  </div>
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => { setForgotStep(1); setForgotError(''); }}
                  >
                    Atrás
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={forgotLoading || forgotOtp.length !== 6 || forgotNewPassword.length < 8 || forgotNewPassword !== forgotConfirmPassword}
                  >
                    {forgotLoading ? (
                      <>
                        <span style={{ width: 14, height: 14, marginRight: 6 }}><Icon.Loader /></span>
                        Restableciendo...
                      </>
                    ) : (
                      <>
                        <span style={{ width: 14, height: 14, marginRight: 6 }}><Icon.Check /></span>
                        Confirmar Nueva Contraseña
                      </>
                    )}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
