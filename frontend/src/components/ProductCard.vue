<script setup lang="ts">
import { RouterLink } from "vue-router";
import type { ProductResponse } from "@/types/product";
import { formatCurrency } from "@/utils/format";

defineProps<{ product: ProductResponse }>();
</script>

<template>
  <RouterLink
    :to="{ name: 'product-detail', params: { id: product.id } }"
    class="block bg-white border border-neutral-200 rounded-lg overflow-hidden hover:shadow-md transition-shadow"
  >
    <div class="aspect-square bg-neutral-100 flex items-center justify-center overflow-hidden">
      <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" class="w-full h-full object-cover" />
      <span v-else class="text-neutral-300 text-xs">Không có ảnh</span>
    </div>
    <div class="p-2.5">
      <p class="text-xs text-neutral-800 line-clamp-2 leading-snug mb-1.5 min-h-[2.2em]">{{ product.name }}</p>
      <p class="text-brand font-bold text-sm">{{ formatCurrency(product.price) }}</p>
    </div>
  </RouterLink>
</template>
