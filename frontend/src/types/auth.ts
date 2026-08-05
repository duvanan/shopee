// Mirror backend/user-service DTOs — RBAC (docs/architecture/rbac.md):
// a user can hold multiple roles, so `roles` is an array. Role code is a
// free-form string now (not a fixed "CUSTOMER"|"ADMIN" union) - admin can
// create custom roles (VD "STAFF") from the Phân quyền screen, so any code
// can come back from the API.

export interface UserResponse {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  roles: string[];
  /** Danh sach permission code (VD "USER_MANAGE") - dung de an/hien UI, khong chi dua vao ten role. */
  permissions: string[];
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  user: UserResponse;
}

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export interface ForgotPasswordPayload {
  email: string;
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}

export interface SessionResponse {
  sessionId: string;
  ipAddress: string | null;
  userAgent: string | null;
  issuedAt: string;
  lastUsedAt: string;
  expiresAt: string;
}

// Shape of an error ApiResponse<Void> (success:false) - docs/architecture/api-response-standard.md.
export interface ApiErrorBody {
  success?: boolean;
  code?: string;
  message?: string;
  errors?: { field: string; message: string }[];
}
