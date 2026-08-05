<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import AdminPageHeader from "@/components/ui/AdminPageHeader.vue";
import Badge from "@/components/ui/Badge.vue";
import Button from "@/components/ui/Button.vue";
import ConfirmDialog from "@/components/ui/ConfirmDialog.vue";
import DataTable, { type DataTableColumn } from "@/components/ui/DataTable.vue";
import Icon from "@/components/ui/Icon.vue";
import { adminRoleApi } from "@/api/adminRoleApi";
import { extractErrorMessage } from "@/api/http";
import type { RoleResponse } from "@/types/admin";

const columns: DataTableColumn[] = [
  { key: "role", label: "Role" },
  { key: "description", label: "Mô tả" },
];

const roles = ref<RoleResponse[]>([]);
const isLoading = ref(false);
const errorMessage = ref("");

async function fetchRoles() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    roles.value = await adminRoleApi.list();
  } catch {
    errorMessage.value = "Không tải được danh sách role. Vui lòng thử lại.";
  } finally {
    isLoading.value = false;
  }
}

onMounted(fetchRoles);

const deleteTarget = ref<RoleResponse | null>(null);
const deleteError = ref<string | null>(null);
const isDeleting = ref(false);

function askDelete(role: RoleResponse) {
  deleteTarget.value = role;
  deleteError.value = null;
}

async function confirmDelete() {
  if (!deleteTarget.value) return;
  isDeleting.value = true;
  try {
    await adminRoleApi.remove(deleteTarget.value.id);
    deleteTarget.value = null;
    await fetchRoles();
  } catch (error) {
    deleteError.value = extractErrorMessage(error, "Không xoá được role này.");
  } finally {
    isDeleting.value = false;
  }
}
</script>

<template>
  <div>
    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <AdminPageHeader title="Phân quyền" :subtitle="`${roles.length} role`">
        <RouterLink :to="{ name: 'admin-role-new' }">
          <Button variant="primary"><Icon name="plus" class="w-4 h-4" /> Thêm role</Button>
        </RouterLink>
      </AdminPageHeader>

      <DataTable :columns="columns" :items="roles" :loading="isLoading" :error="errorMessage" has-actions>
        <template #cell-role="{ item }">
          <div class="flex items-center gap-2.5">
            <span class="w-8 h-8 rounded-lg bg-brand-soft text-brand-dark grid place-items-center shrink-0">
              <Icon name="shield" class="w-4 h-4" />
            </span>
            <div>
              <div class="text-neutral-800 font-medium">{{ item.name }}</div>
              <div class="text-[11px] text-neutral-400 font-mono">{{ item.code }}</div>
            </div>
            <Badge v-if="item.isSystem" variant="neutral">Hệ thống</Badge>
          </div>
        </template>
        <template #cell-description="{ item }">
          <span class="text-neutral-500">{{ item.description ?? "—" }}</span>
        </template>
        <template #actions="{ item }">
          <RouterLink :to="{ name: 'admin-role-edit', params: { id: item.id } }">
            <Button variant="secondary"><Icon name="edit" class="w-3.5 h-3.5" /> Sửa</Button>
          </RouterLink>
          <Button v-if="!item.isSystem" variant="danger" @click="askDelete(item)"><Icon name="trash" class="w-3.5 h-3.5" /> Xoá</Button>
        </template>
      </DataTable>
    </div>

    <ConfirmDialog
      :open="deleteTarget !== null"
      :title="`Xoá role '${deleteTarget?.code}'?`"
      :error-message="deleteError"
      :loading="isDeleting"
      @cancel="deleteTarget = null"
      @confirm="confirmDelete"
    />
  </div>
</template>
