<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import AdminPageHeader from "@/components/ui/AdminPageHeader.vue";
import Badge from "@/components/ui/Badge.vue";
import Button from "@/components/ui/Button.vue";
import ConfirmDialog from "@/components/ui/ConfirmDialog.vue";
import DataTable, { type DataTableColumn } from "@/components/ui/DataTable.vue";
import Icon from "@/components/ui/Icon.vue";
import { productTypeApi } from "@/api/productTypeApi";
import { extractErrorMessage } from "@/api/http";
import type { ProductTypeResponse } from "@/types/product";

const columns: DataTableColumn[] = [
  { key: "name", label: "Loại sản phẩm" },
  { key: "code", label: "Mã" },
  { key: "attributeSet", label: "Bộ thuộc tính mặc định" },
  { key: "productCount", label: "Số sản phẩm" },
  { key: "status", label: "Trạng thái" },
];

const productTypes = ref<ProductTypeResponse[]>([]);
const isLoading = ref(false);
const errorMessage = ref("");

async function fetchProductTypes() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    productTypes.value = await productTypeApi.list();
  } catch {
    errorMessage.value = "Không tải được danh sách loại sản phẩm. Vui lòng thử lại.";
  } finally {
    isLoading.value = false;
  }
}

onMounted(fetchProductTypes);

const deleteTarget = ref<ProductTypeResponse | null>(null);
const deleteError = ref<string | null>(null);
const isDeleting = ref(false);

function askDelete(productType: ProductTypeResponse) {
  deleteTarget.value = productType;
  deleteError.value = null;
}

async function confirmDelete() {
  if (!deleteTarget.value) return;
  isDeleting.value = true;
  try {
    await productTypeApi.remove(deleteTarget.value.id);
    deleteTarget.value = null;
    await fetchProductTypes();
  } catch (error) {
    deleteError.value = extractErrorMessage(error, "Không xoá được loại sản phẩm này — có thể đang có sản phẩm dùng nó.");
  } finally {
    isDeleting.value = false;
  }
}

async function toggleStatus(productType: ProductTypeResponse) {
  try {
    if (productType.status === "ACTIVE") {
      await productTypeApi.deactivate(productType.id);
    } else {
      await productTypeApi.activate(productType.id);
    }
    await fetchProductTypes();
  } catch {
    errorMessage.value = "Không đổi được trạng thái.";
  }
}
</script>

<template>
  <div>
    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <AdminPageHeader title="Loại sản phẩm" :subtitle="`${productTypes.length} loại sản phẩm • quyết định bộ thuộc tính khi tạo sản phẩm`">
        <RouterLink :to="{ name: 'admin-product-type-new' }">
          <Button variant="primary"><Icon name="plus" class="w-4 h-4" /> Thêm loại sản phẩm</Button>
        </RouterLink>
      </AdminPageHeader>

      <DataTable :columns="columns" :items="productTypes" :loading="isLoading" :error="errorMessage" empty-text="Chưa có loại sản phẩm nào." has-actions>
        <template #cell-name="{ item }">
          <span class="text-neutral-800">{{ item.name }}</span>
        </template>
        <template #cell-code="{ item }">
          <span class="text-neutral-500 font-mono text-xs">{{ item.code }}</span>
        </template>
        <template #cell-attributeSet="{ item }">
          <span class="text-neutral-500">{{ item.defaultAttributeSetName ?? "— Chưa gán —" }}</span>
        </template>
        <template #cell-productCount="{ item }">
          <span class="text-neutral-500">{{ item.productCount }}</span>
        </template>
        <template #cell-status="{ item }">
          <Badge :variant="item.status === 'ACTIVE' ? 'success' : 'warning'">{{ item.status === "ACTIVE" ? "Hoạt động" : "Ngừng" }}</Badge>
        </template>
        <template #actions="{ item }">
          <Button variant="secondary" @click="toggleStatus(item)">{{ item.status === "ACTIVE" ? "Ngừng" : "Kích hoạt" }}</Button>
          <RouterLink :to="{ name: 'admin-product-type-edit', params: { id: item.id } }">
            <Button variant="secondary"><Icon name="edit" class="w-3.5 h-3.5" /> Sửa</Button>
          </RouterLink>
          <Button variant="danger" @click="askDelete(item)"><Icon name="trash" class="w-3.5 h-3.5" /> Xoá</Button>
        </template>
      </DataTable>
    </div>

    <ConfirmDialog
      :open="deleteTarget !== null"
      :title="`Xoá loại sản phẩm '${deleteTarget?.name}'?`"
      :error-message="deleteError"
      :loading="isDeleting"
      @cancel="deleteTarget = null"
      @confirm="confirmDelete"
    />
  </div>
</template>
