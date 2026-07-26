<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import axios from "axios";

import { productApi } from "@/api/productApi";
import type { ProductResponse } from "@/types/product";
import { formatCurrency } from "@/utils/format";

const route = useRoute();

const product = ref<ProductResponse | null>(null);
const isLoading = ref(true);
const notFound = ref(false);

async function fetchProduct(id: string) {
  isLoading.value = true;
  notFound.value = false;
  product.value = null;
  try {
    product.value = await productApi.get(id);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      notFound.value = true;
    }
  } finally {
    isLoading.value = false;
  }
}

watch(
  () => route.params.id,
  (id) => {
    if (typeof id === "string") fetchProduct(id);
  },
  { immediate: true },
);
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 py-10">
    <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-[360px_1fr] gap-8 animate-pulse">
      <div class="aspect-square bg-neutral-100 rounded-xl"></div>
      <div class="space-y-3">
        <div class="h-5 bg-neutral-100 rounded w-3/4"></div>
        <div class="h-7 bg-neutral-100 rounded w-1/3"></div>
        <div class="h-3 bg-neutral-100 rounded w-1/2"></div>
      </div>
    </div>

    <div v-else-if="notFound" class="text-center py-20">
      <div class="text-5xl font-mono text-neutral-300 mb-3">404</div>
      <p class="text-neutral-500 text-sm">Không tìm thấy sản phẩm này — có thể đã bị gỡ hoặc đường link không đúng.</p>
      <RouterLink to="/" class="inline-block mt-4 text-brand font-semibold text-sm hover:underline">← Về trang chủ</RouterLink>
    </div>

    <div v-else-if="product" class="grid grid-cols-1 md:grid-cols-[360px_1fr] gap-8">
      <div class="aspect-square bg-neutral-100 rounded-xl flex items-center justify-center overflow-hidden">
        <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" class="w-full h-full object-cover" />
        <span v-else class="text-neutral-300 text-sm">Không có ảnh</span>
      </div>

      <div>
        <RouterLink v-if="product.categoryName" to="/" class="text-xs text-neutral-400 hover:text-neutral-600">
          {{ product.categoryName }}
        </RouterLink>
        <h1 class="text-xl font-bold text-neutral-900 mt-1 mb-2 text-balance">{{ product.name }}</h1>
        <p class="text-2xl font-bold text-brand mb-4">{{ formatCurrency(product.price) }}</p>

        <div class="flex gap-5 text-xs text-neutral-500 mb-5">
          <span>
            Còn lại:
            <b class="text-neutral-800">{{ product.stockQuantity > 0 ? product.stockQuantity : "Hết hàng" }}</b>
          </span>
          <span v-if="product.categoryName">
            Danh mục: <b class="text-neutral-800">{{ product.categoryName }}</b>
          </span>
        </div>

        <p class="text-sm text-neutral-600 leading-relaxed mb-6 max-w-prose whitespace-pre-line">
          {{ product.description || "Chưa có mô tả cho sản phẩm này." }}
        </p>

        <button
          type="button"
          disabled
          title="Chức năng giỏ hàng sẽ có ở bước tiếp theo"
          class="rounded-lg bg-neutral-300 text-white font-semibold text-sm px-8 py-3 cursor-not-allowed"
        >
          Thêm vào giỏ
        </button>
      </div>
    </div>
  </div>
</template>
