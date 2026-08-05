<script setup lang="ts" generic="T extends { id: string }">
export interface DataTableColumn {
  key: string;
  label: string;
  align?: "left" | "right";
  headerClass?: string;
}

withDefaults(
  defineProps<{
    columns: DataTableColumn[];
    items: T[];
    loading?: boolean;
    error?: string;
    emptyText?: string;
    hasActions?: boolean;
  }>(),
  { loading: false, error: "", emptyText: "Không có dữ liệu.", hasActions: false },
);
</script>

<template>
  <div v-if="error" class="p-6 text-sm text-red-600">{{ error }}</div>
  <div v-else-if="loading" class="p-6 text-sm text-neutral-400">Đang tải...</div>
  <div v-else-if="items.length === 0" class="p-10 text-center text-sm text-neutral-500">{{ emptyText }}</div>

  <div v-else class="overflow-x-auto">
    <table class="w-full text-sm">
      <thead>
        <tr class="text-left text-[11px] uppercase tracking-wide text-neutral-400">
          <th
            v-for="col in columns"
            :key="col.key"
            class="px-5 py-2.5 font-bold"
            :class="[col.align === 'right' ? 'text-right' : '', col.headerClass]"
          >
            {{ col.label }}
          </th>
          <th v-if="hasActions" class="px-5 py-2.5"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id" class="border-t border-neutral-100 hover:bg-neutral-50 align-top">
          <td v-for="col in columns" :key="col.key" class="px-5 py-3" :class="col.align === 'right' ? 'text-right' : ''">
            <slot :name="`cell-${col.key}`" :item="item">{{ (item as Record<string, unknown>)[col.key] }}</slot>
          </td>
          <td v-if="hasActions" class="px-5 py-3">
            <div class="flex justify-end gap-2">
              <slot name="actions" :item="item" />
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
