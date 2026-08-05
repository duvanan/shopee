<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";

import { useAuthStore } from "@/stores/auth";
import { authApi } from "@/api/authApi";
import { extractErrorMessage } from "@/api/http";
import type { SessionResponse } from "@/types/auth";

const auth = useAuthStore();
const router = useRouter();

const passwordForm = reactive({ currentPassword: "", newPassword: "" });
const passwordFieldErrors = reactive<Record<string, string>>({});
const passwordError = ref("");
const passwordSuccess = ref("");
const isChangingPassword = ref(false);

function validatePasswordForm(): boolean {
  passwordFieldErrors.currentPassword = passwordForm.currentPassword ? "" : "Vui lòng nhập mật khẩu hiện tại";
  passwordFieldErrors.newPassword = passwordForm.newPassword.length >= 6 ? "" : "Mật khẩu mới phải có ít nhất 6 ký tự";
  return !Object.values(passwordFieldErrors).some(Boolean);
}

async function handleChangePassword() {
  passwordError.value = "";
  passwordSuccess.value = "";
  if (!validatePasswordForm()) return;

  isChangingPassword.value = true;
  try {
    await authApi.changePassword({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword });
    passwordSuccess.value = "Đổi mật khẩu thành công, đang chuyển về trang đăng nhập...";
    setTimeout(() => {
      auth.logout();
      router.push({ name: "login" });
    }, 1500);
  } catch (error) {
    passwordError.value = extractErrorMessage(error, "Đổi mật khẩu thất bại.");
  } finally {
    isChangingPassword.value = false;
  }
}

const sessions = ref<SessionResponse[]>([]);
const sessionsLoading = ref(false);
const sessionsError = ref("");

async function fetchSessions() {
  sessionsLoading.value = true;
  sessionsError.value = "";
  try {
    sessions.value = await authApi.listSessions();
  } catch {
    sessionsError.value = "Không tải được danh sách thiết bị.";
  } finally {
    sessionsLoading.value = false;
  }
}

async function revokeSession(sessionId: string) {
  try {
    await authApi.revokeSession(sessionId);
    sessions.value = sessions.value.filter((s) => s.sessionId !== sessionId);
  } catch (error) {
    sessionsError.value = extractErrorMessage(error, "Không đăng xuất được thiết bị này.");
  }
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("vi-VN");
}

onMounted(fetchSessions);
</script>

<template>
  <div class="max-w-lg mx-auto px-4 py-16 space-y-6">
    <div class="bg-white border border-neutral-200 rounded-xl p-7">
      <h1 class="text-lg font-bold text-neutral-900 mb-5">Tài khoản của tôi</h1>

      <dl class="space-y-3 text-sm">
        <div class="flex justify-between">
          <dt class="text-neutral-500">Họ tên</dt>
          <dd class="font-medium text-neutral-900">{{ auth.user?.fullName || "—" }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-neutral-500">Email</dt>
          <dd class="font-medium text-neutral-900">{{ auth.user?.email }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-neutral-500">Số điện thoại</dt>
          <dd class="font-medium text-neutral-900">{{ auth.user?.phone || "—" }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-neutral-500">Vai trò</dt>
          <dd class="font-medium text-neutral-900">{{ auth.user?.roles.join(", ") || "—" }}</dd>
        </div>
      </dl>
    </div>

    <div class="bg-white border border-neutral-200 rounded-xl p-7">
      <h2 class="text-sm font-bold text-neutral-900 mb-4">Đổi mật khẩu</h2>

      <div v-if="passwordError" class="flex gap-2 items-start bg-red-50 border border-red-300 text-red-600 text-sm rounded-lg px-3 py-2.5 mb-4">
        <span aria-hidden="true">⚠</span>
        <span>{{ passwordError }}</span>
      </div>
      <div v-if="passwordSuccess" class="bg-emerald-50 border border-emerald-300 text-emerald-700 text-sm rounded-lg px-3 py-2.5 mb-4">
        {{ passwordSuccess }}
      </div>

      <form class="space-y-3.5" novalidate @submit.prevent="handleChangePassword">
        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Mật khẩu hiện tại</label>
          <input
            v-model="passwordForm.currentPassword"
            type="password"
            autocomplete="current-password"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="passwordFieldErrors.currentPassword ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="passwordFieldErrors.currentPassword" class="text-xs text-red-600 mt-1">{{ passwordFieldErrors.currentPassword }}</p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Mật khẩu mới</label>
          <input
            v-model="passwordForm.newPassword"
            type="password"
            autocomplete="new-password"
            placeholder="Tối thiểu 6 ký tự"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="passwordFieldErrors.newPassword ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="passwordFieldErrors.newPassword" class="text-xs text-red-600 mt-1">{{ passwordFieldErrors.newPassword }}</p>
        </div>

        <button
          type="submit"
          :disabled="isChangingPassword"
          class="w-full rounded-lg bg-brand text-white font-semibold text-sm py-2.5 hover:bg-brand-dark disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
        >
          {{ isChangingPassword ? "Đang đổi..." : "Đổi mật khẩu" }}
        </button>
      </form>
    </div>

    <div class="bg-white border border-neutral-200 rounded-xl p-7">
      <h2 class="text-sm font-bold text-neutral-900 mb-1">Thiết bị đang đăng nhập</h2>
      <p class="text-xs text-neutral-500 mb-4">Đăng xuất từ xa nếu thấy thiết bị lạ.</p>

      <div v-if="sessionsError" class="text-sm text-red-600 bg-red-50 border border-red-300 rounded-lg px-3 py-2.5 mb-3">
        {{ sessionsError }}
      </div>
      <div v-if="sessionsLoading" class="text-sm text-neutral-400">Đang tải...</div>
      <div v-else-if="sessions.length === 0" class="text-sm text-neutral-400">Không có thiết bị nào khác.</div>

      <ul v-else class="space-y-2.5">
        <li
          v-for="session in sessions"
          :key="session.sessionId"
          class="flex items-center justify-between gap-3 border border-neutral-200 rounded-lg px-3.5 py-2.5"
        >
          <div class="min-w-0">
            <p class="text-sm text-neutral-800 truncate">{{ session.userAgent ?? "Không rõ trình duyệt" }}</p>
            <p class="text-xs text-neutral-400">{{ session.ipAddress ?? "—" }} • hoạt động {{ formatDateTime(session.lastUsedAt) }}</p>
          </div>
          <button
            type="button"
            class="text-xs font-semibold text-red-600 hover:text-red-700 shrink-0"
            @click="revokeSession(session.sessionId)"
          >
            Đăng xuất
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>
