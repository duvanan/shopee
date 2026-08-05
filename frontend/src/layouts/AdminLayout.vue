<script setup lang="ts">
import { computed } from "vue";
import { RouterLink, RouterView, useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import Icon, { type IconName } from "@/components/ui/Icon.vue";
import {
  AUDIT_LOG_PERMISSIONS,
  CATEGORY_PERMISSIONS,
  hasAnyPermission,
  PRODUCT_PERMISSIONS,
  ROLE_PERMISSIONS,
  USER_PERMISSIONS,
} from "@/constants/permissions";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

interface NavItem {
  name: string;
  label: string;
  icon: IconName;
  /** undefined = luon hien (VD Tổng quan), khong can permission gi ca. */
  permissions?: readonly string[];
}

const allSalesNavItems: NavItem[] = [
  { name: "admin-dashboard", label: "Tổng quan", icon: "grid" },
  { name: "admin-products", label: "Sản phẩm", icon: "box", permissions: PRODUCT_PERMISSIONS },
  { name: "admin-categories", label: "Danh mục", icon: "tag", permissions: CATEGORY_PERMISSIONS },
  { name: "admin-brands", label: "Thương hiệu", icon: "tag", permissions: PRODUCT_PERMISSIONS },
  { name: "admin-attributes", label: "Thuộc tính", icon: "filter", permissions: PRODUCT_PERMISSIONS },
  { name: "admin-product-types", label: "Loại sản phẩm", icon: "grid", permissions: PRODUCT_PERMISSIONS },
  { name: "admin-attribute-sets", label: "Bộ thuộc tính", icon: "clipboard", permissions: PRODUCT_PERMISSIONS },
];

const allOperationsNavItems: NavItem[] = [
  { name: "admin-users", label: "Người dùng", icon: "users", permissions: USER_PERMISSIONS },
  { name: "admin-roles", label: "Phân quyền", icon: "shield", permissions: ROLE_PERMISSIONS },
];

const allSystemNavItems: NavItem[] = [{ name: "admin-audit-logs", label: "Nhật ký", icon: "clipboard", permissions: AUDIT_LOG_PERMISSIONS }];

const comingSoonItems = ["Đơn hàng"];

// Chi hien muc sidebar user THUC SU dung toi duoc - role tuy bien (VD STAFF)
// chi duoc gan vai permission van thay dung phan viec cua minh, khong bi
// ngop giua nhung man khong co quyen bam vao.
function visible(items: NavItem[]): NavItem[] {
  return items.filter((item) => !item.permissions || hasAnyPermission(auth.permissions, item.permissions));
}

const salesNavItems = computed(() => visible(allSalesNavItems));
const operationsNavItems = computed(() => visible(allOperationsNavItems));
const systemNavItems = computed(() => visible(allSystemNavItems));

function isActive(name: string): boolean {
  return route.matched.some((record) => record.name === name);
}

function handleLogout() {
  auth.logout();
  router.push({ name: "login" });
}
</script>

<template>
  <div class="min-h-screen flex bg-neutral-50">
    <aside class="w-56 shrink-0 bg-white border-r border-neutral-200 flex flex-col">
      <RouterLink to="/" class="flex items-center gap-2 px-5 h-14 border-b border-neutral-200 font-bold text-neutral-900">
        <span class="w-7 h-7 rounded-md bg-brand text-white grid place-items-center text-sm">S</span>
        <span>
          Shopee Clone
          <span class="block text-[11px] font-medium text-neutral-400 -mt-0.5">Bảng quản trị</span>
        </span>
      </RouterLink>

      <nav v-if="salesNavItems.length > 0" class="p-3 space-y-0.5">
        <p class="px-2 pb-1.5 text-[11px] font-bold uppercase tracking-wide text-neutral-400">Sàn thương mại</p>
        <RouterLink
          v-for="item in salesNavItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium"
          :class="isActive(item.name) ? 'bg-brand-soft text-brand-dark font-semibold' : 'text-neutral-700 hover:bg-neutral-100'"
        >
          <Icon :name="item.icon" class="w-4 h-4 shrink-0" />
          {{ item.label }}
        </RouterLink>
      </nav>

      <nav v-if="operationsNavItems.length > 0 || comingSoonItems.length > 0" class="p-3 space-y-0.5">
        <p class="px-2 pb-1.5 text-[11px] font-bold uppercase tracking-wide text-neutral-400">Vận hành</p>
        <RouterLink
          v-for="item in operationsNavItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium"
          :class="isActive(item.name) ? 'bg-brand-soft text-brand-dark font-semibold' : 'text-neutral-700 hover:bg-neutral-100'"
        >
          <Icon :name="item.icon" class="w-4 h-4 shrink-0" />
          {{ item.label }}
        </RouterLink>
        <div
          v-for="label in comingSoonItems"
          :key="label"
          class="flex items-center justify-between px-3 py-2 rounded-lg text-sm text-neutral-400 cursor-not-allowed"
        >
          <span>{{ label }}</span>
          <span class="text-[10px] font-bold border border-dashed border-neutral-300 rounded-full px-2 py-0.5">Sắp ra mắt</span>
        </div>
      </nav>

      <nav v-if="systemNavItems.length > 0" class="p-3 space-y-0.5">
        <p class="px-2 pb-1.5 text-[11px] font-bold uppercase tracking-wide text-neutral-400">Hệ thống</p>
        <RouterLink
          v-for="item in systemNavItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium"
          :class="isActive(item.name) ? 'bg-brand-soft text-brand-dark font-semibold' : 'text-neutral-700 hover:bg-neutral-100'"
        >
          <Icon :name="item.icon" class="w-4 h-4 shrink-0" />
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="mt-auto p-4 border-t border-neutral-200 text-[11px] text-neutral-400">
        Đăng nhập với vai trò {{ auth.user?.roles.join(", ") ?? "—" }}
      </div>
    </aside>

    <div class="flex-1 min-w-0 flex flex-col">
      <header class="h-14 border-b border-neutral-200 bg-white flex items-center justify-between px-6 sticky top-0 z-10">
        <div class="text-sm font-semibold text-neutral-900">Bảng quản trị</div>
        <div class="flex items-center gap-4 text-sm">
          <RouterLink to="/" class="flex items-center gap-1.5 text-neutral-500 hover:text-neutral-900">
            <Icon name="arrow-left" class="w-4 h-4" /> Về trang chủ
          </RouterLink>
          <span class="text-neutral-700">{{ auth.user?.fullName ?? "Admin" }}</span>
          <button type="button" class="flex items-center gap-1.5 text-neutral-500 hover:text-brand" @click="handleLogout">
            <Icon name="logout" class="w-4 h-4" /> Đăng xuất
          </button>
        </div>
      </header>

      <main class="flex-1 p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
