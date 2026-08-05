import { http } from "@/api/http";
import type { ApiResponse } from "@/types/api";
import type { CategoryPayload, CategoryResponse } from "@/types/product";

export const categoryApi = {
  list() {
    return http.get<ApiResponse<CategoryResponse[]>>("/api/v1/public/categories").then((res) => res.data.data);
  },
  listAdmin() {
    return http.get<ApiResponse<CategoryResponse[]>>("/api/v1/admin/categories").then((res) => res.data.data);
  },
  create(payload: CategoryPayload) {
    return http.post<ApiResponse<CategoryResponse>>("/api/v1/admin/categories", payload).then((res) => res.data.data);
  },
  update(id: string, payload: CategoryPayload) {
    return http.put<ApiResponse<CategoryResponse>>(`/api/v1/admin/categories/${id}`, payload).then((res) => res.data.data);
  },
  remove(id: string) {
    return http.delete(`/api/v1/admin/categories/${id}`);
  },
};
