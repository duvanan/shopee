<script setup lang="ts">
import { onMounted, reactive, ref, watch } from "vue";
import { RouterLink } from "vue-router";

import Button from "@/components/ui/Button.vue";
import ConfirmDialog from "@/components/ui/ConfirmDialog.vue";
import Pagination from "@/components/ui/Pagination.vue";
import { productApi } from "@/api/productApi";
import { extractErrorMessage } from "@/api/http";
import type { PageData } from "@/types/api";
import type { CategoryResponse, ProductResponse } from "@/types/product";
import { formatCurrency } from "@/utils/format";

const PAGE_SIZE = 10;

const categories = ref<CategoryResponse[]>([]);
const products = ref<PageData<ProductResponse> | null>(null);
const isLoading = ref(false);
const errorMessage = ref("");

const filters = reactive({
  search: "",
  categoryId: undefined as string | undefined,
  page: 0,
});

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
    categories.value = await productApi.listCategories();
  } catch {
    categories.value = [];
  }
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

function stockBadge(stock: number): { label: string; className: string } {
  if (stock <= 0) return { label: "Hết hàng", className: "bg-red-50 text-red-600" };
  if (stock <= 5) return { label: "Sắp hết", className: "bg-amber-50 text-amber-700" };
  return { label: "Còn hàng", className: "bg-emerald-50 text-emerald-700" };
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("vi-VN");
}

const deleteTarget = ref<ProductResponse | null>(null);
const deleteError = ref<string | null>(null);
const isDeleting = ref(false);

function askDelete(product: ProductResponse) {
  deleteTarget.value = product;
  deleteError.value = null;
}

async function confirmDelete() {
  if (!deleteTarget.value) return;
  isDeleting.value = true;
  try {
    await productApi.remove(deleteTarget.value.id);
    deleteTarget.value = null;
    await fetchProducts();
  } catch (error) {
    deleteError.value = extractErrorMessage(error, "Không xoá được sản phẩm này.");
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
          <h1 class="text-sm font-bold text-neutral-900">Sản phẩm</h1>
          <p v-if="products" class="text-xs text-neutral-500 mt-0.5">{{ products.totalElements }} sản phẩm trên toàn sàn</p>
        </div>
        <RouterLink :to="{ name: 'admin-product-new' }">
          <Button variant="primary">+ Thêm sản phẩm</Button>
        </RouterLink>
      </div>

      <div class="flex gap-2.5 px-5 py-3.5 border-b border-neutral-200 bg-neutral-50 flex-wrap">
        <input
          v-model="filters.search"
          type="search"
          placeholder="Tìm theo tên sản phẩm..."
          class="w-60 rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
        />
        <select
          v-model="filters.categoryId"
          class="w-48 rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
        >
          <option :value="undefined">Tất cả danh mục</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
        </select>
      </div>

      <div v-if="errorMessage" class="p-6 text-sm text-red-600">{{ errorMessage }}</div>
      <div v-else-if="isLoading" class="p-6 text-sm text-neutral-400">Đang tải...</div>
      <div v-else-if="!products || products.items.length === 0" class="p-10 text-center text-sm text-neutral-500">
        Không tìm thấy sản phẩm nào khớp bộ lọc.
      </div>

      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="text-left text-[11px] uppercase tracking-wide text-neutral-400">
              <th class="px-5 py-2.5 font-bold">Sản phẩm</th>
              <th class="px-5 py-2.5 font-bold">Danh mục</th>
              <th class="px-5 py-2.5 font-bold">Giá</th>
              <th class="px-5 py-2.5 font-bold">Tồn kho</th>
              <th class="px-5 py-2.5 font-bold">Cập nhật</th>
              <th class="px-5 py-2.5"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in products.items" :key="product.id" class="border-t border-neutral-100 hover:bg-neutral-50">
              <td class="px-5 py-3">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-lg bg-neutral-100 shrink-0 overflow-hidden">
                    <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" class="w-full h-full object-cover" />
                  </div>
                  <span class="text-neutral-800">{{ product.name }}</span>
                </div>
              </td>
              <td class="px-5 py-3 text-neutral-600">{{ product.categoryName ?? "—" }}</td>
              <td class="px-5 py-3 font-medium text-neutral-900">{{ formatCurrency(product.price) }}</td>
              <td class="px-5 py-3">
                <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold" :class="stockBadge(product.stockQuantity).className">
                  {{ product.stockQuantity }} · {{ stockBadge(product.stockQuantity).label }}
                </span>
              </td>
              <td class="px-5 py-3 text-neutral-500">{{ formatDate(product.updatedAt) }}</td>
              <td class="px-5 py-3">
                <div class="flex justify-end gap-2">
                  <RouterLink :to="{ name: 'admin-product-edit', params: { id: product.id } }">
                    <Button variant="secondary">Sửa</Button>
                  </RouterLink>
                  <Button variant="danger" @click="askDelete(product)">Xoá</Button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="products && products.totalPages > 1" class="px-5 py-4 border-t border-neutral-200">
        <Pagination v-model:current-page="filters.page" :total-pages="products.totalPages" />
      </div>
    </div>

    <ConfirmDialog
      :open="deleteTarget !== null"
      :title="`Xoá sản phẩm '${deleteTarget?.name}'?`"
      :error-message="deleteError"
      :loading="isDeleting"
      @cancel="deleteTarget = null"
      @confirm="confirmDelete"
    />
  </div>
</template>
