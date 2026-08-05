<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";

import AdminPageHeader from "@/components/ui/AdminPageHeader.vue";
import Badge from "@/components/ui/Badge.vue";
import Button from "@/components/ui/Button.vue";
import DataTable, { type DataTableColumn } from "@/components/ui/DataTable.vue";
import Icon from "@/components/ui/Icon.vue";
import { attributeApi } from "@/api/attributeApi";
import { extractErrorMessage } from "@/api/http";
import type { AttributeValueResponse, ProductAttributeResponse } from "@/types/product";

const DATA_TYPES = ["STRING", "INTEGER", "DECIMAL", "BOOLEAN", "DATE", "DATETIME", "JSON"];
const INPUT_TYPES = ["TEXT", "TEXTAREA", "NUMBER", "SELECT", "MULTI_SELECT", "RADIO", "CHECKBOX", "SWITCH", "COLOR", "DATE", "DATETIME"];

const columns: DataTableColumn[] = [
  { key: "name", label: "Thuộc tính" },
  { key: "code", label: "Mã" },
  { key: "dataType", label: "Kiểu dữ liệu" },
  { key: "inputType", label: "Input UI" },
  { key: "allowVariant", label: "Tạo variant" },
];

const attributes = ref<ProductAttributeResponse[]>([]);
const isLoading = ref(false);
const errorMessage = ref("");

async function fetchAttributes() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    attributes.value = await attributeApi.list();
  } catch {
    errorMessage.value = "Không tải được danh sách thuộc tính. Vui lòng thử lại.";
  } finally {
    isLoading.value = false;
  }
}

onMounted(fetchAttributes);

// --- Tao / sua attribute ---
const showForm = ref(false);
const editingAttributeId = ref<string | null>(null);
const isSubmitting = ref(false);
const formError = ref("");
const form = reactive({ code: "", name: "", dataType: "STRING", inputType: "SELECT", allowVariant: true, isFilterable: true });

function openCreate() {
  editingAttributeId.value = null;
  form.code = "";
  form.name = "";
  form.dataType = "STRING";
  form.inputType = "SELECT";
  form.allowVariant = true;
  form.isFilterable = true;
  formError.value = "";
  showForm.value = true;
}

function openEditAttribute(attribute: ProductAttributeResponse) {
  editingAttributeId.value = attribute.id;
  form.code = attribute.code;
  form.name = attribute.name;
  form.dataType = attribute.dataType;
  form.inputType = attribute.inputType;
  form.allowVariant = attribute.allowVariant;
  form.isFilterable = attribute.isFilterable;
  formError.value = "";
  showForm.value = true;
}

async function handleSubmit() {
  if (!form.code.trim() || !form.name.trim()) {
    formError.value = "Vui lòng nhập mã và tên thuộc tính.";
    return;
  }
  isSubmitting.value = true;
  formError.value = "";
  try {
    const payload = {
      code: form.code.trim(),
      name: form.name.trim(),
      dataType: form.dataType,
      inputType: form.inputType,
      allowVariant: form.allowVariant,
      isFilterable: form.isFilterable,
    };
    if (editingAttributeId.value) {
      await attributeApi.update(editingAttributeId.value, payload);
    } else {
      await attributeApi.create(payload);
    }
    showForm.value = false;
    await fetchAttributes();
  } catch (error) {
    formError.value = extractErrorMessage(error, "Không lưu được thuộc tính.");
  } finally {
    isSubmitting.value = false;
  }
}

// --- Quan ly gia tri cua 1 attribute dang chon ---
const selectedAttribute = ref<ProductAttributeResponse | null>(null);
const values = ref<AttributeValueResponse[]>([]);
const isLoadingValues = ref(false);
const editingValueId = ref<string | null>(null);
const valueForm = reactive({ code: "", value: "", displayValue: "", colorCode: "" });
const valueError = ref("");
const isSavingValue = ref(false);

function resetValueForm() {
  editingValueId.value = null;
  valueForm.code = "";
  valueForm.value = "";
  valueForm.displayValue = "";
  valueForm.colorCode = "";
  valueError.value = "";
}

async function manageValues(attribute: ProductAttributeResponse) {
  selectedAttribute.value = attribute;
  resetValueForm();
  isLoadingValues.value = true;
  try {
    values.value = await attributeApi.listValues(attribute.id);
  } catch {
    values.value = [];
  } finally {
    isLoadingValues.value = false;
  }
}

function openEditValue(value: AttributeValueResponse) {
  editingValueId.value = value.id;
  valueForm.code = value.code;
  valueForm.value = value.value;
  valueForm.displayValue = value.displayValue ?? "";
  valueForm.colorCode = value.colorCode ?? "";
  valueError.value = "";
}

async function handleSaveValue() {
  if (!selectedAttribute.value) return;
  if (!valueForm.code.trim() || !valueForm.value.trim()) {
    valueError.value = "Vui lòng nhập mã và giá trị.";
    return;
  }
  isSavingValue.value = true;
  valueError.value = "";
  try {
    const payload = {
      code: valueForm.code.trim(),
      value: valueForm.value.trim(),
      displayValue: valueForm.displayValue.trim() || undefined,
      colorCode: valueForm.colorCode.trim() || undefined,
    };
    if (editingValueId.value) {
      await attributeApi.updateValue(selectedAttribute.value.id, editingValueId.value, payload);
    } else {
      await attributeApi.createValue(selectedAttribute.value.id, payload);
    }
    values.value = await attributeApi.listValues(selectedAttribute.value.id);
    resetValueForm();
  } catch (error) {
    valueError.value = extractErrorMessage(error, "Không lưu được giá trị.");
  } finally {
    isSavingValue.value = false;
  }
}
</script>

