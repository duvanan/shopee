<script setup lang="ts">
defineProps<{ title: string; subtitle?: string }>();
</script>

<template>
  <div class="flex items-center justify-between px-5 py-4 border-b border-neutral-200 flex-wrap gap-3">
    <div>
      <h1 class="text-sm font-bold text-neutral-900">{{ title }}</h1>
      <p v-if="subtitle" class="text-xs text-neutral-500 mt-0.5">{{ subtitle }}</p>
    </div>
    <div class="flex items-center gap-2">
      <slot />
    </div>
  </div>
</template>
