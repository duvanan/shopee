<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import Button from "@/components/ui/Button.vue";
import ConfirmDialog from "@/components/ui/ConfirmDialog.vue";
import { categoryApi } from "@/api/categoryApi";
import { extractErrorMessage } from "@/api/http";
import type { CategoryResponse } from "@/types/product";

const categories = ref<CategoryResponse[]>([]);
const isLoading = ref(false);
const errorMessage = ref("");

async function fetchCategories() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    categories.value = await categoryApi.list();
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
      <div class="flex items-center justify-between px-5 py-4 border-b border-neutral-200 flex-wrap gap-3">
        <div>
          <h1 class="text-sm font-bold text-neutral-900">Danh mục</h1>
          <p class="text-xs text-neutral-500 mt-0.5">{{ categories.length }} danh mục • không phân trang</p>
        </div>
        <RouterLink :to="{ name: 'admin-category-new' }">
          <Button variant="primary">+ Thêm danh mục</Button>
        </RouterLink>
      </div>

      <div v-if="errorMessage" class="p-6 text-sm text-red-600">{{ errorMessage }}</div>
      <div v-else-if="isLoading" class="p-6 text-sm text-neutral-400">Đang tải...</div>
      <div v-else-if="categories.length === 0" class="p-10 text-center text-sm text-neutral-500">Chưa có danh mục nào.</div>

      <table v-else class="w-full text-sm">
        <thead>
          <tr class="text-left text-[11px] uppercase tracking-wide text-neutral-400">
            <th class="px-5 py-2.5 font-bold">Tên danh mục</th>
            <th class="px-5 py-2.5 font-bold">Slug</th>
            <th class="px-5 py-2.5"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="category in categories" :key="category.id" class="border-t border-neutral-100 hover:bg-neutral-50">
            <td class="px-5 py-3 text-neutral-800">{{ category.name }}</td>
            <td class="px-5 py-3 text-neutral-500">{{ category.slug ?? "—" }}</td>
            <td class="px-5 py-3">
              <div class="flex justify-end gap-2">
                <RouterLink :to="{ name: 'admin-category-edit', params: { id: category.id } }">
                  <Button variant="secondary">Sửa</Button>
                </RouterLink>
                <Button variant="danger" @click="askDelete(category)">Xoá</Button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
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
