import axios from "axios";
import type { ApiErrorBody } from "@/types/auth";

declare module "axios" {
  export interface InternalAxiosRequestConfig {
    /** Marks a request that has already gone through one silent-refresh retry, to prevent retry loops. */
    _retry?: boolean;
  }
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  // Bat buoc de trinh duyet gui/nhan cookie refreshToken (httpOnly) qua Gateway,
  // vi frontend (5173) va gateway (8080) la 2 origin khac nhau.
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

const TOKEN_STORAGE_KEY = "shopee_access_token";

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function setStoredToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  }
}

http.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Registered by the auth store at app start-up; kept out of this module so
// http.ts never has to import the Pinia store (would create a cycle, since
// the store itself calls into api/authApi -> http).
let onUnauthorized: (() => void) | null = null;

export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler;
}

// Also registered by the auth store. Calls POST /api/users/auth/refresh
// (refreshToken cookie sent automatically) and returns the new access token.
let refreshAccessToken: (() => Promise<string>) | null = null;

export function setRefreshHandler(handler: () => Promise<string>): void {
  refreshAccessToken = handler;
}

// Concurrent 401s (e.g. several requests firing at once right as the access
// token expires) must trigger exactly one /refresh call; the rest await it.
let pendingRefresh: Promise<string> | null = null;

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;
    const isAuthEndpoint = Boolean(config?.url?.includes("/api/users/auth/"));

    if (response?.status === 401 && config && !config._retry && !isAuthEndpoint && refreshAccessToken) {
      config._retry = true;
      try {
        pendingRefresh ??= refreshAccessToken().finally(() => {
          pendingRefresh = null;
        });
        const newAccessToken = await pendingRefresh;
        config.headers.Authorization = `Bearer ${newAccessToken}`;
        return http(config);
      } catch {
        onUnauthorized?.();
        return Promise.reject(error);
      }
    }

    if (response?.status === 401) {
      onUnauthorized?.();
    }
    return Promise.reject(error);
  },
);

export function extractErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    return error.response?.data?.message ?? fallback;
  }
  return fallback;
}
