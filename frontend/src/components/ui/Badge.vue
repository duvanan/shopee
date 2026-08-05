<script setup lang="ts">
withDefaults(defineProps<{ variant?: "success" | "warning" | "danger" | "neutral" | "brand" }>(), {
  variant: "neutral",
});
</script>

<template>
  <span
    class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold whitespace-nowrap"
    :class="{
      'bg-emerald-50 text-emerald-700': variant === 'success',
      'bg-amber-50 text-amber-700': variant === 'warning',
      'bg-red-50 text-red-600': variant === 'danger',
      'bg-neutral-100 text-neutral-500': variant === 'neutral',
      'bg-brand-soft text-brand-dark': variant === 'brand',
    }"
  >
    <slot />
  </span>
</template>
