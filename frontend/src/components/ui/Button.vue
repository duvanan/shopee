<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: "primary" | "secondary" | "danger" | "ghost";
    type?: "button" | "submit";
    disabled?: boolean;
  }>(),
  { variant: "primary", type: "button", disabled: false },
);
</script>

<template>
  <button
    :type="type"
    :disabled="disabled"
    class="inline-flex items-center gap-1.5 rounded-lg text-sm font-semibold px-3.5 py-2 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
    :class="{
      'bg-brand text-white hover:bg-brand-dark': variant === 'primary',
      'bg-white border border-neutral-300 text-neutral-700 hover:bg-neutral-50': variant === 'secondary',
      'bg-red-600 text-white hover:bg-red-700': variant === 'danger',
      'text-neutral-500 hover:text-neutral-900': variant === 'ghost',
    }"
  >
    <slot />
  </button>
</template>
