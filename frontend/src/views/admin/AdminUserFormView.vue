<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import Button from "@/components/ui/Button.vue";
import { adminUserApi } from "@/api/adminUserApi";
import { adminRoleApi } from "@/api/adminRoleApi";
import { extractErrorMessage } from "@/api/http";
import type { RoleResponse } from "@/types/admin";

const route = useRoute();
const router = useRouter();

const userId = computed(() => (typeof route.params.id === "string" ? route.params.id : null));
const isEdit = computed(() => userId.value !== null);

const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref("");
const fieldErrors = reactive<Record<string, string>>({});

const form = reactive({
  email: "",
  password: "",
  fullName: "",
  phone: "",
  roleCode: "",
});

const allRoles = ref<RoleResponse[]>([]);
const userRoleCodes = ref<string[]>([]);
const assignRoleCode = ref("");
const roleActionError = ref("");

const availableRolesToAssign = computed(() => allRoles.value.filter((r) => !userRoleCodes.value.includes(r.code)));

function validate(): boolean {
  fieldErrors.fullName = form.fullName.trim() ? "" : "Vui lòng nhập họ tên";
  if (!isEdit.value) {
    fieldErrors.email = form.email.trim() ? "" : "Vui lòng nhập email";
    fieldErrors.password = form.password.length >= 6 ? "" : "Mật khẩu phải có ít nhất 6 ký tự";
  }
  return !Object.values(fieldErrors).some(Boolean);
}

async function loadRoles() {
  try {
    allRoles.value = await adminRoleApi.list();
  } catch {
    allRoles.value = [];
  }
}

async function loadUser() {
  if (!userId.value) return;
  isLoading.value = true;
  try {
    const user = await adminUserApi.get(userId.value);
    form.email = user.email;
    form.fullName = user.fullName;
    form.phone = user.phone ?? "";
    userRoleCodes.value = user.roles;
  } catch {
    errorMessage.value = "Không tải được thông tin người dùng.";
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  loadRoles();
  loadUser();
});

async function handleSubmit() {
  errorMessage.value = "";
  if (!validate()) return;

  isSubmitting.value = true;
  try {
    if (isEdit.value && userId.value) {
      await adminUserApi.update(userId.value, { fullName: form.fullName.trim(), phone: form.phone.trim() || undefined });
      router.push({ name: "admin-users" });
    } else {
      await adminUserApi.create({
        email: form.email.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        phone: form.phone.trim() || undefined,
        roleCode: form.roleCode || undefined,
      });
      router.push({ name: "admin-users" });
    }
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, "Không lưu được người dùng. Vui lòng thử lại.");
  } finally {
    isSubmitting.value = false;
  }
}

async function assignRole() {
  if (!userId.value || !assignRoleCode.value) return;
  roleActionError.value = "";
  try {
    await adminUserApi.assignRole(userId.value, { roleCode: assignRoleCode.value });
    userRoleCodes.value = [...userRoleCodes.value, assignRoleCode.value];
    assignRoleCode.value = "";
  } catch (error) {
    roleActionError.value = extractErrorMessage(error, "Không gán được role này.");
  }
}

async function removeRole(role: RoleResponse) {
  if (!userId.value) return;
  roleActionError.value = "";
  try {
    await adminUserApi.removeRole(userId.value, role.id);
    userRoleCodes.value = userRoleCodes.value.filter((code) => code !== role.code);
  } catch (error) {
    roleActionError.value = extractErrorMessage(error, "Không gỡ được role này.");
  }
}
</script>

<template>
  <div class="max-w-2xl">
    <button type="button" class="text-xs text-neutral-500 hover:text-brand mb-3" @click="router.push({ name: 'admin-users' })">
      ← Quay lại danh sách người dùng
    </button>

    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <div class="px-6 py-4 border-b border-neutral-200">
        <h1 class="text-sm font-bold text-neutral-900">{{ isEdit ? "Sửa người dùng" : "Thêm người dùng" }}</h1>
      </div>

      <div v-if="isLoading" class="p-6 text-sm text-neutral-400">Đang tải...</div>

      <form v-else class="p-6 space-y-4" novalidate @submit.prevent="handleSubmit">
        <div v-if="errorMessage" class="flex gap-2 items-start bg-red-50 border border-red-300 text-red-600 text-sm rounded-lg px-3 py-2.5">
          <span aria-hidden="true">⚠</span>
          <span>{{ errorMessage }}</span>
        </div>

        <div v-if="!isEdit">
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Email</label>
          <input
            v-model="form.email"
            type="email"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="fieldErrors.email ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.email" class="text-xs text-red-600 mt-1">{{ fieldErrors.email }}</p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Họ tên</label>
          <input
            v-model="form.fullName"
            type="text"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="fieldErrors.fullName ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.fullName" class="text-xs text-red-600 mt-1">{{ fieldErrors.fullName }}</p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Số điện thoại</label>
          <input
            v-model="form.phone"
            type="tel"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          />
        </div>

        <div v-if="!isEdit">
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Mật khẩu</label>
          <input
            v-model="form.password"
            type="password"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="fieldErrors.password ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.password" class="text-xs text-red-600 mt-1">{{ fieldErrors.password }}</p>
        </div>

        <div v-if="!isEdit">
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Vai trò ban đầu</label>
          <select
            v-model="form.roleCode"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          >
            <option value="">CUSTOMER (mặc định)</option>
            <option v-for="role in allRoles" :key="role.id" :value="role.code">{{ role.name }} ({{ role.code }})</option>
          </select>
        </div>

        <div class="flex gap-2 pt-2">
          <Button type="submit" variant="primary" :disabled="isSubmitting">
            {{ isSubmitting ? "Đang lưu..." : "Lưu người dùng" }}
          </Button>
          <Button variant="secondary" @click="router.push({ name: 'admin-users' })">Huỷ</Button>
        </div>
      </form>
    </div>

    <div v-if="isEdit && !isLoading" class="bg-white border border-neutral-200 rounded-xl overflow-hidden mt-4">
      <div class="px-6 py-4 border-b border-neutral-200">
        <h2 class="text-sm font-bold text-neutral-900">Vai trò</h2>
      </div>
      <div class="p-6 space-y-4">
        <div v-if="roleActionError" class="text-sm text-red-600 bg-red-50 border border-red-300 rounded-lg px-3 py-2.5">
          {{ roleActionError }}
        </div>

        <div class="flex flex-wrap gap-2">
          <span
            v-for="code in userRoleCodes"
            :key="code"
            class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold bg-brand-soft text-brand-dark"
          >
            {{ code }}
            <button
              type="button"
              class="text-brand-dark hover:text-red-600"
              @click="removeRole(allRoles.find((r) => r.code === code)!)"
              :disabled="!allRoles.find((r) => r.code === code)"
            >
              ✕
            </button>
          </span>
          <span v-if="userRoleCodes.length === 0" class="text-xs text-neutral-400">Chưa có role nào</span>
        </div>

        <div class="flex gap-2">
          <select
            v-model="assignRoleCode"
            class="flex-1 rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          >
            <option value="">— Chọn role để gán —</option>
            <option v-for="role in availableRolesToAssign" :key="role.id" :value="role.code">{{ role.name }} ({{ role.code }})</option>
          </select>
          <Button variant="secondary" :disabled="!assignRoleCode" @click="assignRole">Gán</Button>
        </div>
      </div>
    </div>
  </div>
</template>
