<script setup lang="ts">
import { onMounted, ref } from "vue";
import { productApi } from "@/api/productApi";

const totalProducts = ref<number | null>(null);
const totalCategories = ref<number | null>(null);

onMounted(async () => {
  const [products, categories] = await Promise.all([
    productApi.list({ size: 1 }),
    productApi.listCategories(),
  ]);
  totalProducts.value = products.totalElements;
  totalCategories.value = categories.length;
});
</script>

<template>
  <div>
    <div class="grid grid-cols-2 gap-4 mb-6 max-w-md">
      <div class="bg-white border border-neutral-200 rounded-xl p-5">
        <div class="text-xs text-neutral-500 mb-1.5">Tổng số sản phẩm</div>
        <div class="text-2xl font-extrabold text-neutral-900">{{ totalProducts ?? "…" }}</div>
      </div>
      <div class="bg-white border border-neutral-200 rounded-xl p-5">
        <div class="text-xs text-neutral-500 mb-1.5">Tổng số danh mục</div>
        <div class="text-2xl font-extrabold text-neutral-900">{{ totalCategories ?? "…" }}</div>
      </div>
    </div>

    <div class="bg-white border border-neutral-200 rounded-xl p-6">
      <h2 class="text-sm font-bold text-neutral-900 mb-1">Chào mừng trở lại</h2>
      <p class="text-sm text-neutral-500">
        Quản lý sản phẩm và danh mục của sàn từ đây. Người dùng, đơn hàng và phân quyền sẽ mở khoá ở các bản cập nhật sau.
      </p>
    </div>
  </div>
</template>
