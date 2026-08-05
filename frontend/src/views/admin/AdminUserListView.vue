<script setup lang="ts">
import { onMounted, reactive, ref, watch } from "vue";
import { RouterLink } from "vue-router";

import AdminPageHeader from "@/components/ui/AdminPageHeader.vue";
import Badge from "@/components/ui/Badge.vue";
import Button from "@/components/ui/Button.vue";
import ConfirmDialog from "@/components/ui/ConfirmDialog.vue";
import DataTable, { type DataTableColumn } from "@/components/ui/DataTable.vue";
import Icon from "@/components/ui/Icon.vue";
import Pagination from "@/components/ui/Pagination.vue";
import { adminUserApi } from "@/api/adminUserApi";
import { extractErrorMessage } from "@/api/http";
import type { PageData } from "@/types/api";
import type { AdminUserResponse } from "@/types/admin";

const PAGE_SIZE = 10;

const columns: DataTableColumn[] = [
  { key: "fullName", label: "Người dùng" },
  { key: "roles", label: "Vai trò" },
  { key: "status", label: "Trạng thái" },
  { key: "lastLoginAt", label: "Đăng nhập gần nhất" },
];

const users = ref<PageData<AdminUserResponse> | null>(null);
const isLoading = ref(false);
const errorMessage = ref("");

const filters = reactive({ search: "", page: 0 });
let searchDebounce: ReturnType<typeof setTimeout> | undefined;

async function fetchUsers() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    users.value = await adminUserApi.list({ search: filters.search || undefined, page: filters.page + 1, size: PAGE_SIZE });
  } catch {
    errorMessage.value = "Không tải được danh sách người dùng. Vui lòng thử lại.";
  } finally {
    isLoading.value = false;
  }
}

watch(
  () => filters.search,
  () => {
    clearTimeout(searchDebounce);
    searchDebounce = setTimeout(() => {
      filters.page = 0;
      fetchUsers();
    }, 350);
  },
);
watch(() => filters.page, fetchUsers);
onMounted(fetchUsers);

function formatDate(iso: string | null): string {
  return iso ? new Date(iso).toLocaleDateString("vi-VN") : "—";
}

const toggleTarget = ref<AdminUserResponse | null>(null);
const toggleError = ref<string | null>(null);
const isToggling = ref(false);

function askToggle(user: AdminUserResponse) {
  toggleTarget.value = user;
  toggleError.value = null;
}

async function confirmToggle() {
  if (!toggleTarget.value) return;
  isToggling.value = true;
  try {
    if (toggleTarget.value.status === "ACTIVE") {
      await adminUserApi.lock(toggleTarget.value.id);
    } else {
      await adminUserApi.unlock(toggleTarget.value.id);
    }
    toggleTarget.value = null;
    await fetchUsers();
  } catch (error) {
    toggleError.value = extractErrorMessage(error, "Không thực hiện được thao tác này.");
  } finally {
    isToggling.value = false;
  }
}
</script>

<template>
  <div>
    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <AdminPageHeader title="Người dùng" :subtitle="users ? `${users.totalElements} người dùng` : undefined">
        <RouterLink :to="{ name: 'admin-user-new' }">
          <Button variant="primary"><Icon name="plus" class="w-4 h-4" /> Thêm người dùng</Button>
        </RouterLink>
      </AdminPageHeader>

      <div class="px-5 py-3.5 border-b border-neutral-200 bg-neutral-50">
        <div class="relative w-72">
          <Icon name="search" class="w-4 h-4 absolute left-2.5 top-1/2 -translate-y-1/2 text-neutral-400" />
          <input
            v-model="filters.search"
            type="search"
            placeholder="Tìm theo email hoặc họ tên..."
            class="w-full rounded-lg border border-neutral-300 pl-8 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          />
        </div>
      </div>

      <DataTable :columns="columns" :items="users?.items ?? []" :loading="isLoading" :error="errorMessage" empty-text="Không tìm thấy người dùng nào." has-actions>
        <template #cell-fullName="{ item }">
          <div class="text-neutral-800 font-medium">{{ item.fullName }}</div>
          <div class="text-xs text-neutral-500">{{ item.email }}</div>
        </template>
        <template #cell-roles="{ item }">
          <span class="text-neutral-600">{{ item.roles.join(", ") || "—" }}</span>
        </template>
        <template #cell-status="{ item }">
          <Badge :variant="item.status === 'ACTIVE' ? 'success' : 'danger'">{{ item.status === "ACTIVE" ? "Hoạt động" : "Đã khóa" }}</Badge>
        </template>
        <template #cell-lastLoginAt="{ item }">
          <span class="text-neutral-500">{{ formatDate(item.lastLoginAt) }}</span>
        </template>
        <template #actions="{ item }">
          <RouterLink :to="{ name: 'admin-user-edit', params: { id: item.id } }">
            <Button variant="secondary"><Icon name="edit" class="w-3.5 h-3.5" /> Sửa</Button>
          </RouterLink>
          <Button :variant="item.status === 'ACTIVE' ? 'danger' : 'primary'" @click="askToggle(item)">
            <Icon :name="item.status === 'ACTIVE' ? 'lock' : 'unlock'" class="w-3.5 h-3.5" />
            {{ item.status === "ACTIVE" ? "Khóa" : "Mở khóa" }}
          </Button>
        </template>
      </DataTable>

      <div v-if="users && users.totalPages > 1" class="px-5 py-4 border-t border-neutral-200">
        <Pagination v-model:current-page="filters.page" :total-pages="users.totalPages" />
      </div>
    </div>

    <ConfirmDialog
      :open="toggleTarget !== null"
      :title="toggleTarget?.status === 'ACTIVE' ? `Khóa tài khoản '${toggleTarget?.email}'?` : `Mở khóa tài khoản '${toggleTarget?.email}'?`"
      :confirm-label="toggleTarget?.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'"
      :error-message="toggleError"
      :loading="isToggling"
      @cancel="toggleTarget = null"
      @confirm="confirmToggle"
    />
  </div>
</template>
