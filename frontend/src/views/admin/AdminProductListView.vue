<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { RouterLink } from "vue-router";

import AdminPageHeader from "@/components/ui/AdminPageHeader.vue";
import Badge from "@/components/ui/Badge.vue";
import Button from "@/components/ui/Button.vue";
import ConfirmDialog from "@/components/ui/ConfirmDialog.vue";
import DataTable, { type DataTableColumn } from "@/components/ui/DataTable.vue";
import Icon from "@/components/ui/Icon.vue";
import Pagination from "@/components/ui/Pagination.vue";
import { productApi } from "@/api/productApi";
import { extractErrorMessage } from "@/api/http";
import type { PageData } from "@/types/api";
import type { ProductSummaryResponse } from "@/types/product";
import { formatCurrency } from "@/utils/format";

const PAGE_SIZE = 10;

const columns: DataTableColumn[] = [
  { key: "name", label: "Sản phẩm" },
  { key: "category", label: "Danh mục" },
  { key: "price", label: "Giá" },
  { key: "status", label: "Trạng thái" },
];

const products = ref<PageData<ProductSummaryResponse> | null>(null);
const isLoading = ref(false);
const errorMessage = ref("");

const filters = reactive({ search: "", page: 0 });
let searchDebounce: ReturnType<typeof setTimeout> | undefined;

async function fetchProducts() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    products.value = await productApi.listAdmin({ search: filters.search || undefined, page: filters.page + 1, size: PAGE_SIZE });
  } catch {
    errorMessage.value = "Không tải được danh sách sản phẩm. Vui lòng thử lại.";
  } finally {
    isLoading.value = false;
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
watch(() => filters.page, fetchProducts);

onMounted(fetchProducts);

function priceLabel(product: ProductSummaryResponse): string {
  if (product.minPrice == null) return "—";
  if (product.maxPrice == null || product.minPrice === product.maxPrice) return formatCurrency(product.minPrice);
  return `${formatCurrency(product.minPrice)} - ${formatCurrency(product.maxPrice)}`;
}

function statusBadge(status: string): "danger" | "warning" | "success" | "neutral" {
  if (status === "ACTIVE") return "success";
  if (status === "DRAFT") return "neutral";
  if (status === "OUT_OF_STOCK" || status === "INACTIVE") return "warning";
  return "danger";
}

const deleteTarget = ref<ProductSummaryResponse | null>(null);
const deleteError = ref<string | null>(null);
const isDeleting = ref(false);

function askDelete(product: ProductSummaryResponse) {
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

const currentPage = computed({
  get: () => filters.page,
  set: (value: number) => (filters.page = value),
});
</script>

<template>
  <div>
    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <AdminPageHeader title="Sản phẩm" :subtitle="products ? `${products.totalElements} sản phẩm trên toàn sàn` : undefined">
        <RouterLink :to="{ name: 'admin-product-new' }">
          <Button variant="primary"><Icon name="plus" class="w-4 h-4" /> Thêm sản phẩm</Button>
        </RouterLink>
      </AdminPageHeader>

      <div class="flex gap-2.5 px-5 py-3.5 border-b border-neutral-200 bg-neutral-50 flex-wrap">
        <div class="relative w-60">
          <Icon name="search" class="w-4 h-4 absolute left-2.5 top-1/2 -translate-y-1/2 text-neutral-400" />
          <input
            v-model="filters.search"
            type="search"
            placeholder="Tìm theo tên sản phẩm..."
            class="w-full rounded-lg border border-neutral-300 pl-8 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          />
        </div>
      </div>

      <DataTable
        :columns="columns"
        :items="products?.items ?? []"
        :loading="isLoading"
        :error="errorMessage"
        empty-text="Không tìm thấy sản phẩm nào khớp bộ lọc."
        has-actions
      >
        <template #cell-name="{ item }">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-lg bg-neutral-100 shrink-0 overflow-hidden">
              <img v-if="item.thumbnailUrl" :src="item.thumbnailUrl" :alt="item.name" class="w-full h-full object-cover" />
            </div>
            <div>
              <div class="text-neutral-800">{{ item.name }}</div>
              <div class="text-neutral-400 text-xs font-mono">{{ item.code }}</div>
            </div>
          </div>
        </template>
        <template #cell-category="{ item }">
          <span class="text-neutral-600">{{ item.primaryCategoryName ?? "—" }}</span>
        </template>
        <template #cell-price="{ item }">
          <span class="font-medium text-neutral-900">{{ priceLabel(item) }}</span>
        </template>
        <template #cell-status="{ item }">
          <Badge :variant="statusBadge(item.status)">{{ item.status }}</Badge>
        </template>
        <template #actions="{ item }">
          <RouterLink :to="{ name: 'admin-product-edit', params: { id: item.id } }">
            <Button variant="secondary"><Icon name="edit" class="w-3.5 h-3.5" /> Sửa</Button>
          </RouterLink>
          <Button variant="danger" @click="askDelete(item)"><Icon name="trash" class="w-3.5 h-3.5" /> Xoá</Button>
        </template>
      </DataTable>

      <div v-if="products && products.totalPages > 1" class="px-5 py-4 border-t border-neutral-200">
        <Pagination v-model:current-page="currentPage" :total-pages="products.totalPages" />
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
