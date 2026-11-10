import { UserRole } from '../types/types';

export const roleNameMap: Record<UserRole, string> = {
  ADMINISTRADOR: 'Administrador',
  OPERADOR: 'Operador Logístico',
  MONITOR: 'Monitor de Entrada',
  INVITADO: 'Invitado / Participante',
};

export const ROLE_ROUTE_MAP: Record<UserRole, string> = {
  ADMINISTRADOR: '/admin',
  OPERADOR: '/operador',
  MONITOR: '/monitor',
  INVITADO: '/invitado',
};

export const ROUTE_ROLE_MAP: Record<string, UserRole> = {
  '/admin': 'ADMINISTRADOR',
  '/operador': 'OPERADOR',
  '/monitor': 'MONITOR',
  '/invitado': 'INVITADO',
  '/participante': 'INVITADO',
};

export const PROTECTED_ROUTES = ['/admin', '/operador', '/monitor', '/invitado', '/participante'];

export function normalizePath(rawPath: string): string {
  const clean = rawPath.replace(/\/+$/, '') || '/';
  return clean.toLowerCase();
}
