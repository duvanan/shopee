import { createRouter, createWebHistory } from "vue-router";

import { useAuthStore } from "@/stores/auth";
import {
  AUDIT_LOG_PERMISSIONS,
  CATEGORY_PERMISSIONS,
  hasAnyPermission,
  PRODUCT_PERMISSIONS,
  ROLE_PERMISSIONS,
  USER_PERMISSIONS,
} from "@/constants/permissions";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      component: () => import("@/layouts/MainLayout.vue"),
      children: [
        { path: "", name: "home", component: () => import("@/views/HomeView.vue") },
        { path: "products/:slug", name: "product-detail", component: () => import("@/views/ProductDetailView.vue") },
        { path: "login", name: "login", component: () => import("@/views/LoginView.vue"), meta: { guestOnly: true } },
        { path: "register", name: "register", component: () => import("@/views/RegisterView.vue"), meta: { guestOnly: true } },
        { path: "account", name: "account", component: () => import("@/views/AccountView.vue"), meta: { requiresAuth: true } },
      ],
    },
    {
      path: "/admin",
      component: () => import("@/layouts/AdminLayout.vue"),
      // Chi can 1 permission bat ky trong so cac man admin thuc su ton tai la
      // vao duoc - khong con doi hoi ten role phai la "ADMIN" (xem stores/auth.ts).
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: "", name: "admin-dashboard", component: () => import("@/views/admin/AdminDashboardView.vue") },
        {
          path: "products",
          name: "admin-products",
          component: () => import("@/views/admin/AdminProductListView.vue"),
          meta: { permissions: PRODUCT_PERMISSIONS },
        },
        {
          path: "products/new",
          name: "admin-product-new",
          component: () => import("@/views/admin/AdminProductFormView.vue"),
          meta: { permissions: PRODUCT_PERMISSIONS },
        },
        {
          path: "products/:id/edit",
          name: "admin-product-edit",
          component: () => import("@/views/admin/AdminProductFormView.vue"),
          meta: { permissions: PRODUCT_PERMISSIONS },
        },
        {
          path: "categories",
          name: "admin-categories",
          component: () => import("@/views/admin/AdminCategoryListView.vue"),
          meta: { permissions: CATEGORY_PERMISSIONS },
        },
        {
          path: "categories/new",
          name: "admin-category-new",
          component: () => import("@/views/admin/AdminCategoryFormView.vue"),
          meta: { permissions: CATEGORY_PERMISSIONS },
        },
        {
          path: "categories/:id/edit",
          name: "admin-category-edit",
          component: () => import("@/views/admin/AdminCategoryFormView.vue"),
          meta: { permissions: CATEGORY_PERMISSIONS },
        },
        {
          path: "brands",
          name: "admin-brands",
          component: () => import("@/views/admin/AdminBrandListView.vue"),
          meta: { permissions: PRODUCT_PERMISSIONS },
        },
        {
          path: "product-attributes",
          name: "admin-attributes",
          component: () => import("@/views/admin/AdminAttributeListView.vue"),
          meta: { permissions: PRODUCT_PERMISSIONS },
        },
        {
          path: "product-types",
          name: "admin-product-types",
          component: () => import("@/views/admin/AdminProductTypeListView.vue"),
          meta: { permissions: PRODUCT_PERMISSIONS },
        },
        {
          path: "product-types/new",
          name: "admin-product-type-new",
          component: () => import("@/views/admin/AdminProductTypeFormView.vue"),
          meta: { permissions: PRODUCT_PERMISSIONS },
        },
        {
          path: "product-types/:id/edit",
          name: "admin-product-type-edit",
          component: () => import("@/views/admin/AdminProductTypeFormView.vue"),
          meta: { permissions: PRODUCT_PERMISSIONS },
        },
        {
          path: "attribute-sets",
          name: "admin-attribute-sets",
          component: () => import("@/views/admin/AdminAttributeSetListView.vue"),
          meta: { permissions: PRODUCT_PERMISSIONS },
        },
        {
          path: "attribute-sets/:id/schema",
          name: "admin-attribute-set-schema",
          component: () => import("@/views/admin/AdminAttributeSetSchemaView.vue"),
          meta: { permissions: PRODUCT_PERMISSIONS },
        },
        {
          path: "users",
          name: "admin-users",
          component: () => import("@/views/admin/AdminUserListView.vue"),
          meta: { permissions: USER_PERMISSIONS },
        },
        {
          path: "users/new",
          name: "admin-user-new",
          component: () => import("@/views/admin/AdminUserFormView.vue"),
          meta: { permissions: USER_PERMISSIONS },
        },
        {
          path: "users/:id/edit",
          name: "admin-user-edit",
          component: () => import("@/views/admin/AdminUserFormView.vue"),
          meta: { permissions: USER_PERMISSIONS },
        },
        {
          path: "roles",
          name: "admin-roles",
          component: () => import("@/views/admin/AdminRoleListView.vue"),
          meta: { permissions: ROLE_PERMISSIONS },
        },
        {
          path: "roles/new",
          name: "admin-role-new",
          component: () => import("@/views/admin/AdminRoleFormView.vue"),
          meta: { permissions: ROLE_PERMISSIONS },
        },
        {
          path: "roles/:id/edit",
          name: "admin-role-edit",
          component: () => import("@/views/admin/AdminRoleFormView.vue"),
          meta: { permissions: ROLE_PERMISSIONS },
        },
        {
          path: "audit-logs",
          name: "admin-audit-logs",
          component: () => import("@/views/admin/AdminAuditLogListView.vue"),
          meta: { permissions: AUDIT_LOG_PERMISSIONS },
        },
      ],
    },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  await auth.ensureBootstrapped();

  if ((to.meta.requiresAuth || to.meta.requiresAdmin) && !auth.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }

  if (to.meta.requiresAdmin && !auth.canAccessAdmin) {
    return { name: "home" };
  }

  // Da vao duoc /admin nhung khong co permission cho MAN CU THE nay (VD chi
  // co USER_MANAGE ma mo /admin/roles) - dua ve dashboard thay vi ve home,
  // vi ho van hop le trong pham vi admin, chi khong hop le cho man nay.
  const requiredPermissions = to.meta.permissions as readonly string[] | undefined;
  if (requiredPermissions && !hasAnyPermission(auth.permissions, requiredPermissions)) {
    return { name: "admin-dashboard" };
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: "home" };
  }

  return true;
});

export default router;
