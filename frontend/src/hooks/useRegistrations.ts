/**
 * useRegistrations — Custom hook que concentra todas las mutaciones sobre
 * inscripciones y la lógica de check-in de asistencia (monitor/operador).
 *
 * Mantiene la separación de responsabilidades:
 *  - App.tsx orquesta el estado global y delega aquí las operaciones puntuales
 *    de inscripciones y check-in.
 */
import { useCallback } from 'react';
import { Registration, Participant } from '../types/types';

interface UseRegistrationsOptions {
  apiRequest: <T>(path: string, options?: RequestInit) => Promise<T>;
  setRegistrations: React.Dispatch<React.SetStateAction<Registration[]>>;
  setParticipants: React.Dispatch<React.SetStateAction<Participant[]>>;
  addToast: (msg: string, type?: 'success' | 'info' | 'error' | 'warning') => void;
}

export interface UseRegistrationsReturn {
  handleRegisterParticipantToEvent: (participantId: string, eventId: string) => Promise<boolean>;
  handleSelfRegister: (eventId: string) => Promise<boolean>;
  handleSelfCancelRegistration: (regId: string, reason: string) => Promise<boolean>;
  handleCheckInByToken: (token: string, eventId: string) => Promise<void>;
  handleManualCheckIn: (eventId: string, participantId: string) => Promise<boolean>;
  handleSearchAttendanceRegistrations: (
    eventId: string,
    query: string,
  ) => Promise<Registration[]>;
  handleLoadAttendanceStats: (
    eventId: string,
  ) => Promise<{ total: number; checkedIn: number; absent: number }>;
  handleBulkImportParticipants: (
    items: Omit<Participant, 'id' | 'createdAt'>[],
  ) => Promise<boolean>;
  handleAddParticipant: (data: Omit<Participant, 'id' | 'createdAt'>) => Promise<boolean>;
}

