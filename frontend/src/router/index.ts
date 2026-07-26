import { createRouter, createWebHistory } from "vue-router";

import { useAuthStore } from "@/stores/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      component: () => import("@/layouts/MainLayout.vue"),
      children: [
        { path: "", name: "home", component: () => import("@/views/HomeView.vue") },
        { path: "products/:id", name: "product-detail", component: () => import("@/views/ProductDetailView.vue") },
        { path: "login", name: "login", component: () => import("@/views/LoginView.vue"), meta: { guestOnly: true } },
        { path: "register", name: "register", component: () => import("@/views/RegisterView.vue"), meta: { guestOnly: true } },
        { path: "account", name: "account", component: () => import("@/views/AccountView.vue"), meta: { requiresAuth: true } },
      ],
    },
    {
      path: "/admin",
      component: () => import("@/layouts/AdminLayout.vue"),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: "", name: "admin-dashboard", component: () => import("@/views/admin/AdminDashboardView.vue") },
        { path: "products", name: "admin-products", component: () => import("@/views/admin/AdminProductListView.vue") },
        { path: "products/new", name: "admin-product-new", component: () => import("@/views/admin/AdminProductFormView.vue") },
        { path: "products/:id/edit", name: "admin-product-edit", component: () => import("@/views/admin/AdminProductFormView.vue") },
        { path: "categories", name: "admin-categories", component: () => import("@/views/admin/AdminCategoryListView.vue") },
        { path: "categories/new", name: "admin-category-new", component: () => import("@/views/admin/AdminCategoryFormView.vue") },
        { path: "categories/:id/edit", name: "admin-category-edit", component: () => import("@/views/admin/AdminCategoryFormView.vue") },
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

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: "home" };
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: "home" };
  }

  return true;
});

export default router;
