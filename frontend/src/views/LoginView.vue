<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const form = reactive({
  email: "",
  password: "",
});

const isSubmitting = ref(false);
const errorMessage = ref("");

async function handleSubmit() {
  errorMessage.value = "";
  isSubmitting.value = true;
  try {
    await auth.login({ email: form.email, password: form.password });
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/";
    router.push(redirect);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Đăng nhập thất bại.";
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="max-w-sm mx-auto px-4 py-16">
    <div class="bg-white border border-neutral-200 rounded-xl p-7">
      <h1 class="text-lg font-bold text-neutral-900">Đăng nhập</h1>
      <p class="text-sm text-neutral-500 mt-1 mb-5">Nhập email và mật khẩu để tiếp tục</p>

      <div v-if="errorMessage" class="flex gap-2 items-start bg-red-50 border border-red-300 text-red-600 text-sm rounded-lg px-3 py-2.5 mb-4">
        <span aria-hidden="true">⚠</span>
        <span>{{ errorMessage }}</span>
      </div>

      <form class="space-y-3.5" novalidate @submit.prevent="handleSubmit">
        <div>
          <label for="email" class="block text-xs font-semibold text-neutral-600 mb-1.5">Email</label>
          <input
            id="email"
            v-model="form.email"
            type="email"
            required
            autocomplete="email"
            placeholder="ban@example.com"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          />
        </div>

        <div>
          <label for="password" class="block text-xs font-semibold text-neutral-600 mb-1.5">Mật khẩu</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            required
            autocomplete="current-password"
            placeholder="••••••••"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          />
        </div>

        <button
          type="submit"
          :disabled="isSubmitting"
          class="w-full rounded-lg bg-brand text-white font-semibold text-sm py-2.5 mt-1 hover:bg-brand-dark disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
        >
          {{ isSubmitting ? "Đang đăng nhập…" : "Đăng nhập" }}
        </button>
      </form>

      <p class="text-center text-sm text-neutral-500 mt-5">
        Chưa có tài khoản?
        <RouterLink to="/register" class="text-brand font-semibold hover:underline">Đăng ký</RouterLink>
      </p>
    </div>
  </div>
</template>
