import React, { useState } from 'react';
import { Icon } from '../components/icons';
import { ThemeToggle } from '../components/ThemeToggle';
import { API_BASE_URL } from '../config/config';

export function RegisterScreen({
  onRegistered,
  onGoLogin,
  onBack,
}: {
  onRegistered: () => void;
  onGoLogin: () => void;
  onBack: () => void;
}) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [documentType, setDocumentType] = useState('CC');
  const [documentNumber, setDocumentNumber] = useState('');
  const [phone, setPhone] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage('');

    const cleanPhone = phone.replace(/\D/g, '');
    if (cleanPhone.length !== 10) {
      setErrorMessage('El número de celular debe contener exactamente 10 dígitos numéricos (sin letras, puntos ni comas).');
      setIsLoading(false);
      return;
    }

    const cleanDoc = documentType === 'CC' || documentType === 'TI'
      ? documentNumber.replace(/\D/g, '')
      : documentNumber.replace(/[.,]/g, '').trim();

    if ((documentType === 'CC' || documentType === 'TI') && (cleanDoc.length < 6 || cleanDoc.length > 10)) {
      setErrorMessage('La cédula de ciudadanía debe contener entre 6 y 10 dígitos numéricos (sin puntos ni comas).');
      setIsLoading(false);
      return;
    }

    if (!cleanDoc) {
      setErrorMessage('El número de documento es obligatorio.');
      setIsLoading(false);
      return;
    }

    const cleanEmail = email.trim().toLowerCase();
    const cleanFirstName = firstName.trim();
    const cleanLastName = lastName.trim();

    if (!cleanFirstName || !cleanLastName) {
      setErrorMessage('Nombres y apellidos son obligatorios.');
      setIsLoading(false);
      return;
    }

    if (!cleanEmail) {
      setErrorMessage('El correo electrónico es obligatorio.');
      setIsLoading(false);
      return;
    }

    if (password.length < 8) {
      setErrorMessage('La contraseña debe contener al menos 8 caracteres.');
      setIsLoading(false);
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: cleanEmail,
          password,
          firstName: cleanFirstName,
          lastName: cleanLastName,
          documentType,
          documentNumber: cleanDoc,
          phone: cleanPhone,
        }),
      });
      if (!response.ok) {
        const problem = await response.json().catch(() => null);
        let msg = problem?.detail || problem?.message || problem?.title;
        if (problem?.errors && typeof problem.errors === 'object') {
          const fieldErrors = Object.entries(problem.errors)
            .map(([field, err]) => `${field}: ${err}`)
            .join(' | ');
          msg = msg ? `${msg} (${fieldErrors})` : fieldErrors;
        }
        setErrorMessage(msg || `Error ${response.status}: No se pudo crear la cuenta.`);
        return;
      }
      onRegistered();
    } catch {
      setErrorMessage('No hay conexión con el servidor. Verifica que el backend esté activo.');
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

        <h2 className="auth-title">Crear Cuenta</h2>
        <p className="auth-subtitle">Crea tu cuenta de Invitado y completa tus datos para inscribirte.</p>

        {errorMessage && (
          <div className="auth-error animate-fade-in" style={{ marginBottom: 16 }}>
            <span style={{ width: 16, height: 16 }}><Icon.AlertTriangle /></span>
            {errorMessage}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label className="form-label">Nombres</label>
            <input type="text" required maxLength={100} className="input" value={firstName}
              onChange={e => setFirstName(e.target.value)} />
          </div>
          <div className="form-group">
            <label className="form-label">Apellidos</label>
            <input type="text" required maxLength={100} className="input" value={lastName}
              onChange={e => setLastName(e.target.value)} />
          </div>
          <div className="form-group">
            <label className="form-label">Correo Electrónico</label>
            <input
              type="email"
              required
              className="input"
              placeholder="laura@ejemplo.com"
              value={email}
              onChange={e => setEmail(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label className="form-label">Tipo de documento</label>
            <select
              className="select"
              value={documentType}
              onChange={e => {
                const newType = e.target.value;
                setDocumentType(newType);
                if (newType === 'CC' || newType === 'TI') {
                  setDocumentNumber(prev => prev.replace(/\D/g, '').slice(0, 10));
                }
              }}
            >
              <option value="CC">Cédula de ciudadanía</option>
              <option value="CE">Cédula de extranjería</option>
              <option value="TI">Tarjeta de identidad</option>
              <option value="PASAPORTE">Pasaporte</option>
            </select>
          </div>
          <div className="form-group">
            <label className="form-label">
              Número de documento {documentType === 'CC' ? '(solo números, hasta 10 dígitos)' : ''}
            </label>
            <input
              type="text"
              required
              inputMode={documentType === 'CC' || documentType === 'TI' ? 'numeric' : 'text'}
              maxLength={documentType === 'CC' || documentType === 'TI' ? 10 : 20}
              className="input"
              placeholder={documentType === 'CC' ? 'Ej. 1025664889' : 'Número de documento'}
              value={documentNumber}
              onChange={e => {
                if (documentType === 'CC' || documentType === 'TI') {
                  setDocumentNumber(e.target.value.replace(/\D/g, '').slice(0, 10));
                } else {
                  setDocumentNumber(e.target.value.replace(/[.,]/g, '').slice(0, 20));
                }
              }}
            />
          </div>
          <div className="form-group">
            <label className="form-label">Número de celular (10 dígitos)</label>
            <input
              type="tel"
              required
              inputMode="numeric"
              maxLength={10}
              className="input"
              placeholder="Ej. 3001234567"
              value={phone}
              onChange={e => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Contraseña</label>
            <input
              type="password"
              required
              minLength={8}
              maxLength={128}
              className="input"
              placeholder="••••••••"
              value={password}
              onChange={e => setPassword(e.target.value)}
            />
          </div>

          <button type="submit" className="btn btn-primary w-full btn-lg" disabled={isLoading} style={{ marginTop: 8 }}>
            {isLoading ? 'Creando cuenta…' : 'Registrarme'}
          </button>
        </form>

        <div className="auth-switch">
          ¿Ya tienes cuenta?{' '}
          <button className="auth-switch-btn" onClick={onGoLogin}>
            Iniciar sesión
          </button>
        </div>
      </div>
    </div>
  );
}
