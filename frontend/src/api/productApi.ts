import { http } from "@/api/http";
import type { ApiResponse, PageData } from "@/types/api";
import type {
  CategoryResponse,
  ProductCreatePayload,
  ProductListParams,
  ProductResponse,
  ProductUpdatePayload,
} from "@/types/product";

export const productApi = {
  list(params: ProductListParams = {}) {
    return http
      .get<ApiResponse<PageData<ProductResponse>>>("/api/products", { params })
      .then((res) => res.data.data);
  },
  get(id: string) {
    return http.get<ApiResponse<ProductResponse>>(`/api/products/${id}`).then((res) => res.data.data);
  },
  listCategories() {
    return http.get<ApiResponse<CategoryResponse[]>>("/api/categories").then((res) => res.data.data);
  },
  create(payload: ProductCreatePayload) {
    return http.post<ApiResponse<ProductResponse>>("/api/products", payload).then((res) => res.data.data);
  },
  update(id: string, payload: ProductUpdatePayload) {
    return http.put<ApiResponse<ProductResponse>>(`/api/products/${id}`, payload).then((res) => res.data.data);
  },
  remove(id: string) {
    return http.delete(`/api/products/${id}`);
  },
};
