<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import axios from "axios";

import { productApi } from "@/api/productApi";
import type { ProductDetailResponse, ProductVariantResponse } from "@/types/product";
import { formatCurrency } from "@/utils/format";

const route = useRoute();

const product = ref<ProductDetailResponse | null>(null);
const isLoading = ref(true);
const notFound = ref(false);

// attributeId -> valueId dang duoc chon. Chi cac attribute co isVariantAttribute
// (mau sac/kich thuoc...) moi xuat hien o day.
const selection = reactive<Record<string, string>>({});

const variantAttributeConfigs = computed(() => (product.value?.attributeConfigs ?? []).filter((c) => c.isVariantAttribute));

/** Variant khop chinh xac voi to hop dang chon - so sanh set attributeValueId, khong quan tam thu tu. */
const matchedVariant = computed<ProductVariantResponse | null>(() => {
  if (!product.value) return null;
  const requiredAttrIds = variantAttributeConfigs.value.map((c) => c.attributeId);
  if (requiredAttrIds.some((id) => !selection[id])) return null;

  return (
    product.value.variants.find((variant) => {
      if (variant.attributeValues.length !== requiredAttrIds.length) return false;
      return requiredAttrIds.every((attrId) => variant.attributeValues.some((av) => av.attributeId === attrId && av.valueId === selection[attrId]));
    }) ?? null
  );
});

/** 1 gia tri thuoc tinh co "kha dung" hay khong voi cac lua chon HIEN TAI cua cac attribute khac - dung de disable swatch chua co variant tuong ung. */
function isValueAvailable(attributeId: string, valueId: string): boolean {
  if (!product.value) return false;
  const otherSelections = Object.entries(selection).filter(([attrId]) => attrId !== attributeId);
  return product.value.variants.some((variant) => {
    const hasThisValue = variant.attributeValues.some((av) => av.attributeId === attributeId && av.valueId === valueId);
    if (!hasThisValue) return false;
    return otherSelections.every(([attrId, valId]) => variant.attributeValues.some((av) => av.attributeId === attrId && av.valueId === valId));
  });
}

function selectValue(attributeId: string, valueId: string) {
  selection[attributeId] = valueId;
}

function initSelectionFromDefaultVariant() {
  if (!product.value) return;
  const defaultVariant = product.value.variants.find((v) => v.isDefault) ?? product.value.variants[0];
  Object.keys(selection).forEach((key) => delete selection[key]);
  if (defaultVariant) {
    for (const av of defaultVariant.attributeValues) {
      selection[av.attributeId] = av.valueId ?? "";
    }
  }
}

const displayVariant = computed<ProductVariantResponse | null>(
  () => matchedVariant.value ?? product.value?.variants.find((v) => v.isDefault) ?? product.value?.variants[0] ?? null,
);

const priceLabel = computed(() => {
  if (displayVariant.value) return formatCurrency(displayVariant.value.salePrice);
  if (!product.value) return "";
  const { minPrice, maxPrice } = product.value;
  if (minPrice == null) return "";
  if (maxPrice == null || minPrice === maxPrice) return formatCurrency(minPrice);
  return `${formatCurrency(minPrice)} - ${formatCurrency(maxPrice)}`;
});

const allOutOfStock = computed(() => (product.value?.variants ?? []).every((v) => v.quantityAvailable <= 0 || v.status !== "ACTIVE"));
const canAddToCart = computed(() => matchedVariant.value !== null && matchedVariant.value.quantityAvailable > 0);

async function fetchProduct(slug: string) {
  isLoading.value = true;
  notFound.value = false;
  product.value = null;
  try {
    product.value = await productApi.getBySlug(slug);
    initSelectionFromDefaultVariant();
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      notFound.value = true;
    }
  } finally {
    isLoading.value = false;
  }
}

watch(
  () => route.params.slug,
  (slug) => {
    if (typeof slug === "string") fetchProduct(slug);
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
        <img
          v-if="displayVariant?.imageUrl || product.thumbnailUrl"
          :src="displayVariant?.imageUrl ?? product.thumbnailUrl ?? undefined"
          :alt="product.name"
          class="w-full h-full object-cover"
        />
        <span v-else class="text-neutral-300 text-sm">Không có ảnh</span>
      </div>

      <div>
        <RouterLink v-if="product.categories[0]" to="/" class="text-xs text-neutral-400 hover:text-neutral-600">
          {{ product.categories[0].name }}
        </RouterLink>
        <h1 class="text-xl font-bold text-neutral-900 mt-1 mb-2 text-balance">{{ product.name }}</h1>
        <p class="text-2xl font-bold text-brand mb-4">{{ priceLabel }}</p>

        <div v-for="config in variantAttributeConfigs" :key="config.attributeId" class="mb-4">
          <div class="text-xs text-neutral-500 mb-2">
            {{ config.name }}: <b class="text-neutral-800">{{ config.values.find((v) => v.id === selection[config.attributeId])?.displayValue }}</b>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="value in config.values"
              :key="value.id"
              type="button"
              :disabled="!isValueAvailable(config.attributeId, value.id)"
              class="px-4 py-2 rounded-lg border text-sm transition-colors"
              :class="[
                selection[config.attributeId] === value.id
                  ? 'border-brand bg-brand-soft text-brand font-bold'
                  : 'border-neutral-300 text-neutral-700 hover:border-neutral-400',
                !isValueAvailable(config.attributeId, value.id) ? 'opacity-40 line-through cursor-not-allowed' : '',
              ]"
              @click="selectValue(config.attributeId, value.id)"
            >
              {{ value.displayValue ?? value.value }}
            </button>
          </div>
        </div>

        <div class="flex gap-5 text-xs text-neutral-500 mb-5">
          <span v-if="displayVariant">
            SKU: <b class="text-neutral-800">{{ displayVariant.sku }}</b>
          </span>
          <span v-if="displayVariant">
            Còn lại:
            <b class="text-neutral-800">{{ displayVariant.quantityAvailable > 0 ? displayVariant.quantityAvailable : "Hết hàng" }}</b>
          </span>
        </div>

        <p class="text-sm text-neutral-600 leading-relaxed mb-6 max-w-prose whitespace-pre-line">
          {{ product.shortDescription || product.description || "Chưa có mô tả cho sản phẩm này." }}
        </p>

        <button
          type="button"
          disabled
          :title="allOutOfStock ? 'Sản phẩm đã hết hàng' : 'Chức năng giỏ hàng sẽ có ở bước tiếp theo'"
          class="rounded-lg text-white font-semibold text-sm px-8 py-3 cursor-not-allowed"
          :class="canAddToCart ? 'bg-neutral-300' : 'bg-neutral-300'"
        >
          {{ allOutOfStock ? "Hết hàng" : "Thêm vào giỏ" }}
        </button>
      </div>
    </div>
  </div>
</template>
