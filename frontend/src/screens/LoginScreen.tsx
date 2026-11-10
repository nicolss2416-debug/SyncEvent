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
