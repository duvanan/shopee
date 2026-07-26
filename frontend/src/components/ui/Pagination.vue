<script setup lang="ts">
// currentPage is 0-based (matches the filters.page convention already used in HomeView.vue);
// the API's own `page` query param is 1-based and is converted at the call site, not here.
defineProps<{ currentPage: number; totalPages: number }>();
const emit = defineEmits<{ "update:currentPage": [page: number] }>();
</script>

<template>
  <div v-if="totalPages > 1" class="flex justify-center gap-1.5">
    <button
      v-for="pageNumber in totalPages"
      :key="pageNumber"
      type="button"
      class="w-8 h-8 rounded-md text-xs border flex items-center justify-center transition-colors"
      :class="pageNumber - 1 === currentPage
        ? 'bg-neutral-900 text-white border-neutral-900'
        : 'bg-white border-neutral-300 text-neutral-600 hover:border-neutral-400'"
      @click="emit('update:currentPage', pageNumber - 1)"
    >
      {{ pageNumber }}
    </button>
  </div>
</template>