<template>
  <div class="space-y-5">
    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <AdminPageHeader title="Thuộc tính sản phẩm" :subtitle="`${attributes.length} thuộc tính — dùng để tạo variant (màu/size...) hoặc mô tả chung`">
        <Button variant="primary" @click="openCreate"><Icon name="plus" class="w-4 h-4" /> Thêm thuộc tính</Button>
      </AdminPageHeader>

      <form v-if="showForm" class="px-5 py-4 border-b border-neutral-200 bg-neutral-50 space-y-3" novalidate @submit.prevent="handleSubmit">
        <div v-if="formError" class="text-xs text-red-600">{{ formError }}</div>
        <div class="grid grid-cols-4 gap-3">
          <input
            v-model="form.code"
            :disabled="editingAttributeId !== null"
            placeholder="Mã (VD: COLOR)"
            class="rounded-lg border border-neutral-300 px-3 py-2 text-sm disabled:bg-neutral-100 disabled:text-neutral-400"
          />
          <input v-model="form.name" placeholder="Tên (VD: Màu sắc)" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
          <select v-model="form.dataType" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm">
            <option v-for="t in DATA_TYPES" :key="t" :value="t">{{ t }}</option>
          </select>
          <select v-model="form.inputType" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm">
            <option v-for="t in INPUT_TYPES" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="flex gap-4 text-sm text-neutral-600">
          <label class="flex items-center gap-1.5"><input v-model="form.allowVariant" type="checkbox" /> Cho phép tạo variant</label>
          <label class="flex items-center gap-1.5"><input v-model="form.isFilterable" type="checkbox" /> Dùng để lọc</label>
        </div>
        <div class="flex gap-2">
          <Button type="submit" variant="primary" :disabled="isSubmitting">{{ isSubmitting ? "Đang lưu..." : "Lưu" }}</Button>
          <Button variant="secondary" @click="showForm = false">Huỷ</Button>
        </div>
      </form>

      <DataTable :columns="columns" :items="attributes" :loading="isLoading" :error="errorMessage" empty-text="Chưa có thuộc tính nào." has-actions>
        <template #cell-name="{ item }"><span class="text-neutral-800">{{ item.name }}</span></template>
        <template #cell-code="{ item }"><span class="text-neutral-500 font-mono text-xs">{{ item.code }}</span></template>
        <template #cell-dataType="{ item }"><span class="text-neutral-500">{{ item.dataType }}</span></template>
        <template #cell-inputType="{ item }"><span class="text-neutral-500">{{ item.inputType }}</span></template>
        <template #cell-allowVariant="{ item }">
          <Badge :variant="item.allowVariant ? 'success' : 'warning'">{{ item.allowVariant ? "Có" : "Không" }}</Badge>
        </template>
        <template #actions="{ item }">
          <Button variant="secondary" @click="openEditAttribute(item)"><Icon name="edit" class="w-3.5 h-3.5" /> Sửa</Button>
          <Button variant="secondary" @click="manageValues(item)"><Icon name="filter" class="w-3.5 h-3.5" /> Giá trị</Button>
        </template>
      </DataTable>
    </div>

    <div v-if="selectedAttribute" class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <AdminPageHeader :title="`Giá trị của “${selectedAttribute.name}”`" :subtitle="`${values.length} giá trị`" />

      <form class="px-5 py-4 border-b border-neutral-200 bg-neutral-50 space-y-3" novalidate @submit.prevent="handleSaveValue">
        <div v-if="valueError" class="text-xs text-red-600">{{ valueError }}</div>
        <div class="grid grid-cols-4 gap-3">
          <input
            v-model="valueForm.code"
            :disabled="editingValueId !== null"
            placeholder="Mã (VD: BLACK)"
            class="rounded-lg border border-neutral-300 px-3 py-2 text-sm disabled:bg-neutral-100 disabled:text-neutral-400"
          />
          <input v-model="valueForm.value" placeholder="Giá trị lưu (VD: BLACK)" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
          <input v-model="valueForm.displayValue" placeholder="Hiển thị (VD: Đen)" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
          <input v-model="valueForm.colorCode" placeholder="Mã màu (VD: #000000)" class="rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
        </div>
        <div class="flex gap-2">
          <Button type="submit" variant="primary" :disabled="isSavingValue">
            {{ isSavingValue ? "Đang lưu..." : editingValueId ? "Lưu thay đổi" : "Thêm giá trị" }}
          </Button>
          <Button v-if="editingValueId" type="button" variant="secondary" @click="resetValueForm">Huỷ sửa</Button>
        </div>
      </form>

      <div v-if="isLoadingValues" class="p-6 text-sm text-neutral-400">Đang tải...</div>
      <div v-else-if="values.length === 0" class="p-10 text-center text-sm text-neutral-500">Chưa có giá trị nào.</div>
      <div v-else class="flex flex-wrap gap-2 p-5">
        <button
          v-for="value in values"
          :key="value.id"
          type="button"
          class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-sm hover:border-brand"
          :class="editingValueId === value.id ? 'border-brand bg-brand-soft text-brand' : 'border-neutral-300'"
          @click="openEditValue(value)"
        >
          <span
            v-if="value.colorCode"
            class="w-3.5 h-3.5 rounded-full border border-neutral-300"
            :style="{ backgroundColor: value.colorCode }"
          ></span>
          {{ value.displayValue ?? value.value }}
          <span class="text-neutral-400 text-xs font-mono">{{ value.code }}</span>
          <Icon name="edit" class="w-3 h-3 text-neutral-400" />
        </button>
      </div>
    </div>
  </div>
</template>
