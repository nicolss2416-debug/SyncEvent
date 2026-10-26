import React, { useState } from 'react';

type VerificationStatus = 'NOT_FOUND' | 'VALID' | 'REVOKED' | 'COUNTERFEIT';

type VerificationResult = {
  status: VerificationStatus;
  verificationCode: string;
  participantName: string | null;
  eventName: string | null;
  message: string;
  issuedAt: string | null;
  active: boolean;
};

const API_BASE = 'http://localhost:8080/api';

export default function App() {
  const [code, setCode] = useState('CERT-VALID-123');
  const [result, setResult] = useState<VerificationResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function verify() {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${API_BASE}/public/certificates/${encodeURIComponent(code)}`);
      if (!response.ok) {
        throw new Error(`Error HTTP ${response.status}`);
      }

      const data = (await response.json()) as VerificationResult;
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo validar el certificado.');
      setResult(null);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="page">
      <section className="card">
        <p className="eyebrow">SyncEvent</p>
        <h1>Verificación pública de certificados</h1>
        <p className="subtitle">
          Introduce el código del certificado para comprobar si es válido, revocado o sospechoso.
        </p>

        <div className="form-row">
          <input
            value={code}
            onChange={(event) => setCode(event.target.value)}
            placeholder="Ej. CERT-VALID-123"
            aria-label="Código de verificación"
          />
          <button onClick={verify} disabled={loading || !code.trim()}>
            {loading ? 'Verificando…' : 'Verificar'}
          </button>
        </div>

        {error ? <p className="error">{error}</p> : null}

        {result ? (
          <div className={`status status-${result.status.toLowerCase()}`}>
            <strong>{result.status}</strong>
            <p>{result.message}</p>
            <dl>
              <div>
                <dt>Código</dt>
                <dd>{result.verificationCode}</dd>
              </div>
              <div>
                <dt>Participante</dt>
                <dd>{result.participantName ?? '—'}</dd>
              </div>
              <div>
                <dt>Evento</dt>
                <dd>{result.eventName ?? '—'}</dd>
              </div>
              <div>
                <dt>Emitido</dt>
                <dd>{result.issuedAt ? new Date(result.issuedAt).toLocaleString('es-ES') : '—'}</dd>
              </div>
            </dl>
          </div>
        ) : null}
      </section>
    </main>
  );
}
