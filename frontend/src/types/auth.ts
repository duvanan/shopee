// Mirror backend/user-service DTOs — RBAC (docs/architecture/rbac.md):
// a user can hold multiple roles, so `roles` is an array.

export type Role = "CUSTOMER" | "ADMIN";

export interface UserResponse {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  roles: Role[];
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

// Shape of an error ApiResponse<Void> (success:false) - docs/architecture/api-response-standard.md.
export interface ApiErrorBody {
  success?: boolean;
  code?: string;
  message?: string;
  errors?: { field: string; message: string }[];
}
