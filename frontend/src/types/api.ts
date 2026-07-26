// Mirror backend/common (com.shopee.common.response) - docs/architecture/api-response-standard.md

export interface FieldErrorDetail {
  field: string;
  message: string;
}

export interface ApiMeta {
  timestamp: string;
  traceId: string | null;
  path: string | null;
}

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  errors: FieldErrorDetail[] | null;
  meta: ApiMeta;
}

export interface PageData<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
