<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";

import AdminPageHeader from "@/components/ui/AdminPageHeader.vue";
import Badge from "@/components/ui/Badge.vue";
import Button from "@/components/ui/Button.vue";
import ConfirmDialog from "@/components/ui/ConfirmDialog.vue";
import DataTable, { type DataTableColumn } from "@/components/ui/DataTable.vue";
import Icon from "@/components/ui/Icon.vue";
import { attributeSetApi } from "@/api/attributeSetApi";
import { extractErrorMessage } from "@/api/http";
import type { AttributeSetResponse } from "@/types/product";

const columns: DataTableColumn[] = [
  { key: "name", label: "Bộ thuộc tính" },
  { key: "code", label: "Mã" },
  { key: "groupCount", label: "Nhóm" },
  { key: "attributeCount", label: "Thuộc tính" },
  { key: "productTypeCount", label: "Loại SP dùng" },
  { key: "version", label: "Phiên bản" },
];

const attributeSets = ref<AttributeSetResponse[]>([]);
const isLoading = ref(false);
const errorMessage = ref("");

async function fetchAttributeSets() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    attributeSets.value = await attributeSetApi.list();
  } catch {
    errorMessage.value = "Không tải được danh sách bộ thuộc tính. Vui lòng thử lại.";
  } finally {
    isLoading.value = false;
  }
}

onMounted(fetchAttributeSets);

// --- Tao bo thuoc tinh moi ---
const showCreateForm = ref(false);
const createForm = reactive({ code: "", name: "", description: "" });
const createError = ref("");
const isCreating = ref(false);

function openCreate() {
  createForm.code = "";
  createForm.name = "";
  createForm.description = "";
  createError.value = "";
  showCreateForm.value = true;
}

async function submitCreate() {
  if (!createForm.code.trim() || !createForm.name.trim()) {
    createError.value = "Vui lòng nhập mã và tên bộ thuộc tính.";
    return;
  }
  isCreating.value = true;
  createError.value = "";
  try {
    await attributeSetApi.create({
      code: createForm.code.trim(),
      name: createForm.name.trim(),
      description: createForm.description.trim() || null,
    });
    showCreateForm.value = false;
    await fetchAttributeSets();
  } catch (error) {
    createError.value = extractErrorMessage(error, "Không tạo được bộ thuộc tính.");
  } finally {
    isCreating.value = false;
  }
}

// --- Nhan ban ---
const cloneTarget = ref<AttributeSetResponse | null>(null);
const cloneForm = reactive({ code: "", name: "" });
const cloneError = ref("");
const isCloning = ref(false);

function openClone(set: AttributeSetResponse) {
  cloneTarget.value = set;
  cloneForm.code = `${set.code}_COPY`;
  cloneForm.name = `${set.name} (bản sao)`;
  cloneError.value = "";
}

async function submitClone() {
  if (!cloneTarget.value) return;
  if (!cloneForm.code.trim() || !cloneForm.name.trim()) {
    cloneError.value = "Vui lòng nhập mã và tên bộ thuộc tính mới.";
    return;
  }
  isCloning.value = true;
  cloneError.value = "";
  try {
    await attributeSetApi.clone(cloneTarget.value.id, { code: cloneForm.code.trim(), name: cloneForm.name.trim() });
    cloneTarget.value = null;
    await fetchAttributeSets();
  } catch (error) {
    cloneError.value = extractErrorMessage(error, "Không nhân bản được bộ thuộc tính.");
  } finally {
    isCloning.value = false;
  }
}

// --- Xoa ---
const deleteTarget = ref<AttributeSetResponse | null>(null);
const deleteError = ref<string | null>(null);
const isDeleting = ref(false);

function askDelete(set: AttributeSetResponse) {
  deleteTarget.value = set;
  deleteError.value = null;
}

async function confirmDelete() {
  if (!deleteTarget.value) return;
  isDeleting.value = true;
  try {
    await attributeSetApi.remove(deleteTarget.value.id);
    deleteTarget.value = null;
    await fetchAttributeSets();
  } catch (error) {
    deleteError.value = extractErrorMessage(error, "Không xoá được bộ thuộc tính này — có thể đang có loại sản phẩm dùng nó.");
  } finally {
    isDeleting.value = false;
  }
}
</script>

