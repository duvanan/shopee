import { http } from "@/api/http";
import type { ApiResponse, PageData } from "@/types/api";
import type {
  AdminCreateUserPayload,
  AdminUpdateUserPayload,
  AdminUserResponse,
  AssignRolePayload,
} from "@/types/admin";

export interface AdminUserListParams {
  search?: string;
  page?: number;
  size?: number;
}

export const adminUserApi = {
  list(params: AdminUserListParams = {}) {
    return http
      .get<ApiResponse<PageData<AdminUserResponse>>>("/api/users/admin/users", { params })
      .then((res) => res.data.data);
  },
  get(id: string) {
    return http.get<ApiResponse<AdminUserResponse>>(`/api/users/admin/users/${id}`).then((res) => res.data.data);
  },
  create(payload: AdminCreateUserPayload) {
    return http.post<ApiResponse<AdminUserResponse>>("/api/users/admin/users", payload).then((res) => res.data.data);
  },
  update(id: string, payload: AdminUpdateUserPayload) {
    return http.put<ApiResponse<AdminUserResponse>>(`/api/users/admin/users/${id}`, payload).then((res) => res.data.data);
  },
  lock(id: string) {
    return http.post<ApiResponse<void>>(`/api/users/admin/users/${id}/lock`).then((res) => res.data);
  },
  unlock(id: string) {
    return http.post<ApiResponse<void>>(`/api/users/admin/users/${id}/unlock`).then((res) => res.data);
  },
  assignRole(id: string, payload: AssignRolePayload) {
    return http.post<ApiResponse<void>>(`/api/users/admin/users/${id}/roles`, payload).then((res) => res.data);
  },
  removeRole(id: string, roleId: string) {
    return http.delete(`/api/users/admin/users/${id}/roles/${roleId}`);
  },
};
