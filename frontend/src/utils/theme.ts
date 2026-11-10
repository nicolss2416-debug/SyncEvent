import { useCallback, useEffect, useState } from 'react';

export type ThemeMode = 'auto' | 'light' | 'dark';
export type ResolvedTheme = 'light' | 'dark';

const STORAGE_KEY = 'syncevent_theme';

// El modo automático se activa a las 18:30 y vuelve a claro a las 06:30.
export const DARK_START_MINUTES = 18 * 60 + 30;
export const DARK_END_MINUTES = 6 * 60 + 30;

const listeners = new Set<() => void>();

function notifyThemeChange() {
  listeners.forEach(listener => listener());
}

export function isNightTime(date: Date = new Date()): boolean {
  const minutes = date.getHours() * 60 + date.getMinutes();
  if (DARK_START_MINUTES > DARK_END_MINUTES) {
    return minutes >= DARK_START_MINUTES || minutes < DARK_END_MINUTES;
  }
  return minutes >= DARK_START_MINUTES && minutes < DARK_END_MINUTES;
}

export function getStoredThemeMode(): ThemeMode {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark' || stored === 'auto') return stored;
  } catch {
    // localStorage puede no estar disponible (modo privado, etc.)
  }
  return 'auto';
}

export function resolveTheme(mode: ThemeMode): ResolvedTheme {
  if (mode === 'auto') return isNightTime() ? 'dark' : 'light';
  return mode;
}

function applyTheme(mode: ThemeMode): ResolvedTheme {
  const theme = resolveTheme(mode);
  document.documentElement.setAttribute('data-theme', theme);
  document.documentElement.setAttribute('data-theme-mode', mode);
  return theme;
}

function persistThemeMode(mode: ThemeMode) {
  try {
    if (mode === 'auto') {
      localStorage.removeItem(STORAGE_KEY);
    } else {
      localStorage.setItem(STORAGE_KEY, mode);
    }
  } catch {
    // Ignorar errores de almacenamiento.
  }
}

export function useThemeMode() {
  const [mode, setModeState] = useState<ThemeMode>(getStoredThemeMode);
  const [theme, setThemeState] = useState<ResolvedTheme>(() => resolveTheme(getStoredThemeMode()));

  const sync = useCallback(() => {
    const currentMode = getStoredThemeMode();
    setModeState(currentMode);
    setThemeState(applyTheme(currentMode));
  }, []);

  useEffect(() => {
    sync();
    listeners.add(sync);

    const interval = window.setInterval(() => {
      // Solo el modo automático debe reaccionar al paso del tiempo.
      if (getStoredThemeMode() === 'auto') sync();
    }, 60 * 1000);

    const handleVisibility = () => {
      if (document.visibilityState === 'visible') sync();
    };

    document.addEventListener('visibilitychange', handleVisibility);
    return () => {
      listeners.delete(sync);
      window.clearInterval(interval);
      document.removeEventListener('visibilitychange', handleVisibility);
    };
  }, [sync]);

  const setMode = useCallback((nextMode: ThemeMode) => {
    persistThemeMode(nextMode);
    const nextTheme = applyTheme(nextMode);
    setModeState(nextMode);
    setThemeState(nextTheme);
    notifyThemeChange();
  }, []);

  // Auto → Oscuro → Claro → Auto
  const cycleMode = useCallback(() => {
    const nextMode: ThemeMode = mode === 'auto' ? 'dark' : mode === 'dark' ? 'light' : 'auto';
    setMode(nextMode);
  }, [mode, setMode]);

  return { mode, theme, setMode, cycleMode };
}
