import { http } from "@/api/http";
import type { ApiResponse } from "@/types/api";
import type {
  AuthResponse,
  ChangePasswordPayload,
  ForgotPasswordPayload,
  LoginPayload,
  RegisterPayload,
  ResetPasswordPayload,
  SessionResponse,
  UserResponse,
} from "@/types/auth";

export const authApi = {
  register(payload: RegisterPayload) {
    return http.post<ApiResponse<AuthResponse>>("/api/users/auth/register", payload).then((res) => res.data.data);
  },
  login(payload: LoginPayload) {
    return http.post<ApiResponse<AuthResponse>>("/api/users/auth/login", payload).then((res) => res.data.data);
  },
  me() {
    return http.get<ApiResponse<UserResponse>>("/api/users/me").then((res) => res.data.data);
  },
  // Refresh token itself travels as an httpOnly cookie the browser attaches
  // automatically - nothing to pass here.
  refresh() {
    return http.post<ApiResponse<AuthResponse>>("/api/users/auth/refresh").then((res) => res.data.data);
  },
  logout() {
    return http.post<void>("/api/users/auth/logout");
  },
  logoutAll() {
    return http.post<void>("/api/users/auth/logout-all");
  },
  changePassword(payload: ChangePasswordPayload) {
    return http.post<ApiResponse<void>>("/api/users/auth/change-password", payload).then((res) => res.data);
  },
  forgotPassword(payload: ForgotPasswordPayload) {
    return http.post<ApiResponse<void>>("/api/users/auth/forgot-password", payload).then((res) => res.data);
  },
  resetPassword(payload: ResetPasswordPayload) {
    return http.post<ApiResponse<void>>("/api/users/auth/reset-password", payload).then((res) => res.data);
  },
  listSessions() {
    return http.get<ApiResponse<SessionResponse[]>>("/api/users/auth/sessions").then((res) => res.data.data);
  },
  revokeSession(sessionId: string) {
    return http.delete(`/api/users/auth/sessions/${sessionId}`);
  },
};
