export interface CategoryResponse {
  id: string;
  name: string;
  slug: string | null;
}

export interface ProductResponse {
  id: string;
  name: string;
  description: string | null;
  price: number;
  stockQuantity: number;
  imageUrl: string | null;
  categoryId: string | null;
  categoryName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductListParams {
  categoryId?: string;
  search?: string;
  /** 1-based, khop voi quy uoc PageData - xem docs/architecture/api-response-standard.md muc 3. */
  page?: number;
  size?: number;
}

export interface ProductPayload {
  name: string;
  description: string | null;
  price: number;
  stockQuantity: number;
  imageUrl: string | null;
  categoryId: string | null;
}
export type ProductCreatePayload = ProductPayload;
export type ProductUpdatePayload = ProductPayload;

export interface CategoryPayload {
  name: string;
  slug: string | null;
}
export type CategoryCreatePayload = CategoryPayload;
export type CategoryUpdatePayload = CategoryPayload;
