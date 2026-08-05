<script setup lang="ts">
import { computed } from "vue";
import { RouterLink } from "vue-router";
import type { ProductSummaryResponse } from "@/types/product";
import { formatCurrency } from "@/utils/format";

const props = defineProps<{ product: ProductSummaryResponse }>();

const priceLabel = computed(() => {
  const { minPrice, maxPrice } = props.product;
  if (minPrice == null) return "";
  if (maxPrice == null || minPrice === maxPrice) return formatCurrency(minPrice);
  return `${formatCurrency(minPrice)} - ${formatCurrency(maxPrice)}`;
});
</script>

<template>
  <RouterLink
    :to="{ name: 'product-detail', params: { slug: product.slug } }"
    class="block bg-white border border-neutral-200 rounded-lg overflow-hidden hover:shadow-md transition-shadow"
  >
    <div class="aspect-square bg-neutral-100 flex items-center justify-center overflow-hidden">
      <img v-if="product.thumbnailUrl" :src="product.thumbnailUrl" :alt="product.name" class="w-full h-full object-cover" />
      <span v-else class="text-neutral-300 text-xs">Không có ảnh</span>
    </div>
    <div class="p-2.5">
      <p class="text-xs text-neutral-800 line-clamp-2 leading-snug mb-1.5 min-h-[2.2em]">{{ product.name }}</p>
      <p class="text-brand font-bold text-sm">{{ priceLabel }}</p>
    </div>
  </RouterLink>
</template>
