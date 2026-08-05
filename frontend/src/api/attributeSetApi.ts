import { http } from "@/api/http";
import type { ApiResponse } from "@/types/api";
import type {
  AttributeSchemaResponse,
  AttributeSetPayload,
  AttributeSetResponse,
  CloneAttributeSetPayload,
  SetAttributeSetSchemaPayload,
} from "@/types/product";

export const attributeSetApi = {
  list() {
    return http.get<ApiResponse<AttributeSetResponse[]>>("/api/v1/admin/attribute-sets").then((res) => res.data.data);
  },
  get(id: string) {
    return http.get<ApiResponse<AttributeSetResponse>>(`/api/v1/admin/attribute-sets/${id}`).then((res) => res.data.data);
  },
  create(payload: AttributeSetPayload) {
    return http.post<ApiResponse<AttributeSetResponse>>("/api/v1/admin/attribute-sets", payload).then((res) => res.data.data);
  },
  update(id: string, payload: AttributeSetPayload) {
    return http.put<ApiResponse<AttributeSetResponse>>(`/api/v1/admin/attribute-sets/${id}`, payload).then((res) => res.data.data);
  },
  remove(id: string) {
    return http.delete(`/api/v1/admin/attribute-sets/${id}`);
  },
  clone(id: string, payload: CloneAttributeSetPayload) {
    return http.post<ApiResponse<AttributeSetResponse>>(`/api/v1/admin/attribute-sets/${id}/clone`, payload).then((res) => res.data.data);
  },
  getSchema(id: string) {
    return http.get<ApiResponse<AttributeSchemaResponse>>(`/api/v1/admin/attribute-sets/${id}/schema`).then((res) => res.data.data);
  },
  setSchema(id: string, payload: SetAttributeSetSchemaPayload) {
    return http
      .put<ApiResponse<AttributeSchemaResponse>>(`/api/v1/admin/attribute-sets/${id}/schema`, payload)
      .then((res) => res.data.data);
  },
};
