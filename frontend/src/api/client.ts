import type { AuthTokens, ApiError } from './types';
import { loadSession, saveSession, clearSession } from './tokenStore';

export const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080/api/v1';

export class ApiRequestError extends Error {
  status: number;
  code?: string;
  detail?: string;

  constructor(status: number, body: ApiError | null) {
    super(body?.detail ?? body?.title ?? `Erreur HTTP ${status}`);
    this.status = status;
    this.code = body?.code;
    this.detail = body?.detail;
  }
}

/** Appelé par AuthContext : redirige vers la connexion quand la session ne peut plus être renouvelée. */
let sessionExpiredHandler: (() => void) | null = null;
export function onSessionExpired(handler: () => void) {
  sessionExpiredHandler = handler;
}

let refreshInFlight: Promise<string | null> | null = null;

async function doRefresh(): Promise<string | null> {
  const session = loadSession();
  if (!session) return null;
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: session.refreshToken }),
    });
    if (!res.ok) {
      clearSession();
      sessionExpiredHandler?.();
      return null;
    }
    const tokens = (await res.json()) as AuthTokens;
    saveSession(tokens);
    return tokens.accessToken;
  } catch {
    return null;
  }
}

function refreshAccessToken(): Promise<string | null> {
  if (!refreshInFlight) {
    refreshInFlight = doRefresh().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

export interface RequestOptions {
  method?: string;
  body?: unknown;
  auth?: boolean; // par défaut true
  query?: Record<string, string | number | boolean | undefined | null>;
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = new URL(`${API_BASE}${path}`);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.toString();
}

async function parseBody(res: Response): Promise<unknown> {
  if (res.status === 204) return null;
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = true, query } = options;
  const url = buildUrl(path, query);

  const doFetch = async (): Promise<Response> => {
    const headers: Record<string, string> = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (auth) {
      const session = loadSession();
      if (session) headers.Authorization = `Bearer ${session.accessToken}`;
    }
    return fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  };

  let res = await doFetch();

  if (res.status === 401 && auth && loadSession()) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      res = await doFetch();
    }
  }

  if (!res.ok) {
    const errorBody = (await parseBody(res)) as ApiError | null;
    throw new ApiRequestError(res.status, errorBody);
  }

  return (await parseBody(res)) as T;
}

export function accessTokenForEventSource(): string | null {
  return loadSession()?.accessToken ?? null;
}
