import { http } from "@/api/http";
import type { ApiResponse, PageData } from "@/types/api";
import type {
  AttributeValueGroup,
  ProductCreatePayload,
  ProductDetailResponse,
  ProductListParams,
  ProductSummaryResponse,
  ProductUpdatePayload,
  VariantCombinationResponse,
} from "@/types/product";

export const productApi = {
  list(params: ProductListParams = {}) {
    return http
      .get<ApiResponse<PageData<ProductSummaryResponse>>>("/api/v1/public/products", { params })
      .then((res) => res.data.data);
  },
  getBySlug(slug: string) {
    return http
      .get<ApiResponse<ProductDetailResponse>>(`/api/v1/public/products/${slug}`)
      .then((res) => res.data.data);
  },
  listAdmin(params: { search?: string; page?: number; size?: number } = {}) {
    return http
      .get<ApiResponse<PageData<ProductSummaryResponse>>>("/api/v1/admin/products", { params })
      .then((res) => res.data.data);
  },
  getById(id: string) {
    return http.get<ApiResponse<ProductDetailResponse>>(`/api/v1/admin/products/${id}`).then((res) => res.data.data);
  },
  create(payload: ProductCreatePayload) {
    return http.post<ApiResponse<ProductDetailResponse>>("/api/v1/admin/products", payload).then((res) => res.data.data);
  },
  update(id: string, payload: ProductUpdatePayload) {
    return http.put<ApiResponse<ProductDetailResponse>>(`/api/v1/admin/products/${id}`, payload).then((res) => res.data.data);
  },
  remove(id: string) {
    return http.delete(`/api/v1/admin/products/${id}`);
  },
  generateVariantPreview(attributeGroups: AttributeValueGroup[]) {
    return http
      .post<ApiResponse<VariantCombinationResponse[]>>("/api/v1/admin/products/variants/generate-preview", { attributeGroups })
      .then((res) => res.data.data);
  },
};
