<script setup lang="ts">
import { onMounted, reactive, ref, watch } from "vue";

import ProductCard from "@/components/ProductCard.vue";
import { productApi } from "@/api/productApi";
import { categoryApi } from "@/api/categoryApi";
import type { PageData } from "@/types/api";
import type { CategoryResponse, ProductSummaryResponse } from "@/types/product";

const PAGE_SIZE = 12;

const categories = ref<CategoryResponse[]>([]);
// filters.page is 0-based internally (matches goToPage/button index math below);
// converted to the API's 1-based `page` param at the call site.
const products = ref<PageData<ProductSummaryResponse> | null>(null);

const filters = reactive({
  search: "",
  categoryId: undefined as string | undefined,
  page: 0,
});

const isLoading = ref(false);
const errorMessage = ref("");
let searchDebounce: ReturnType<typeof setTimeout> | undefined;

async function fetchProducts() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    products.value = await productApi.list({
      search: filters.search || undefined,
      categoryId: filters.categoryId,
      page: filters.page + 1,
      size: PAGE_SIZE,
    });
  } catch {
    errorMessage.value = "Không tải được danh sách sản phẩm. Vui lòng thử lại.";
  } finally {
    isLoading.value = false;
  }
}

async function fetchCategories() {
  try {
    categories.value = await categoryApi.list();
  } catch {
    categories.value = [];
  }
}

function selectCategory(categoryId: string | undefined) {
  filters.categoryId = categoryId;
  filters.page = 0;
}

function goToPage(page: number) {
  filters.page = page;
}

watch(
  () => filters.search,
  () => {
    clearTimeout(searchDebounce);
    searchDebounce = setTimeout(() => {
      filters.page = 0;
      fetchProducts();
    }, 350);
  },
);

watch([() => filters.categoryId, () => filters.page], fetchProducts);

onMounted(() => {
  fetchCategories();
  fetchProducts();
});
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <div class="flex flex-col sm:flex-row gap-3 mb-4">
      <input
        v-model="filters.search"
        type="search"
        placeholder="Tìm sản phẩm theo tên…"
        class="flex-1 rounded-lg border border-neutral-300 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
      />
    </div>

    <div class="flex flex-wrap gap-2 mb-6">
      <button
        type="button"
        class="text-xs px-3 py-1.5 rounded-full border transition-colors"
        :class="filters.categoryId === undefined
          ? 'bg-brand-soft border-brand text-brand font-semibold'
          : 'bg-white border-neutral-300 text-neutral-600 hover:border-neutral-400'"
        @click="selectCategory(undefined)"
      >
        Tất cả
      </button>
      <button
        v-for="category in categories"
        :key="category.id"
        type="button"
        class="text-xs px-3 py-1.5 rounded-full border transition-colors"
        :class="filters.categoryId === category.id
          ? 'bg-brand-soft border-brand text-brand font-semibold'
          : 'bg-white border-neutral-300 text-neutral-600 hover:border-neutral-400'"
        @click="selectCategory(category.id)"
      >
        {{ category.name }}
      </button>
    </div>

    <div v-if="errorMessage" class="text-center py-16 border border-dashed border-red-300 bg-red-50 text-red-600 text-sm rounded-lg">
      ⚠ {{ errorMessage }}
    </div>

    <div v-else-if="isLoading" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
      <div v-for="n in 6" :key="n" class="bg-white border border-neutral-200 rounded-lg overflow-hidden animate-pulse">
        <div class="aspect-square bg-neutral-100"></div>
        <div class="p-2.5 space-y-2">
          <div class="h-2.5 bg-neutral-100 rounded"></div>
          <div class="h-2.5 bg-neutral-100 rounded w-2/3"></div>
        </div>
      </div>
    </div>

    <div v-else-if="!products || products.items.length === 0" class="text-center py-16 border border-dashed border-neutral-300 text-neutral-500 text-sm rounded-lg">
      📦 Không tìm thấy sản phẩm nào khớp bộ lọc.<br />Thử từ khoá hoặc danh mục khác.
    </div>

    <template v-else>
      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
        <ProductCard v-for="product in products.items" :key="product.id" :product="product" />
      </div>

      <div v-if="products.totalPages > 1" class="flex justify-center gap-1.5 mt-8">
        <button
          v-for="pageNumber in products.totalPages"
          :key="pageNumber"
          type="button"
          class="w-8 h-8 rounded-md text-xs border flex items-center justify-center transition-colors"
          :class="pageNumber - 1 === filters.page
            ? 'bg-neutral-900 text-white border-neutral-900'
            : 'bg-white border-neutral-300 text-neutral-600 hover:border-neutral-400'"
          @click="goToPage(pageNumber - 1)"
        >
          {{ pageNumber }}
        </button>
      </div>
    </template>
  </div>
</template>
