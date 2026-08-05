<script setup lang="ts">
import { onMounted, reactive, ref, watch } from "vue";

import AdminPageHeader from "@/components/ui/AdminPageHeader.vue";
import Badge from "@/components/ui/Badge.vue";
import DataTable, { type DataTableColumn } from "@/components/ui/DataTable.vue";
import Icon from "@/components/ui/Icon.vue";
import Pagination from "@/components/ui/Pagination.vue";
import { auditLogApi } from "@/api/auditLogApi";
import type { PageData } from "@/types/api";
import type { AuditLogResponse } from "@/types/admin";

const PAGE_SIZE = 20;

const columns: DataTableColumn[] = [
  { key: "createdAt", label: "Thời gian" },
  { key: "actorEmail", label: "Người thực hiện" },
  { key: "action", label: "Hành động" },
  { key: "module", label: "Module" },
  { key: "description", label: "Mô tả" },
  { key: "status", label: "Kết quả" },
];

const logs = ref<PageData<AuditLogResponse> | null>(null);
const isLoading = ref(false);
const errorMessage = ref("");
const filters = reactive({ module: "", page: 0 });

async function fetchLogs() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    logs.value = await auditLogApi.list({ module: filters.module || undefined, page: filters.page + 1, size: PAGE_SIZE });
  } catch {
    errorMessage.value = "Không tải được nhật ký. Vui lòng thử lại.";
  } finally {
    isLoading.value = false;
  }
}

watch(() => filters.module, () => {
  filters.page = 0;
  fetchLogs();
});
watch(() => filters.page, fetchLogs);
onMounted(fetchLogs);

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("vi-VN");
}
</script>

<template>
  <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
    <AdminPageHeader title="Nhật ký thao tác" :subtitle="logs ? `${logs.totalElements} bản ghi` : undefined">
      <div class="relative">
        <Icon name="filter" class="w-4 h-4 absolute left-2.5 top-1/2 -translate-y-1/2 text-neutral-400" />
        <select
          v-model="filters.module"
          class="w-48 rounded-lg border border-neutral-300 pl-8 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
        >
          <option value="">Tất cả module</option>
          <option value="USER">USER</option>
          <option value="ROLE">ROLE</option>
        </select>
      </div>
    </AdminPageHeader>

    <DataTable :columns="columns" :items="logs?.items ?? []" :loading="isLoading" :error="errorMessage" empty-text="Chưa có nhật ký nào.">
      <template #cell-createdAt="{ item }">
        <span class="text-neutral-500 whitespace-nowrap">{{ formatDateTime(item.createdAt) }}</span>
      </template>
      <template #cell-actorEmail="{ item }">
        <span class="text-neutral-700">{{ item.actorEmail ?? "—" }}</span>
      </template>
      <template #cell-action="{ item }">
        <span class="font-mono text-xs text-neutral-700">{{ item.action }}</span>
      </template>
      <template #cell-description="{ item }">
        <span class="text-neutral-700">{{ item.description ?? "—" }}</span>
      </template>
      <template #cell-status="{ item }">
        <Badge :variant="item.status === 'SUCCESS' ? 'success' : 'danger'">{{ item.status === "SUCCESS" ? "Thành công" : "Thất bại" }}</Badge>
      </template>
    </DataTable>

    <div v-if="logs && logs.totalPages > 1" class="px-5 py-4 border-t border-neutral-200">
      <Pagination v-model:current-page="filters.page" :total-pages="logs.totalPages" />
    </div>
  </div>
</template>
