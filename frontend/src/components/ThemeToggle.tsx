import React from 'react';
import { Icon } from './icons';
import { useThemeMode } from '../utils/theme';

/**
 * Botón de tema: alterna entre Automático → Oscuro → Claro → Automático.
 * El modo automático se activa a las 18:30 y vuelve a claro a las 06:30.
 */
export function ThemeToggle() {
  const { mode, theme, cycleMode } = useThemeMode();

  const resolvedLabel = theme === 'dark' ? 'oscuro' : 'claro';
  const title = mode === 'auto'
    ? `Tema automático (${resolvedLabel}, 18:30–06:30). Clic para fijar oscuro`
    : mode === 'dark'
      ? 'Tema oscuro fijo. Clic para fijar claro'
      : 'Tema claro fijo. Clic para volver a automático';

  const Glyph = mode === 'auto' ? Icon.Clock : mode === 'dark' ? Icon.Moon : Icon.Sun;
  const label = mode === 'auto' ? 'Auto' : mode === 'dark' ? 'Oscuro' : 'Claro';

  return (
    <button
      type="button"
      className="btn btn-ghost btn-sm theme-toggle"
      onClick={cycleMode}
      title={title}
      aria-label={title}
    >
      <span style={{ width: 16, height: 16 }}><Glyph /></span>
      <span className="theme-toggle-label">{label}</span>
    </button>
  );
}
