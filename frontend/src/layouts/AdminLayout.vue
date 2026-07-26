<script setup lang="ts">
import { RouterLink, RouterView, useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const navItems = [
  { name: "admin-dashboard", label: "Tổng quan" },
  { name: "admin-products", label: "Sản phẩm" },
  { name: "admin-categories", label: "Danh mục" },
];

const comingSoonItems = ["Người dùng", "Đơn hàng", "Phân quyền"];

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

      <nav class="p-3 space-y-0.5">
        <p class="px-2 pb-1.5 text-[11px] font-bold uppercase tracking-wide text-neutral-400">Sàn thương mại</p>
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="block px-3 py-2 rounded-lg text-sm font-medium"
          :class="isActive(item.name) ? 'bg-brand-soft text-brand-dark font-semibold' : 'text-neutral-700 hover:bg-neutral-100'"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <nav class="p-3 space-y-0.5">
        <p class="px-2 pb-1.5 text-[11px] font-bold uppercase tracking-wide text-neutral-400">Vận hành</p>
        <div
          v-for="label in comingSoonItems"
          :key="label"
          class="flex items-center justify-between px-3 py-2 rounded-lg text-sm text-neutral-400 cursor-not-allowed"
        >
          <span>{{ label }}</span>
          <span class="text-[10px] font-bold border border-dashed border-neutral-300 rounded-full px-2 py-0.5">Sắp ra mắt</span>
        </div>
      </nav>

      <div class="mt-auto p-4 border-t border-neutral-200 text-[11px] text-neutral-400">
        Đăng nhập với vai trò ADMIN
      </div>
    </aside>

    <div class="flex-1 min-w-0 flex flex-col">
      <header class="h-14 border-b border-neutral-200 bg-white flex items-center justify-between px-6 sticky top-0 z-10">
        <div class="text-sm font-semibold text-neutral-900">Bảng quản trị</div>
        <div class="flex items-center gap-4 text-sm">
          <RouterLink to="/" class="text-neutral-500 hover:text-neutral-900">← Về trang chủ</RouterLink>
          <span class="text-neutral-700">{{ auth.user?.fullName ?? "Admin" }}</span>
          <button type="button" class="text-neutral-500 hover:text-brand" @click="handleLogout">Đăng xuất</button>
        </div>
      </header>

      <main class="flex-1 p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
