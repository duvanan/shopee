<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import Button from "@/components/ui/Button.vue";
import { categoryApi } from "@/api/categoryApi";
import { extractErrorMessage } from "@/api/http";

const route = useRoute();
const router = useRouter();

const categoryId = computed(() => (typeof route.params.id === "string" ? route.params.id : null));
const isEdit = computed(() => categoryId.value !== null);

const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref("");
const fieldErrors = reactive<Record<string, string>>({});

const form = reactive({
  name: "",
  slug: "",
});

function validate(): boolean {
  fieldErrors.name = form.name.trim() ? "" : "Vui lòng nhập tên danh mục";
  return !fieldErrors.name;
}

async function loadCategory() {
  if (!categoryId.value) return;
  isLoading.value = true;
  try {
    // Backend chua co GET /api/categories/{id} - tim trong list day du.
    const categories = await categoryApi.list();
    const category = categories.find((item) => item.id === categoryId.value);
    if (!category) {
      errorMessage.value = "Không tìm thấy danh mục này.";
      return;
    }
    form.name = category.name;
    form.slug = category.slug ?? "";
  } catch {
    errorMessage.value = "Không tải được thông tin danh mục.";
  } finally {
    isLoading.value = false;
  }
}

onMounted(loadCategory);

async function handleSubmit() {
  errorMessage.value = "";
  if (!validate()) return;

  const payload = { name: form.name.trim(), slug: form.slug.trim() || null };

  isSubmitting.value = true;
  try {
    if (isEdit.value && categoryId.value) {
      await categoryApi.update(categoryId.value, payload);
    } else {
      await categoryApi.create(payload);
    }
    router.push({ name: "admin-categories" });
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, "Không lưu được danh mục. Vui lòng thử lại.");
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="max-w-lg">
    <button type="button" class="text-xs text-neutral-500 hover:text-brand mb-3" @click="router.push({ name: 'admin-categories' })">
      ← Quay lại danh sách danh mục
    </button>

    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <div class="px-6 py-4 border-b border-neutral-200">
        <h1 class="text-sm font-bold text-neutral-900">{{ isEdit ? "Sửa danh mục" : "Thêm danh mục" }}</h1>
      </div>

      <div v-if="isLoading" class="p-6 text-sm text-neutral-400">Đang tải...</div>

      <form v-else class="p-6 space-y-4" novalidate @submit.prevent="handleSubmit">
        <div v-if="errorMessage" class="flex gap-2 items-start bg-red-50 border border-red-300 text-red-600 text-sm rounded-lg px-3 py-2.5">
          <span aria-hidden="true">⚠</span>
          <span>{{ errorMessage }}</span>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Tên danh mục</label>
          <input
            v-model="form.name"
            type="text"
            placeholder="VD: Phụ kiện điện thoại"
            class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            :class="fieldErrors.name ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.name" class="text-xs text-red-600 mt-1">{{ fieldErrors.name }}</p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Slug</label>
          <input
            v-model="form.slug"
            type="text"
            placeholder="vd-phu-kien-dien-thoai"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          />
          <p class="text-xs text-neutral-400 mt-1">Dùng trong đường dẫn URL, chỉ chữ thường và dấu gạch ngang</p>
        </div>

        <div class="flex gap-2 pt-2">
          <Button type="submit" variant="primary" :disabled="isSubmitting">
            {{ isSubmitting ? "Đang lưu..." : "Lưu danh mục" }}
          </Button>
          <Button variant="secondary" @click="router.push({ name: 'admin-categories' })">Huỷ</Button>
        </div>
      </form>
    </div>
  </div>
</template>
