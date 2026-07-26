<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();

function handleLogout() {
  auth.logout();
  router.push({ name: "login" });
}
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <header class="border-b border-neutral-200 bg-white">
      <div class="max-w-5xl mx-auto px-4 h-14 flex items-center justify-between">
        <RouterLink to="/" class="flex items-center gap-2 font-bold text-neutral-900">
          <span class="w-7 h-7 rounded-md bg-brand text-white grid place-items-center text-sm">S</span>
          Shopee Clone
        </RouterLink>

        <nav class="flex items-center gap-4 text-sm">
          <template v-if="auth.isAuthenticated">
            <RouterLink v-if="auth.isAdmin" to="/admin" class="text-neutral-600 hover:text-neutral-900">Quản trị</RouterLink>
            <RouterLink to="/account" class="text-neutral-600 hover:text-neutral-900">{{ auth.user?.fullName ?? "Tài khoản" }}</RouterLink>
            <button type="button" class="text-neutral-500 hover:text-brand" @click="handleLogout">Đăng xuất</button>
          </template>
          <template v-else>
            <RouterLink to="/login" class="text-neutral-600 hover:text-neutral-900">Đăng nhập</RouterLink>
            <RouterLink
              to="/register"
              class="px-3 py-1.5 rounded-md bg-brand text-white font-semibold hover:bg-brand-dark"
            >
              Đăng ký
            </RouterLink>
          </template>
        </nav>
      </div>
    </header>

    <main class="flex-1">
      <RouterView />
    </main>
  </div>
</template>
