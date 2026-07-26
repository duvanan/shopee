<script setup lang="ts">
import Button from "@/components/ui/Button.vue";

withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    message?: string;
    confirmLabel?: string;
    errorMessage?: string | null;
    loading?: boolean;
  }>(),
  { message: "Hành động này không thể hoàn tác.", confirmLabel: "Xoá", errorMessage: null, loading: false },
);

const emit = defineEmits<{ confirm: []; cancel: [] }>();
</script>

<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4" @click.self="emit('cancel')">
    <div class="w-full max-w-sm rounded-xl bg-white p-5 shadow-lg">
      <h3 class="text-sm font-bold text-neutral-900 mb-1.5">{{ title }}</h3>
      <p v-if="!errorMessage" class="text-sm text-neutral-500 mb-4">{{ message }}</p>
      <div v-if="errorMessage" class="text-sm rounded-lg bg-red-50 border border-red-300 text-red-600 px-3 py-2.5 mb-4">
        {{ errorMessage }}
      </div>
      <div class="flex justify-end gap-2">
        <Button variant="secondary" @click="emit('cancel')">Huỷ</Button>
        <Button v-if="!errorMessage" variant="danger" :disabled="loading" @click="emit('confirm')">{{ confirmLabel }}</Button>
      </div>
    </div>
  </div>
</template>
