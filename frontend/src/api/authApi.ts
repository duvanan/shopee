import { http } from "@/api/http";
import type { ApiResponse } from "@/types/api";
import type { AuthResponse, LoginPayload, RegisterPayload, UserResponse } from "@/types/auth";

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
};
