/**
 * useAuth — Custom hook que encapsula toda la lógica de autenticación de SyncEvent:
 *  - Estado de sesión (authUser)
 *  - Login / logout
 *  - Sincronización periódica de permisos con el backend (/api/auth/me)
 *  - Gestión del token JWT (expiración, refresh)
 */
import { useState, useCallback, useEffect } from 'react';
import { AuthUser, UserRole, Toast } from '../types/types';
import { API_BASE_URL } from '../config/config';
import { getSavedSession } from '../utils/session';
import {
  roleNameMap,
  ROLE_ROUTE_MAP,
  ROUTE_ROLE_MAP,
  normalizePath,
} from '../config/routes';

interface UseAuthOptions {
  addToast: (msg: string, type?: Toast['type']) => void;
  navigateTo: (path: string, replace?: boolean) => void;
  onLogout?: () => void;
}

export interface UseAuthReturn {
  authUser: AuthUser | null;
  setAuthUser: React.Dispatch<React.SetStateAction<AuthUser | null>>;
  handleLoginSuccess: (user: AuthUser) => void;
  handleLogout: () => void;
  syncLivePermissions: () => Promise<void>;
}

export function useAuth({ addToast, navigateTo, onLogout }: UseAuthOptions): UseAuthReturn {
  const [authUser, setAuthUser] = useState<AuthUser | null>(getSavedSession);

  // ── Sincronización periódica de permisos ───────────────────────────────────
  const syncLivePermissions = useCallback(async () => {
    if (!authUser?.token) return;
    try {
      const meRes = await fetch(`${API_BASE_URL}/api/auth/me`, {
        headers: { Authorization: `Bearer ${authUser.token}` },
      });
      if (meRes.status === 401) {
        localStorage.removeItem('syncevent_session');
        setAuthUser(null);
        navigateTo('/login', true);
        return;
      }
      if (!meRes.ok) return;
      const meData = (await meRes.json()) as { accessToken?: string; roles?: UserRole[] };
      const liveRoles: UserRole[] =
        Array.isArray(meData.roles) && meData.roles.length > 0 ? meData.roles : ['INVITADO'];
      const resolvedRole: UserRole = liveRoles.includes(authUser.role) ? authUser.role : liveRoles[0];
      const rolesChanged =
        resolvedRole !== authUser.role ||
        JSON.stringify(liveRoles) !== JSON.stringify(authUser.allRoles || []);
      const tokenChanged = Boolean(meData.accessToken && meData.accessToken !== authUser.token);
      if (rolesChanged || tokenChanged) {
        const updatedSession: AuthUser = {
          ...authUser,
          token: meData.accessToken || authUser.token,
          role: resolvedRole,
          allRoles: liveRoles,
        };
        setAuthUser(updatedSession);
        localStorage.setItem('syncevent_session', JSON.stringify(updatedSession));
        if (rolesChanged) {
          const targetPath = ROLE_ROUTE_MAP[resolvedRole] || '/invitado';
          if (normalizePath(window.location.pathname) !== targetPath) {
            navigateTo(targetPath, true);
          }
          addToast(
            `Permisos actualizados desde la base de datos: ahora ingresas como ${roleNameMap[resolvedRole]}.`,
            'info',
          );
        }
      }
    } catch {
      // Ignorar fallos de red temporales
    }
  }, [authUser, navigateTo, addToast]);

  // Polling cada 15s + eventos de foco/visibilidad
  useEffect(() => {
    if (!authUser?.token) return;
    const handleFocus = () => void syncLivePermissions();
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') void syncLivePermissions();
    };
    window.addEventListener('focus', handleFocus);
    document.addEventListener('visibilitychange', handleVisibilityChange);
    const interval = window.setInterval(() => void syncLivePermissions(), 15000);
    return () => {
      window.removeEventListener('focus', handleFocus);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.clearInterval(interval);
    };
  }, [authUser?.token, syncLivePermissions]);

  // ── Login ──────────────────────────────────────────────────────────────────
  const handleLoginSuccess = useCallback(
    (user: AuthUser) => {
      setAuthUser(user);
      localStorage.setItem('syncevent_session', JSON.stringify(user));
      addToast(`¡Bienvenido a SyncEvent, ${user.name}! Accediendo como ${user.role}.`, 'success');

      const savedRedirect = sessionStorage.getItem('syncevent_redirect');
      sessionStorage.removeItem('syncevent_redirect');
      if (
        savedRedirect &&
        ROUTE_ROLE_MAP[savedRedirect] &&
        user.allRoles?.includes(ROUTE_ROLE_MAP[savedRedirect])
      ) {
        const redirectRole = ROUTE_ROLE_MAP[savedRedirect];
        const updatedUser = { ...user, role: redirectRole };
        setAuthUser(updatedUser);
        localStorage.setItem('syncevent_session', JSON.stringify(updatedUser));
        navigateTo(savedRedirect, true);
      } else {
        navigateTo(ROLE_ROUTE_MAP[user.role] || '/invitado', true);
      }
    },
    [addToast, navigateTo],
  );

  // ── Logout ─────────────────────────────────────────────────────────────────
  const handleLogout = useCallback(() => {
    if (authUser?.token) {
      void fetch(`${API_BASE_URL}/api/auth/logout`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${authUser.token}` },
      }).catch(() => undefined);
    }
    localStorage.removeItem('syncevent_session');
    sessionStorage.removeItem('syncevent_redirect');
    setAuthUser(null);
    onLogout?.();
    navigateTo('/', true);
    addToast('Sesión cerrada correctamente', 'info');
  }, [authUser, navigateTo, addToast, onLogout]);

  return { authUser, setAuthUser, handleLoginSuccess, handleLogout, syncLivePermissions };
}
