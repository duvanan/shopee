<script setup lang="ts">
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
</script>

<template>
  <div class="max-w-sm mx-auto px-4 py-16">
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
  </div>
</template>
