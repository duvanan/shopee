<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import axios from "axios";

import Button from "@/components/ui/Button.vue";
import { productApi } from "@/api/productApi";
import { extractErrorMessage } from "@/api/http";
import type { ApiErrorBody } from "@/types/auth";
import type { CategoryResponse } from "@/types/product";

const route = useRoute();
const router = useRouter();

const productId = computed(() => (typeof route.params.id === "string" ? route.params.id : null));
const isEdit = computed(() => productId.value !== null);

const categories = ref<CategoryResponse[]>([]);
const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref("");
const fieldErrors = reactive<Record<string, string>>({});

const form = reactive({
  name: "",
  description: "",
  price: "" as string | number,
  stockQuantity: "" as string | number,
  imageUrl: "",
  categoryId: "" as string,
});

function validate(): boolean {
  fieldErrors.name = form.name.trim() ? "" : "Vui lòng nhập tên sản phẩm";
  fieldErrors.price = Number(form.price) >= 0 && form.price !== "" ? "" : "Giá phải là số lớn hơn hoặc bằng 0";
  fieldErrors.stockQuantity =
    Number.isInteger(Number(form.stockQuantity)) && Number(form.stockQuantity) >= 0
      ? ""
      : "Tồn kho phải là số nguyên lớn hơn hoặc bằng 0";
  return !fieldErrors.name && !fieldErrors.price && !fieldErrors.stockQuantity;
}

async function loadCategories() {
  try {
    categories.value = await productApi.listCategories();
  } catch {
    categories.value = [];
  }
}

async function loadProduct() {
  if (!productId.value) return;
  isLoading.value = true;
  try {
    const product = await productApi.get(productId.value);
    form.name = product.name;
    form.description = product.description ?? "";
    form.price = product.price;
    form.stockQuantity = product.stockQuantity;
    form.imageUrl = product.imageUrl ?? "";
    form.categoryId = product.categoryId ?? "";
  } catch {
    errorMessage.value = "Không tải được thông tin sản phẩm.";
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  loadCategories();
  loadProduct();
});

async function handleSubmit() {
  errorMessage.value = "";
  if (!validate()) return;

  const payload = {
    name: form.name.trim(),
    description: form.description.trim() || null,
    price: Number(form.price),
    stockQuantity: Number(form.stockQuantity),
    imageUrl: form.imageUrl.trim() || null,
    categoryId: form.categoryId || null,
  };

  isSubmitting.value = true;
  try {
    if (isEdit.value && productId.value) {
      await productApi.update(productId.value, payload);
    } else {
      await productApi.create(payload);
    }
    router.push({ name: "admin-products" });
  } catch (error) {
    if (axios.isAxiosError<ApiErrorBody>(error) && error.response?.data.errors) {
      for (const fieldError of error.response.data.errors) {
        fieldErrors[fieldError.field] = fieldError.message;
      }
    }
    errorMessage.value = extractErrorMessage(error, "Không lưu được sản phẩm. Vui lòng thử lại.");
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="max-w-2xl">
    <button type="button" class="text-xs text-neutral-500 hover:text-brand mb-3" @click="router.push({ name: 'admin-products' })">
      ← Quay lại danh sách sản phẩm
    </button>

    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <div class="px-6 py-4 border-b border-neutral-200">
        <h1 class="text-sm font-bold text-neutral-900">{{ isEdit ? "Sửa sản phẩm" : "Thêm sản phẩm" }}</h1>
      </div>

      <div v-if="isLoading" class="p-6 text-sm text-neutral-400">Đang tải...</div>

      <form v-else class="p-6 space-y-4" novalidate @submit.prevent="handleSubmit">
        <div v-if="errorMessage" class="flex gap-2 items-start bg-red-50 border border-red-300 text-red-600 text-sm rounded-lg px-3 py-2.5">
          <span aria-hidden="true">⚠</span>
          <span>{{ errorMessage }}</span>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Tên sản phẩm</label>
          <input
            v-model="form.name"
            type="text"
            placeholder="VD: Áo thun basic cotton"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="fieldErrors.name ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.name" class="text-xs text-red-600 mt-1">{{ fieldErrors.name }}</p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Mô tả</label>
          <textarea
            v-model="form.description"
            rows="3"
            placeholder="Mô tả ngắn về sản phẩm..."
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          ></textarea>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Giá (₫)</label>
            <input
              v-model="form.price"
              type="number"
              min="0"
              step="1000"
              class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
              :class="fieldErrors.price ? 'border-red-400' : 'border-neutral-300'"
            />
            <p v-if="fieldErrors.price" class="text-xs text-red-600 mt-1">{{ fieldErrors.price }}</p>
          </div>
          <div>
            <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Tồn kho</label>
            <input
              v-model="form.stockQuantity"
              type="number"
              min="0"
              step="1"
              class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
              :class="fieldErrors.stockQuantity ? 'border-red-400' : 'border-neutral-300'"
            />
            <p v-if="fieldErrors.stockQuantity" class="text-xs text-red-600 mt-1">{{ fieldErrors.stockQuantity }}</p>
          </div>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">URL ảnh</label>
          <input
            v-model="form.imageUrl"
            type="text"
            placeholder="https://..."
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          />
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Danh mục</label>
          <select
            v-model="form.categoryId"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          >
            <option value="">— Không thuộc danh mục nào —</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
          </select>
        </div>

        <div class="flex gap-2 pt-2">
          <Button type="submit" variant="primary" :disabled="isSubmitting">
            {{ isSubmitting ? "Đang lưu..." : "Lưu sản phẩm" }}
          </Button>
          <Button variant="secondary" @click="router.push({ name: 'admin-products' })">Huỷ</Button>
        </div>
      </form>
    </div>
  </div>
</template>
