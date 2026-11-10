function resolveApiBaseUrl(): string {
  const envUrl = (import.meta as { env?: { VITE_API_URL?: string } }).env?.VITE_API_URL?.trim();

  // If accessed over LAN (non-localhost) in browser, use relative path through Vite proxy
  if (typeof window !== 'undefined' && window.location.hostname && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
    return '';
  }

  if (!envUrl) {
    return '';
  }

  // Strip trailing slashes and remove trailing /api to prevent /api/api/... duplication
  return envUrl.replace(/\/+$/, '').replace(/\/api$/, '');
}

export const API_BASE_URL = resolveApiBaseUrl();

