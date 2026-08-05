import { http } from "@/api/http";
import type { ApiResponse } from "@/types/api";
import type {
  PermissionResponse,
  RoleCreatePayload,
  RoleDetailResponse,
  RoleResponse,
  RoleUpdatePayload,
  SetRolePermissionsPayload,
} from "@/types/admin";

export const adminRoleApi = {
  list() {
    return http.get<ApiResponse<RoleResponse[]>>("/api/users/admin/roles").then((res) => res.data.data);
  },
  get(id: string) {
    return http.get<ApiResponse<RoleDetailResponse>>(`/api/users/admin/roles/${id}`).then((res) => res.data.data);
  },
  create(payload: RoleCreatePayload) {
    return http.post<ApiResponse<RoleResponse>>("/api/users/admin/roles", payload).then((res) => res.data.data);
  },
  update(id: string, payload: RoleUpdatePayload) {
    return http.put<ApiResponse<RoleResponse>>(`/api/users/admin/roles/${id}`, payload).then((res) => res.data.data);
  },
  remove(id: string) {
    return http.delete(`/api/users/admin/roles/${id}`);
  },
  /** Thay toan bo tap permission cua role trong 1 request - xem AdminRoleController.setPermissions. */
  setPermissions(id: string, payload: SetRolePermissionsPayload) {
    return http.put<ApiResponse<RoleDetailResponse>>(`/api/users/admin/roles/${id}/permissions`, payload).then((res) => res.data.data);
  },
  listPermissions() {
    return http.get<ApiResponse<PermissionResponse[]>>("/api/users/admin/permissions").then((res) => res.data.data);
  },
};
