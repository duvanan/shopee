// Anh xa permission code -> section admin tuong ung, dung de:
//  - Cho router guard biet route con nao can permission gi (xem router/index.ts)
//  - Cho AdminLayout an/hien muc sidebar theo dung permission user dang co
// Khop voi api-gateway.yml (permission-required cho tung endpoint ghi).
// Them man admin moi thi chi can khai o day, khong phai sua logic guard/sidebar.

export const PRODUCT_PERMISSIONS = ["PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_DELETE"] as const;
export const CATEGORY_PERMISSIONS = ["CATEGORY_MANAGE"] as const;
export const USER_PERMISSIONS = ["USER_MANAGE"] as const;
export const ROLE_PERMISSIONS = ["ROLE_MANAGE"] as const;
export const AUDIT_LOG_PERMISSIONS = ["AUDIT_LOG_VIEW"] as const;

/** Tat ca permission dang co man admin thuc su dung toi - can it nhat 1 cai de vao duoc /admin. */
export const ADMIN_ACCESS_PERMISSIONS: string[] = [
  ...PRODUCT_PERMISSIONS,
  ...CATEGORY_PERMISSIONS,
  ...USER_PERMISSIONS,
  ...ROLE_PERMISSIONS,
  ...AUDIT_LOG_PERMISSIONS,
];

export function hasAnyPermission(userPermissions: string[], required: readonly string[]): boolean {
  return required.some((code) => userPermissions.includes(code));
}
