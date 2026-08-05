import { http } from "@/api/http";
import type { ApiResponse } from "@/types/api";
import type { AttributeValuePayload, AttributeValueResponse, ProductAttributePayload, ProductAttributeResponse } from "@/types/product";

export const attributeApi = {
  list() {
    return http.get<ApiResponse<ProductAttributeResponse[]>>("/api/v1/admin/product-attributes").then((res) => res.data.data);
  },
  create(payload: ProductAttributePayload) {
    return http
      .post<ApiResponse<ProductAttributeResponse>>("/api/v1/admin/product-attributes", payload)
      .then((res) => res.data.data);
  },
  update(id: string, payload: ProductAttributePayload) {
    return http
      .put<ApiResponse<ProductAttributeResponse>>(`/api/v1/admin/product-attributes/${id}`, payload)
      .then((res) => res.data.data);
  },
  listValues(attributeId: string) {
    return http
      .get<ApiResponse<AttributeValueResponse[]>>(`/api/v1/admin/product-attributes/${attributeId}/values`)
      .then((res) => res.data.data);
  },
  createValue(attributeId: string, payload: AttributeValuePayload) {
    return http
      .post<ApiResponse<AttributeValueResponse>>(`/api/v1/admin/product-attributes/${attributeId}/values`, payload)
      .then((res) => res.data.data);
  },
  updateValue(attributeId: string, valueId: string, payload: AttributeValuePayload) {
    return http
      .put<ApiResponse<AttributeValueResponse>>(`/api/v1/admin/product-attributes/${attributeId}/values/${valueId}`, payload)
      .then((res) => res.data.data);
  },
};
