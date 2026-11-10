import { AuthUser } from '../types/types';

export const SESSION_STORAGE_KEY = 'syncevent_session';

export function getSavedSession(): AuthUser | null {
  try {
    const raw = localStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return null;
    const user = JSON.parse(raw) as AuthUser;
    if (!user || !user.token || !user.role || !user.email) return null;
    const parts = user.token.split('.');
    if (parts.length === 3) {
      try {
        const payload = JSON.parse(atob(parts[1]));
        if (payload.exp && payload.exp * 1000 < Date.now()) {
          localStorage.removeItem(SESSION_STORAGE_KEY);
          return null;
        }
      } catch {
        // Ignorar posibles tokens sin base64 estándar
      }
    }
    return user;
  } catch {
    return null;
  }
}