<template>
  <div>
    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <AdminPageHeader title="Bộ thuộc tính" :subtitle="`${attributeSets.length} bộ thuộc tính • gán cho loại sản phẩm để sinh form động`">
        <Button variant="primary" @click="openCreate"><Icon name="plus" class="w-4 h-4" /> Thêm bộ thuộc tính</Button>
      </AdminPageHeader>

      <form v-if="showCreateForm" class="px-5 py-4 border-b border-neutral-200 bg-neutral-50 space-y-3" novalidate @submit.prevent="submitCreate">
        <div v-if="createError" class="text-xs text-red-600">{{ createError }}</div>
        <div class="grid grid-cols-3 gap-3">
          <input v-model="createForm.code" placeholder="Mã (VD: SMARTPHONE_DEFAULT)" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
          <input v-model="createForm.name" placeholder="Tên (VD: Bộ thuộc tính điện thoại)" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
          <input v-model="createForm.description" placeholder="Mô tả (không bắt buộc)" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
        </div>
        <div class="flex gap-2">
          <Button type="submit" variant="primary" :disabled="isCreating">{{ isCreating ? "Đang tạo..." : "Tạo" }}</Button>
          <Button type="button" variant="secondary" @click="showCreateForm = false">Huỷ</Button>
        </div>
      </form>

      <DataTable :columns="columns" :items="attributeSets" :loading="isLoading" :error="errorMessage" empty-text="Chưa có bộ thuộc tính nào." has-actions>
        <template #cell-name="{ item }">
          <span class="text-neutral-800">{{ item.name }}</span>
          <Badge v-if="item.isSystem" variant="brand" class="ml-1.5">Hệ thống</Badge>
        </template>
        <template #cell-code="{ item }"><span class="text-neutral-500 font-mono text-xs">{{ item.code }}</span></template>
        <template #cell-groupCount="{ item }"><span class="text-neutral-500">{{ item.groupCount }}</span></template>
        <template #cell-attributeCount="{ item }"><span class="text-neutral-500">{{ item.attributeCount }}</span></template>
        <template #cell-productTypeCount="{ item }"><span class="text-neutral-500">{{ item.productTypeCount }}</span></template>
        <template #cell-version="{ item }"><span class="text-neutral-500">v{{ item.versionNo }}</span></template>
        <template #actions="{ item }">
          <RouterLink :to="{ name: 'admin-attribute-set-schema', params: { id: item.id } }">
            <Button variant="secondary"><Icon name="edit" class="w-3.5 h-3.5" /> Cấu hình</Button>
          </RouterLink>
          <Button variant="secondary" @click="openClone(item)">Nhân bản</Button>
          <Button variant="danger" @click="askDelete(item)"><Icon name="trash" class="w-3.5 h-3.5" /> Xoá</Button>
        </template>
      </DataTable>
    </div>

    <div v-if="cloneTarget" class="fixed inset-0 bg-black/30 grid place-items-center z-50" @click.self="cloneTarget = null">
      <div class="bg-white rounded-xl w-full max-w-sm p-5 space-y-3">
        <h2 class="text-sm font-bold text-neutral-900">Nhân bản "{{ cloneTarget.name }}"</h2>
        <div v-if="cloneError" class="text-xs text-red-600">{{ cloneError }}</div>
        <input v-model="cloneForm.code" placeholder="Mã bộ thuộc tính mới" class="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
        <input v-model="cloneForm.name" placeholder="Tên bộ thuộc tính mới" class="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
        <div class="flex gap-2 justify-end pt-1">
          <Button variant="secondary" @click="cloneTarget = null">Huỷ</Button>
          <Button variant="primary" :disabled="isCloning" @click="submitClone">{{ isCloning ? "Đang tạo..." : "Nhân bản" }}</Button>
        </div>
      </div>
    </div>

    <ConfirmDialog
      :open="deleteTarget !== null"
      :title="`Xoá bộ thuộc tính '${deleteTarget?.name}'?`"
      :error-message="deleteError"
      :loading="isDeleting"
      @cancel="deleteTarget = null"
      @confirm="confirmDelete"
    />
  </div>
</template>
