// Mirror backend/user-service admin DTOs - docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md

export interface AdminUserResponse {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  status: "ACTIVE" | "LOCKED";
  roles: string[];
  createdAt: string;
  lastLoginAt: string | null;
}

export interface AdminCreateUserPayload {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  roleCode?: string;
}

export interface AdminUpdateUserPayload {
  fullName: string;
  phone?: string;
}

export interface AssignRolePayload {
  roleCode: string;
}

export interface RoleResponse {
  id: string;
  code: string;
  name: string;
  description: string | null;
  isSystem: boolean;
}

export interface RoleDetailResponse extends RoleResponse {
  permissionCodes: string[];
}

export interface RoleCreatePayload {
  code: string;
  name: string;
  description?: string;
}

export interface RoleUpdatePayload {
  name: string;
  description?: string;
}

export interface PermissionResponse {
  id: string;
  code: string;
  name: string;
  description: string | null;
  module: string;
}

export interface SetRolePermissionsPayload {
  permissionCodes: string[];
}

export interface AuditLogResponse {
  id: string;
  actorId: string | null;
  actorEmail: string | null;
  actorType: string;
  action: string;
  module: string;
  objectType: string | null;
  objectId: string | null;
  description: string | null;
  oldValue: string | null;
  newValue: string | null;
  status: "SUCCESS" | "FAILED";
  createdAt: string;
}
