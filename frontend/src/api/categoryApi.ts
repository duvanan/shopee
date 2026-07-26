import { http } from "@/api/http";
import type { ApiResponse } from "@/types/api";
import type { CategoryCreatePayload, CategoryResponse, CategoryUpdatePayload } from "@/types/product";

export const categoryApi = {
  list() {
    return http.get<ApiResponse<CategoryResponse[]>>("/api/categories").then((res) => res.data.data);
  },
  create(payload: CategoryCreatePayload) {
    return http.post<ApiResponse<CategoryResponse>>("/api/categories", payload).then((res) => res.data.data);
  },
  update(id: string, payload: CategoryUpdatePayload) {
    return http.put<ApiResponse<CategoryResponse>>(`/api/categories/${id}`, payload).then((res) => res.data.data);
  },
  remove(id: string) {
    return http.delete(`/api/categories/${id}`);
  },
};
