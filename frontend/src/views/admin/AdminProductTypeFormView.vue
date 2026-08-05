<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import Button from "@/components/ui/Button.vue";
import { productTypeApi } from "@/api/productTypeApi";
import { attributeSetApi } from "@/api/attributeSetApi";
import { extractErrorMessage } from "@/api/http";
import type { AttributeSetResponse } from "@/types/product";

const route = useRoute();
const router = useRouter();

const productTypeId = computed(() => (typeof route.params.id === "string" ? route.params.id : null));
const isEdit = computed(() => productTypeId.value !== null);

const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref("");
const fieldErrors = reactive<Record<string, string>>({});
const attributeSets = ref<AttributeSetResponse[]>([]);

const form = reactive({
  code: "",
  name: "",
  description: "",
  defaultAttributeSetId: "",
  sortOrder: 0,
});

function validate(): boolean {
  fieldErrors.code = form.code.trim() ? "" : "Vui lòng nhập mã loại sản phẩm";
  fieldErrors.name = form.name.trim() ? "" : "Vui lòng nhập tên loại sản phẩm";
  return !fieldErrors.code && !fieldErrors.name;
}

onMounted(async () => {
  isLoading.value = true;
  try {
    attributeSets.value = await attributeSetApi.list();
    if (productTypeId.value) {
      const productType = await productTypeApi.get(productTypeId.value);
      form.code = productType.code;
      form.name = productType.name;
      form.description = productType.description ?? "";
      form.defaultAttributeSetId = productType.defaultAttributeSetId ?? "";
      form.sortOrder = productType.sortOrder;
    }
  } catch {
    errorMessage.value = "Không tải được dữ liệu cần thiết.";
  } finally {
    isLoading.value = false;
  }
});

async function handleSubmit() {
  errorMessage.value = "";
  if (!validate()) return;

  const payload = {
    code: form.code.trim(),
    name: form.name.trim(),
    description: form.description.trim() || null,
    defaultAttributeSetId: form.defaultAttributeSetId || null,
    sortOrder: Number(form.sortOrder) || 0,
  };

  isSubmitting.value = true;
  try {
    if (isEdit.value && productTypeId.value) {
      await productTypeApi.update(productTypeId.value, payload);
    } else {
      await productTypeApi.create(payload);
    }
    router.push({ name: "admin-product-types" });
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, "Không lưu được loại sản phẩm. Vui lòng thử lại.");
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="max-w-lg">
    <button type="button" class="text-xs text-neutral-500 hover:text-brand mb-3" @click="router.push({ name: 'admin-product-types' })">
      ← Quay lại danh sách loại sản phẩm
    </button>

    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <div class="px-6 py-4 border-b border-neutral-200">
        <h1 class="text-sm font-bold text-neutral-900">{{ isEdit ? "Sửa loại sản phẩm" : "Thêm loại sản phẩm" }}</h1>
      </div>

      <div v-if="isLoading" class="p-6 text-sm text-neutral-400">Đang tải...</div>

      <form v-else class="p-6 space-y-4" novalidate @submit.prevent="handleSubmit">
        <div v-if="errorMessage" class="flex gap-2 items-start bg-red-50 border border-red-300 text-red-600 text-sm rounded-lg px-3 py-2.5">
          <span aria-hidden="true">⚠</span>
          <span>{{ errorMessage }}</span>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Mã loại sản phẩm</label>
          <input
            v-model="form.code"
            type="text"
            placeholder="VD: SMARTPHONE"
            :disabled="isEdit"
            class="w-full rounded-lg border px-3 py-2.5 text-sm disabled:bg-neutral-50 disabled:text-neutral-400 focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="fieldErrors.code ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.code" class="text-xs text-red-600 mt-1">{{ fieldErrors.code }}</p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Tên loại sản phẩm</label>
          <input
            v-model="form.name"
            type="text"
            placeholder="VD: Điện thoại thông minh"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="fieldErrors.name ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.name" class="text-xs text-red-600 mt-1">{{ fieldErrors.name }}</p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Bộ thuộc tính mặc định</label>
          <select
            v-model="form.defaultAttributeSetId"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          >
            <option value="">— Chưa gán —</option>
            <option v-for="set in attributeSets" :key="set.id" :value="set.id">{{ set.name }} ({{ set.attributeCount }} thuộc tính)</option>
          </select>
          <p class="text-xs text-neutral-400 mt-1">
            Quyết định form nhập thuộc tính khi tạo sản phẩm thuộc loại này. Chưa có bộ phù hợp? Vào mục "Bộ thuộc tính" để tạo trước.
          </p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Mô tả</label>
          <textarea
            v-model="form.description"
            rows="3"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          ></textarea>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Thứ tự hiển thị</label>
          <input
            v-model="form.sortOrder"
            type="number"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          />
        </div>

        <div class="flex gap-2 pt-2">
          <Button type="submit" variant="primary" :disabled="isSubmitting">
            {{ isSubmitting ? "Đang lưu..." : "Lưu loại sản phẩm" }}
          </Button>
          <Button variant="secondary" @click="router.push({ name: 'admin-product-types' })">Huỷ</Button>
        </div>
      </form>
    </div>
  </div>
</template>
