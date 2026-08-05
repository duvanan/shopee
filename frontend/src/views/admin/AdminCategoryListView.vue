<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import AdminPageHeader from "@/components/ui/AdminPageHeader.vue";
import Button from "@/components/ui/Button.vue";
import ConfirmDialog from "@/components/ui/ConfirmDialog.vue";
import DataTable, { type DataTableColumn } from "@/components/ui/DataTable.vue";
import Icon from "@/components/ui/Icon.vue";
import { categoryApi } from "@/api/categoryApi";
import { extractErrorMessage } from "@/api/http";
import type { CategoryResponse } from "@/types/product";

const columns: DataTableColumn[] = [
  { key: "name", label: "Tên danh mục" },
  { key: "code", label: "Mã" },
  { key: "parent", label: "Danh mục cha" },
  { key: "slug", label: "Slug" },
];

const categories = ref<CategoryResponse[]>([]);
const isLoading = ref(false);
const errorMessage = ref("");

function parentName(category: CategoryResponse): string {
  if (!category.parentId) return "—";
  return categories.value.find((c) => c.id === category.parentId)?.name ?? "—";
}

async function fetchCategories() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    categories.value = await categoryApi.listAdmin();
  } catch {
    errorMessage.value = "Không tải được danh sách danh mục. Vui lòng thử lại.";
  } finally {
    isLoading.value = false;
  }
}

onMounted(fetchCategories);

const deleteTarget = ref<CategoryResponse | null>(null);
const deleteError = ref<string | null>(null);
const isDeleting = ref(false);

function askDelete(category: CategoryResponse) {
  deleteTarget.value = category;
  deleteError.value = null;
}

async function confirmDelete() {
  if (!deleteTarget.value) return;
  isDeleting.value = true;
  try {
    await categoryApi.remove(deleteTarget.value.id);
    deleteTarget.value = null;
    await fetchCategories();
  } catch (error) {
    deleteError.value = extractErrorMessage(error, "Không xoá được danh mục này.");
  } finally {
    isDeleting.value = false;
  }
}
</script>

<template>
  <div>
    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <AdminPageHeader title="Danh mục" :subtitle="`${categories.length} danh mục • không phân trang`">
        <RouterLink :to="{ name: 'admin-category-new' }">
          <Button variant="primary"><Icon name="plus" class="w-4 h-4" /> Thêm danh mục</Button>
        </RouterLink>
      </AdminPageHeader>

      <DataTable :columns="columns" :items="categories" :loading="isLoading" :error="errorMessage" empty-text="Chưa có danh mục nào." has-actions>
        <template #cell-name="{ item }">
          <span class="text-neutral-800">{{ item.name }}</span>
        </template>
        <template #cell-code="{ item }">
          <span class="text-neutral-500 font-mono text-xs">{{ item.code }}</span>
        </template>
        <template #cell-parent="{ item }">
          <span class="text-neutral-500">{{ parentName(item) }}</span>
        </template>
        <template #cell-slug="{ item }">
          <span class="text-neutral-500">{{ item.slug ?? "—" }}</span>
        </template>
        <template #actions="{ item }">
          <RouterLink :to="{ name: 'admin-category-edit', params: { id: item.id } }">
            <Button variant="secondary"><Icon name="edit" class="w-3.5 h-3.5" /> Sửa</Button>
          </RouterLink>
          <Button variant="danger" @click="askDelete(item)"><Icon name="trash" class="w-3.5 h-3.5" /> Xoá</Button>
        </template>
      </DataTable>
    </div>

    <ConfirmDialog
      :open="deleteTarget !== null"
      :title="`Xoá danh mục '${deleteTarget?.name}'?`"
      :error-message="deleteError"
      :loading="isDeleting"
      @cancel="deleteTarget = null"
      @confirm="confirmDelete"
    />
  </div>
</template>
