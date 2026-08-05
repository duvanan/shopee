import { http } from "@/api/http";
import type { ApiResponse } from "@/types/api";
import type { AttributeSchemaResponse, ProductTypePayload, ProductTypeResponse } from "@/types/product";

export const productTypeApi = {
  list() {
    return http.get<ApiResponse<ProductTypeResponse[]>>("/api/v1/admin/product-types").then((res) => res.data.data);
  },
  get(id: string) {
    return http.get<ApiResponse<ProductTypeResponse>>(`/api/v1/admin/product-types/${id}`).then((res) => res.data.data);
  },
  create(payload: ProductTypePayload) {
    return http.post<ApiResponse<ProductTypeResponse>>("/api/v1/admin/product-types", payload).then((res) => res.data.data);
  },
  update(id: string, payload: ProductTypePayload) {
    return http.put<ApiResponse<ProductTypeResponse>>(`/api/v1/admin/product-types/${id}`, payload).then((res) => res.data.data);
  },
  remove(id: string) {
    return http.delete(`/api/v1/admin/product-types/${id}`);
  },
  activate(id: string) {
    return http.post(`/api/v1/admin/product-types/${id}/activate`);
  },
  deactivate(id: string) {
    return http.post(`/api/v1/admin/product-types/${id}/deactivate`);
  },
  getAttributeSchema(id: string) {
    return http
      .get<ApiResponse<AttributeSchemaResponse>>(`/api/v1/admin/product-types/${id}/attribute-schema`)
      .then((res) => res.data.data);
  },
};
