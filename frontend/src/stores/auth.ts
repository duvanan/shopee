import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { authApi } from "@/api/authApi";
import {
  extractErrorMessage,
  getStoredToken,
  setRefreshHandler,
  setStoredToken,
  setUnauthorizedHandler,
} from "@/api/http";
import type { LoginPayload, RegisterPayload, UserResponse } from "@/types/auth";

export const useAuthStore = defineStore("auth", () => {
  const user = ref<UserResponse | null>(null);
  const token = ref<string | null>(getStoredToken());

  const isAuthenticated = computed(() => token.value !== null);
  const isAdmin = computed(() => user.value?.roles.includes("ADMIN") ?? false);

  function setSession(accessToken: string, sessionUser: UserResponse): void {
    token.value = accessToken;
    user.value = sessionUser;
    setStoredToken(accessToken);
  }

  function clearSession(): void {
    token.value = null;
    user.value = null;
    setStoredToken(null);
  }

  async function register(payload: RegisterPayload): Promise<void> {
    try {
      const data = await authApi.register(payload);
      setSession(data.accessToken, data.user);
    } catch (error) {
      throw new Error(extractErrorMessage(error, "Đăng ký thất bại, vui lòng thử lại."));
    }
  }

  async function login(payload: LoginPayload): Promise<void> {
    try {
      const data = await authApi.login(payload);
      setSession(data.accessToken, data.user);
    } catch (error) {
      throw new Error(extractErrorMessage(error, "Email hoặc mật khẩu không đúng."));
    }
  }

  function logout(): void {
    // Xoa session cuc bo ngay cho UX nhanh; thu hoi refresh token cookie o
    // server chay ngam, khong can cho vi UI da chuyen ve trang dang nhap.
    clearSession();
    authApi.logout().catch(() => {});
  }

  // Registered with http.ts so a 401 anywhere can trigger one silent
  // refresh (via the refreshToken httpOnly cookie) instead of forcing
  // re-login on every access-token expiry (~60 min).
  async function refreshAccessToken(): Promise<string> {
    const data = await authApi.refresh();
    token.value = data.accessToken;
    user.value = data.user;
    setStoredToken(data.accessToken);
    return data.accessToken;
  }

  // Called once at app start-up (see main.ts) when a token survives a page
  // reload but we haven't fetched the profile yet in this session.
  async function fetchCurrentUser(): Promise<void> {
    if (!token.value) return;
    try {
      user.value = await authApi.me();
    } catch {
      clearSession();
    }
  }

  // Vue Router kicks off its initial navigation as soon as app.use(router)
  // runs, independently of main.ts awaiting fetchCurrentUser() - so on a
  // hard refresh the router guard could run before `user` is populated,
  // making isAdmin false for a real admin. main.ts and the guard both await
  // this same memoized promise so the guard sees the resolved profile.
  let bootstrapPromise: Promise<void> | null = null;
  function ensureBootstrapped(): Promise<void> {
    bootstrapPromise ??= fetchCurrentUser();
    return bootstrapPromise;
  }

  setUnauthorizedHandler(clearSession);
  setRefreshHandler(refreshAccessToken);

  return { user, token, isAuthenticated, isAdmin, register, login, logout, fetchCurrentUser, ensureBootstrapped };
});
