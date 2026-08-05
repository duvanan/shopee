<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";

import AdminPageHeader from "@/components/ui/AdminPageHeader.vue";
import Button from "@/components/ui/Button.vue";
import DataTable, { type DataTableColumn } from "@/components/ui/DataTable.vue";
import Icon from "@/components/ui/Icon.vue";
import { brandApi } from "@/api/brandApi";
import { extractErrorMessage } from "@/api/http";
import type { BrandResponse } from "@/types/product";

const columns: DataTableColumn[] = [
  { key: "name", label: "Thương hiệu" },
  { key: "code", label: "Mã" },
  { key: "slug", label: "Slug" },
  { key: "status", label: "Trạng thái" },
];

const brands = ref<BrandResponse[]>([]);
const isLoading = ref(false);
const errorMessage = ref("");

async function fetchBrands() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    brands.value = await brandApi.list();
  } catch {
    errorMessage.value = "Không tải được danh sách thương hiệu. Vui lòng thử lại.";
  } finally {
    isLoading.value = false;
  }
}

onMounted(fetchBrands);

const showForm = ref(false);
const editingId = ref<string | null>(null);
const isSubmitting = ref(false);
const formError = ref("");
const form = reactive({ code: "", name: "", slug: "" });

function openCreate() {
  editingId.value = null;
  form.code = "";
  form.name = "";
  form.slug = "";
  formError.value = "";
  showForm.value = true;
}

function openEdit(brand: BrandResponse) {
  editingId.value = brand.id;
  form.code = brand.code;
  form.name = brand.name;
  form.slug = brand.slug;
  formError.value = "";
  showForm.value = true;
}

async function handleSubmit() {
  if (!form.code.trim() || !form.name.trim() || !form.slug.trim()) {
    formError.value = "Vui lòng nhập đầy đủ mã, tên và slug.";
    return;
  }
  isSubmitting.value = true;
  formError.value = "";
  try {
    const payload = { code: form.code.trim(), name: form.name.trim(), slug: form.slug.trim() };
    if (editingId.value) {
      await brandApi.update(editingId.value, payload);
    } else {
      await brandApi.create(payload);
    }
    showForm.value = false;
    await fetchBrands();
  } catch (error) {
    formError.value = extractErrorMessage(error, "Không lưu được thương hiệu.");
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
    <AdminPageHeader title="Thương hiệu" :subtitle="`${brands.length} thương hiệu`">
      <Button variant="primary" @click="openCreate"><Icon name="plus" class="w-4 h-4" /> Thêm thương hiệu</Button>
    </AdminPageHeader>

    <form v-if="showForm" class="px-5 py-4 border-b border-neutral-200 bg-neutral-50 space-y-3" novalidate @submit.prevent="handleSubmit">
      <div v-if="formError" class="text-xs text-red-600">{{ formError }}</div>
      <div class="grid grid-cols-3 gap-3">
        <input v-model="form.code" placeholder="Mã (VD: NIKE)" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
        <input v-model="form.name" placeholder="Tên thương hiệu" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
        <input v-model="form.slug" placeholder="Slug" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
      </div>
      <div class="flex gap-2">
        <Button type="submit" variant="primary" :disabled="isSubmitting">{{ isSubmitting ? "Đang lưu..." : "Lưu" }}</Button>
        <Button variant="secondary" @click="showForm = false">Huỷ</Button>
      </div>
    </form>

    <DataTable :columns="columns" :items="brands" :loading="isLoading" :error="errorMessage" empty-text="Chưa có thương hiệu nào." has-actions>
      <template #cell-name="{ item }"><span class="text-neutral-800">{{ item.name }}</span></template>
      <template #cell-code="{ item }"><span class="text-neutral-500 font-mono text-xs">{{ item.code }}</span></template>
      <template #cell-slug="{ item }"><span class="text-neutral-500">{{ item.slug }}</span></template>
      <template #cell-status="{ item }"><span class="text-neutral-500">{{ item.status }}</span></template>
      <template #actions="{ item }">
        <Button variant="secondary" @click="openEdit(item)"><Icon name="edit" class="w-3.5 h-3.5" /> Sửa</Button>
      </template>
    </DataTable>
  </div>
</template>
