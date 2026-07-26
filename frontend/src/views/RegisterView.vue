<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";

import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();

const form = reactive({
  fullName: "",
  email: "",
  password: "",
  phone: "",
});

const fieldErrors = reactive<Record<string, string>>({});
const isSubmitting = ref(false);
const errorMessage = ref("");

function validate(): boolean {
  fieldErrors.fullName = form.fullName.trim() ? "" : "Vui lòng nhập họ tên";
  fieldErrors.email = form.email.trim() ? "" : "Vui lòng nhập email";
  fieldErrors.password = form.password.length >= 6 ? "" : "Mật khẩu phải có ít nhất 6 ký tự";
  return !fieldErrors.fullName && !fieldErrors.email && !fieldErrors.password;
}

async function handleSubmit() {
  errorMessage.value = "";
  if (!validate()) return;

  isSubmitting.value = true;
  try {
    await auth.register({
      fullName: form.fullName,
      email: form.email,
      password: form.password,
      phone: form.phone || undefined,
    });
    router.push("/");
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Đăng ký thất bại.";
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="max-w-sm mx-auto px-4 py-16">
    <div class="bg-white border border-neutral-200 rounded-xl p-7">
      <h1 class="text-lg font-bold text-neutral-900">Tạo tài khoản</h1>
      <p class="text-sm text-neutral-500 mt-1 mb-5">Đăng ký để bắt đầu mua sắm</p>

      <div v-if="errorMessage" class="flex gap-2 items-start bg-red-50 border border-red-300 text-red-600 text-sm rounded-lg px-3 py-2.5 mb-4">
        <span aria-hidden="true">⚠</span>
        <span>{{ errorMessage }}</span>
      </div>

      <form class="space-y-3.5" novalidate @submit.prevent="handleSubmit">
        <div>
          <label for="fullName" class="block text-xs font-semibold text-neutral-600 mb-1.5">Họ và tên</label>
          <input
            id="fullName"
            v-model="form.fullName"
            type="text"
            autocomplete="name"
            placeholder="Nguyễn Văn A"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="fieldErrors.fullName ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.fullName" class="text-xs text-red-600 mt-1">{{ fieldErrors.fullName }}</p>
        </div>

        <div>
          <label for="email" class="block text-xs font-semibold text-neutral-600 mb-1.5">Email</label>
          <input
            id="email"
            v-model="form.email"
            type="email"
            autocomplete="email"
            placeholder="ban@example.com"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="fieldErrors.email ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.email" class="text-xs text-red-600 mt-1">{{ fieldErrors.email }}</p>
        </div>

        <div>
          <label for="password" class="block text-xs font-semibold text-neutral-600 mb-1.5">Mật khẩu</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            placeholder="Tối thiểu 6 ký tự"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="fieldErrors.password ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.password" class="text-xs text-red-600 mt-1">{{ fieldErrors.password }}</p>
        </div>

        <div>
          <label for="phone" class="block text-xs font-semibold text-neutral-600 mb-1.5">
            Số điện thoại <span class="font-normal text-neutral-400">(không bắt buộc)</span>
          </label>
          <input
            id="phone"
            v-model="form.phone"
            type="tel"
            autocomplete="tel"
            placeholder="0900 000 000"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          />
        </div>

        <button
          type="submit"
          :disabled="isSubmitting"
          class="w-full rounded-lg bg-brand text-white font-semibold text-sm py-2.5 mt-1 hover:bg-brand-dark disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
        >
          {{ isSubmitting ? "Đang đăng ký…" : "Đăng ký" }}
        </button>
      </form>

      <p class="text-center text-sm text-neutral-500 mt-5">
        Đã có tài khoản?
        <RouterLink to="/login" class="text-brand font-semibold hover:underline">Đăng nhập</RouterLink>
      </p>
    </div>
  </div>
</template>