export function useRegistrations({
  apiRequest,
  setRegistrations,
  setParticipants,
  addToast,
}: UseRegistrationsOptions): UseRegistrationsReturn {

  // ── Inscribir participante a evento (Operador/Admin) ───────────────────────
  const handleRegisterParticipantToEvent = useCallback(
    async (participantId: string, eventId: string): Promise<boolean> => {
      try {
        const reg = await apiRequest<{
          id: string;
          eventId: string;
          eventName: string;
          participantId: string;
          status: 'ACTIVA' | 'CANCELADA';
          qrToken: string;
          createdAt: string;
          checkedIn?: boolean;
          participantName?: string;
          participantEmail?: string;
        }>(`/api/events/${eventId}/registrations`, {
          method: 'POST',
          body: JSON.stringify({ participantId }),
        });
        setRegistrations((prev) => [
          ...prev,
          {
            ...reg,
            participantName: reg.participantName || 'Participante',
            participantEmail: reg.participantEmail || '',
            checkedIn: Boolean(reg.checkedIn),
          },
        ]);
        addToast('Participante inscrito correctamente.', 'success');
        return true;
      } catch (error) {
        addToast(
          error instanceof Error ? error.message : 'No se pudo inscribir al participante.',
          'error',
        );
        return false;
      }
    },
    [apiRequest, setRegistrations, addToast],
  );

  // ── Auto-inscripción del invitado ──────────────────────────────────────────
  const handleSelfRegister = useCallback(
    async (eventId: string): Promise<boolean> => {
      try {
        const reg = await apiRequest<{
          id: string;
          eventId: string;
          eventName: string;
          participantId: string;
          status: 'ACTIVA' | 'CANCELADA';
          qrToken: string;
          createdAt: string;
          checkedIn?: boolean;
        }>('/api/registrations', {
          method: 'POST',
          body: JSON.stringify({ eventId }),
        });
        setRegistrations((prev) => [
          ...prev,
          { ...reg, participantName: '', participantEmail: '', checkedIn: false },
        ]);
        addToast('¡Te has inscrito al evento correctamente!', 'success');
        return true;
      } catch (error) {
        addToast(
          error instanceof Error ? error.message : 'No se pudo realizar la inscripción.',
          'error',
        );
        return false;
      }
    },
    [apiRequest, setRegistrations, addToast],
  );

  // ── Cancelar inscripción propia ────────────────────────────────────────────
  const handleSelfCancelRegistration = useCallback(
    async (regId: string, reason: string): Promise<boolean> => {
      try {
        await apiRequest<void>(`/api/registrations/${regId}/cancel`, {
          method: 'POST',
          body: JSON.stringify({ reason }),
        });
        setRegistrations((prev) =>
          prev.map((r) => (r.id === regId ? { ...r, status: 'CANCELADA' } : r)),
        );
        addToast('Inscripción cancelada.', 'info');
        return true;
      } catch (error) {
        addToast(
          error instanceof Error ? error.message : 'No se pudo cancelar la inscripción.',
          'error',
        );
        return false;
      }
    },
    [apiRequest, setRegistrations, addToast],
  );

  // ── Check-in por token QR ──────────────────────────────────────────────────
  const handleCheckInByToken = useCallback(
    async (token: string, eventId: string): Promise<void> => {
      try {
        const result = await apiRequest<{
          participantId: string;
          participantName: string;
          alreadyCheckedIn: boolean;
        }>(`/api/events/${eventId}/check-in`, {
          method: 'POST',
          body: JSON.stringify({ qrToken: token }),
        });
        setRegistrations((prev) =>
          prev.map((r) =>
            r.eventId === eventId && r.qrToken === token ? { ...r, checkedIn: true } : r,
          ),
        );
        addToast(
          result.alreadyCheckedIn
            ? `${result.participantName} ya había registrado asistencia.`
            : `Asistencia registrada: ${result.participantName}`,
          result.alreadyCheckedIn ? 'warning' : 'success',
        );
      } catch (error) {
        addToast(
          error instanceof Error ? error.message : 'No se pudo registrar la asistencia.',
          'error',
        );
      }
    },
    [apiRequest, setRegistrations, addToast],
  );

  // ── Check-in manual por ID de participante ─────────────────────────────────
  const handleManualCheckIn = useCallback(
    async (eventId: string, participantId: string): Promise<boolean> => {
      try {
        await apiRequest<void>(`/api/events/${eventId}/check-in/manual`, {
          method: 'POST',
          body: JSON.stringify({ participantId }),
        });
        setRegistrations((prev) =>
          prev.map((r) =>
            r.eventId === eventId && r.participantId === participantId
              ? { ...r, checkedIn: true }
              : r,
          ),
        );
        addToast('Asistencia registrada manualmente.', 'success');
        return true;
      } catch (error) {
        addToast(
          error instanceof Error ? error.message : 'No se pudo registrar la asistencia manual.',
          'error',
        );
        return false;
      }
    },
    [apiRequest, setRegistrations, addToast],
  );

  // ── Búsqueda de inscripciones para check-in ────────────────────────────────
  const handleSearchAttendanceRegistrations = useCallback(
    async (eventId: string, query: string): Promise<Registration[]> => {
      try {
        const results = await apiRequest<
          Array<{
            id: string;
            eventId: string;
            participantId: string;
            status: 'ACTIVA' | 'CANCELADA';
            qrToken: string;
            createdAt: string;
            checkedIn?: boolean;
            participantName?: string;
            participantEmail?: string;
          }>
        >(`/api/events/${eventId}/registrations?q=${encodeURIComponent(query)}`);
        return results.map((r) => ({
          ...r,
          participantName: r.participantName || 'Participante',
          participantEmail: r.participantEmail || '',
          checkedIn: Boolean(r.checkedIn),
          eventName: '',
        }));
      } catch {
        return [];
      }
    },
    [apiRequest],
  );

  // ── Estadísticas de asistencia ─────────────────────────────────────────────
  const handleLoadAttendanceStats = useCallback(
    async (eventId: string): Promise<{ total: number; checkedIn: number; absent: number }> => {
      try {
        return await apiRequest<{ total: number; checkedIn: number; absent: number }>(
          `/api/events/${eventId}/attendance-stats`,
        );
      } catch {
        return { total: 0, checkedIn: 0, absent: 0 };
      }
    },
    [apiRequest],
  );

  // ── Importación masiva CSV ─────────────────────────────────────────────────
  const handleBulkImportParticipants = useCallback(
    async (items: Omit<Participant, 'id' | 'createdAt'>[]): Promise<boolean> => {
      try {
        const report = await apiRequest<{
          total: number;
          saved: number;
          rejected: number;
          errors: { row: number; documentNumber: string; reason: string }[];
        }>('/api/participants/bulk', {
          method: 'POST',
          body: JSON.stringify({ items }),
        });
        if (report.rejected > 0) {
          addToast(
            `Lote rechazado: ${report.rejected} de ${report.total} registros con errores. Ningún participante fue guardado.`,
            'error',
          );
          return false;
        }
        // Recargar lista actualizada desde el servidor
        const updatedList = await apiRequest<Participant[]>('/api/participants?size=1000');
        setParticipants(updatedList ?? []);
        addToast(`Importación exitosa: ${report.saved} participantes registrados.`, 'success');
        return true;
      } catch (error) {
        addToast(
          error instanceof Error ? error.message : 'No se pudieron importar los participantes.',
          'error',
        );
        return false;
      }
    },
    [apiRequest, setParticipants, addToast],
  );

  // ── Registrar participante individual ──────────────────────────────────────
  const handleAddParticipant = useCallback(
    async (data: Omit<Participant, 'id' | 'createdAt'>): Promise<boolean> => {
      try {
        const created = await apiRequest<Participant>('/api/participants', {
          method: 'POST',
          body: JSON.stringify(data),
        });
        setParticipants((prev) => [...prev, created]);
        addToast('Participante registrado correctamente.', 'success');
        return true;
      } catch (error) {
        addToast(
          error instanceof Error ? error.message : 'No se pudo registrar el participante.',
          'error',
        );
        return false;
      }
    },
    [apiRequest, setParticipants, addToast],
  );

  return {
    handleRegisterParticipantToEvent,
    handleSelfRegister,
    handleSelfCancelRegistration,
    handleCheckInByToken,
    handleManualCheckIn,
    handleSearchAttendanceRegistrations,
    handleLoadAttendanceStats,
    handleBulkImportParticipants,
    handleAddParticipant,
  };
}
