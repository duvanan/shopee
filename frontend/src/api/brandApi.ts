import { http } from "@/api/http";
import type { ApiResponse } from "@/types/api";
import type { BrandPayload, BrandResponse } from "@/types/product";

export const brandApi = {
  list() {
    return http.get<ApiResponse<BrandResponse[]>>("/api/v1/admin/brands").then((res) => res.data.data);
  },
  create(payload: BrandPayload) {
    return http.post<ApiResponse<BrandResponse>>("/api/v1/admin/brands", payload).then((res) => res.data.data);
  },
  update(id: string, payload: BrandPayload) {
    return http.put<ApiResponse<BrandResponse>>(`/api/v1/admin/brands/${id}`, payload).then((res) => res.data.data);
  },
};
