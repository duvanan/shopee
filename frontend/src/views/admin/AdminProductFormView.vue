<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import axios from "axios";

import Button from "@/components/ui/Button.vue";
import { productApi } from "@/api/productApi";
import { categoryApi } from "@/api/categoryApi";
import { brandApi } from "@/api/brandApi";
import { productTypeApi } from "@/api/productTypeApi";
import { inventoryApi } from "@/api/inventoryApi";
import { extractErrorMessage } from "@/api/http";
import type { ApiErrorBody } from "@/types/auth";
import type {
  AttributeSchemaFieldResponse,
  AttributeSchemaResponse,
  AttributeValueGroup,
  BrandResponse,
  CategoryResponse,
  ProductAttributeValuePayload,
  ProductTypeResponse,
  ProductVariantPayload,
} from "@/types/product";
import { formatCurrency } from "@/utils/format";

const route = useRoute();
const router = useRouter();

const productId = computed(() => (typeof route.params.id === "string" ? route.params.id : null));
const isEdit = computed(() => productId.value !== null);

const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref("");
const fieldErrors = reactive<Record<string, string>>({});

const brands = ref<BrandResponse[]>([]);
const categories = ref<CategoryResponse[]>([]);
const productTypes = ref<ProductTypeResponse[]>([]);

const form = reactive({
  code: "",
  name: "",
  slug: "",
  productTypeId: "",
  structureType: "SIMPLE" as "SIMPLE" | "VARIABLE",
  brandId: "",
  categoryIds: [] as string[],
  primaryCategoryId: "",
  shortDescription: "",
  description: "",
  status: "DRAFT",
  isFeatured: false,
});

// --- Schema dong theo Product Type da chon (chi ap dung khi TAO MOI - Product
// cu khong doi Attribute Set sau khi tao, xem docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md muc 12) ---
const schema = ref<AttributeSchemaResponse | null>(null);
const isLoadingSchema = ref(false);
const schemaError = ref("");

const variantFields = computed<AttributeSchemaFieldResponse[]>(
  () => schema.value?.groups.flatMap((g) => g.attributes.filter((a) => a.variantAttribute)) ?? [],
);
const descriptiveFields = computed<AttributeSchemaFieldResponse[]>(
  () => schema.value?.groups.flatMap((g) => g.attributes.filter((a) => !a.variantAttribute)) ?? [],
);

interface DescriptiveValueState {
  attributeValueId: string;
  valueText: string;
  valueNumber: string;
  valueBoolean: boolean;
  valueDate: string;
}
const descriptiveValues = reactive<Record<string, DescriptiveValueState>>({});
const variantValueSelections = reactive<Record<string, string[]>>({});

function resetAttributeFormState() {
  for (const key of Object.keys(descriptiveValues)) delete descriptiveValues[key];
  for (const key of Object.keys(variantValueSelections)) delete variantValueSelections[key];
}

async function loadSchemaForProductType() {
  schema.value = null;
  schemaError.value = "";
  resetAttributeFormState();
  if (!form.productTypeId) return;
  isLoadingSchema.value = true;
  try {
    schema.value = await productTypeApi.getAttributeSchema(form.productTypeId);
    for (const field of descriptiveFields.value) {
      descriptiveValues[field.attributeId] = { attributeValueId: "", valueText: "", valueNumber: "", valueBoolean: false, valueDate: "" };
    }
    for (const field of variantFields.value) {
      variantValueSelections[field.attributeId] = [];
    }
  } catch {
    schemaError.value = "Không tải được cấu hình thuộc tính của loại sản phẩm này.";
  } finally {
    isLoadingSchema.value = false;
  }
}

watch(() => form.productTypeId, () => {
  if (!isEdit.value) loadSchemaForProductType();
});

interface VariantRow {
  id?: string;
  sku: string;
  name: string;
  attributeValueIds: string[];
  listPrice: string | number;
  salePrice: string | number;
  initialQuantity: string | number;
  quantityAvailable?: number;
}
const variantRows = ref<VariantRow[]>([]);
const variantError = ref("");

async function generateVariants() {
  variantError.value = "";
  const groups: AttributeValueGroup[] = Object.entries(variantValueSelections)
    .filter(([, valueIds]) => valueIds.length > 0)
    .map(([attributeId, valueIds]) => ({ attributeId, valueIds }));

  if (groups.length === 0) {
    variantError.value = "Chọn ít nhất 1 giá trị cho thuộc tính tạo variant trước khi sinh tổ hợp.";
    return;
  }

  try {
    const combinations = await productApi.generateVariantPreview(groups);
    variantRows.value = combinations.map((combo) => ({
      sku: `${form.code || "SKU"}-${combo.attributeValues.map((v) => v.value).join("-")}`,
      name: combo.suggestedName,
      attributeValueIds: combo.attributeValues.map((v) => v.valueId as string),
      listPrice: "",
      salePrice: "",
      initialQuantity: 0,
    }));
  } catch (error) {
    variantError.value = extractErrorMessage(error, "Không sinh được tổ hợp variant.");
  }
}

function ensureSimpleVariantRow() {
  if (variantRows.value.length === 0) {
    variantRows.value = [{ sku: form.code || "SKU", name: form.name || "Mặc định", attributeValueIds: [], listPrice: "", salePrice: "", initialQuantity: 0 }];
  }
}

async function loadReferenceData() {
  const [brandList, categoryList, productTypeList] = await Promise.all([brandApi.list(), categoryApi.listAdmin(), productTypeApi.list()]);
  brands.value = brandList;
  categories.value = categoryList;
  productTypes.value = productTypeList.filter((pt) => pt.status === "ACTIVE");
}

async function loadProduct() {
  if (!productId.value) return;
  const product = await productApi.getById(productId.value);
  form.code = product.code;
  form.name = product.name;
  form.slug = product.slug;
  form.productTypeId = product.productTypeId ?? "";
  form.structureType = product.structureType;
  form.brandId = product.brandId ?? "";
  form.categoryIds = product.categories.map((c) => c.id);
  form.primaryCategoryId = product.categories.find((c) => c.status === "ACTIVE")?.id ?? "";
  form.shortDescription = product.shortDescription ?? "";
  form.description = product.description ?? "";
  form.status = product.status;
  form.isFeatured = product.isFeatured;
  variantRows.value = product.variants.map((v) => ({
    id: v.id,
    sku: v.sku,
    name: v.name,
    attributeValueIds: v.attributeValues.map((av) => av.valueId as string),
    listPrice: v.listPrice,
    salePrice: v.salePrice,
    initialQuantity: 0,
    quantityAvailable: v.quantityAvailable,
  }));

  // Man sua chi cho phep sua GIA TRI cua thuoc tinh mo ta (khong phai variant)
  // - Product Type/Attribute Set/cau truc variant giu nguyen sau khi tao. Phai
  // tu load schema o day (watcher productTypeId bi chan trong che do sua) roi
  // moi de len gia tri thuc te, neu khong descriptiveFields rong se lam
  // buildAttributeValuesPayload() gui mang rong va xoa mat gia tri da luu.
  if (form.productTypeId) {
    await loadSchemaForProductType();
    const configByAttributeId = new Map(product.attributeConfigs.map((c) => [c.attributeId, c]));
    for (const field of descriptiveFields.value) {
      const config = configByAttributeId.get(field.attributeId);
      descriptiveValues[field.attributeId] = {
        attributeValueId: field.values.find((v) => v.displayValue === config?.valueText || v.value === config?.valueText)?.id ?? "",
        valueText: config?.valueText ?? "",
        valueNumber: field.dataType === "INTEGER" || field.dataType === "DECIMAL" ? (config?.valueText ?? "") : "",
        valueBoolean: config?.valueText === "true",
        valueDate: field.dataType === "DATE" || field.dataType === "DATETIME" ? (config?.valueText ?? "") : "",
      };
    }
  }
}

onMounted(async () => {
  isLoading.value = true;
  try {
    await loadReferenceData();
    if (isEdit.value) {
      await loadProduct();
    }
  } catch {
    errorMessage.value = "Không tải được dữ liệu cần thiết.";
  } finally {
    isLoading.value = false;
  }
});

function validate(): boolean {
  fieldErrors.code = form.code.trim() ? "" : "Vui lòng nhập mã sản phẩm";
  fieldErrors.name = form.name.trim() ? "" : "Vui lòng nhập tên sản phẩm";
  fieldErrors.slug = form.slug.trim() ? "" : "Vui lòng nhập slug";
  fieldErrors.categoryIds = form.categoryIds.length > 0 ? "" : "Chọn ít nhất 1 danh mục";
  fieldErrors.productTypeId = isEdit.value || form.productTypeId ? "" : "Chọn loại sản phẩm";
  const hasEmptyVariant = !isEdit.value && variantRows.value.some((r) => !r.sku.trim() || r.listPrice === "" || r.salePrice === "");
  variantError.value = hasEmptyVariant ? "Mỗi variant cần đủ SKU, giá niêm yết, giá bán." : "";
  return !fieldErrors.code && !fieldErrors.name && !fieldErrors.slug && !fieldErrors.categoryIds && !fieldErrors.productTypeId && !hasEmptyVariant;
}

function buildAttributeValuesPayload(fields: AttributeSchemaFieldResponse[]): ProductAttributeValuePayload[] {
  const payload: ProductAttributeValuePayload[] = [];
  for (const field of fields) {
    const state = descriptiveValues[field.attributeId];
    if (!state) continue;
    if (field.values.length > 0) {
      if (state.attributeValueId) payload.push({ attributeId: field.attributeId, attributeValueId: state.attributeValueId });
      continue;
    }
    if (field.dataType === "INTEGER" || field.dataType === "DECIMAL") {
      if (state.valueNumber !== "") payload.push({ attributeId: field.attributeId, valueNumber: Number(state.valueNumber) });
    } else if (field.dataType === "BOOLEAN") {
      payload.push({ attributeId: field.attributeId, valueBoolean: state.valueBoolean });
    } else if (field.dataType === "DATE" || field.dataType === "DATETIME") {
      if (state.valueDate) payload.push({ attributeId: field.attributeId, valueDate: state.valueDate });
    } else if (state.valueText.trim()) {
      payload.push({ attributeId: field.attributeId, valueText: state.valueText.trim() });
    }
  }
  return payload;
}

async function handleSubmit() {
  errorMessage.value = "";
  if (form.structureType === "SIMPLE") ensureSimpleVariantRow();
  if (!validate()) return;

  isSubmitting.value = true;
  try {
    if (isEdit.value && productId.value) {
      await productApi.update(productId.value, {
        name: form.name.trim(),
        slug: form.slug.trim(),
        brandId: form.brandId || null,
        categoryIds: form.categoryIds,
        primaryCategoryId: form.primaryCategoryId || null,
        shortDescription: form.shortDescription.trim() || null,
        description: form.description.trim() || null,
        status: form.status,
        isFeatured: form.isFeatured,
        attributeValues: buildAttributeValuesPayload(descriptiveFields.value),
      });
    } else {
      const variants: ProductVariantPayload[] = variantRows.value.map((r) => ({
        sku: r.sku.trim(),
        name: r.name.trim(),
        attributeValueIds: r.attributeValueIds,
        listPrice: Number(r.listPrice),
        salePrice: Number(r.salePrice),
        initialStocks: [{ quantity: Number(r.initialQuantity) || 0 }],
      }));
      await productApi.create({
        code: form.code.trim(),
        name: form.name.trim(),
        slug: form.slug.trim(),
        productTypeId: form.productTypeId,
        structureType: form.structureType,
        brandId: form.brandId || null,
        categoryIds: form.categoryIds,
        primaryCategoryId: form.primaryCategoryId || null,
        shortDescription: form.shortDescription.trim() || null,
        description: form.description.trim() || null,
        status: form.status,
        isFeatured: form.isFeatured,
        attributeValues: buildAttributeValuesPayload(descriptiveFields.value),
        variants,
      });
    }
    router.push({ name: "admin-products" });
  } catch (error) {
    if (axios.isAxiosError<ApiErrorBody>(error) && error.response?.data.errors) {
      for (const fieldError of error.response.data.errors) {
        fieldErrors[fieldError.field] = fieldError.message;
      }
    }
    errorMessage.value = extractErrorMessage(error, "Không lưu được sản phẩm. Vui lòng thử lại.");
  } finally {
    isSubmitting.value = false;
  }
}

// --- Nhap kho nhanh (chi khi sua san pham) ---
const importingVariantId = ref<string | null>(null);
const importQuantity = ref<string | number>("");
const importError = ref("");

function openImport(variantId: string) {
  importingVariantId.value = variantId;
  importQuantity.value = "";
  importError.value = "";
}

async function submitImport() {
  if (!importingVariantId.value || !importQuantity.value) return;
  try {
    const result = await inventoryApi.importStock(importingVariantId.value, { quantity: Number(importQuantity.value) });
    const row = variantRows.value.find((r) => r.id === importingVariantId.value);
    if (row) row.quantityAvailable = result.quantityAvailable;
    importingVariantId.value = null;
  } catch (error) {
    importError.value = extractErrorMessage(error, "Không nhập kho được — có thể tồn kho vừa bị người khác thay đổi, thử tải lại trang.");
  }
}
</script>

<template>
  <div class="max-w-4xl">
    <button type="button" class="text-xs text-neutral-500 hover:text-brand mb-3" @click="router.push({ name: 'admin-products' })">
      ← Quay lại danh sách sản phẩm
    </button>

    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <div class="px-6 py-4 border-b border-neutral-200">
        <h1 class="text-sm font-bold text-neutral-900">{{ isEdit ? "Sửa sản phẩm" : "Thêm sản phẩm" }}</h1>
      </div>

      <div v-if="isLoading" class="p-6 text-sm text-neutral-400">Đang tải...</div>

      <form v-else class="p-6 space-y-6" novalidate @submit.prevent="handleSubmit">
        <div v-if="errorMessage" class="flex gap-2 items-start bg-red-50 border border-red-300 text-red-600 text-sm rounded-lg px-3 py-2.5">
          <span aria-hidden="true">⚠</span>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- Thong tin chung -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Mã sản phẩm</label>
            <input
              v-model="form.code"
              type="text"
              :disabled="isEdit"
              class="w-full rounded-lg border px-3 py-2.5 text-sm disabled:bg-neutral-50 disabled:text-neutral-400"
              :class="fieldErrors.code ? 'border-red-400' : 'border-neutral-300'"
            />
            <p v-if="fieldErrors.code" class="text-xs text-red-600 mt-1">{{ fieldErrors.code }}</p>
          </div>
          <div>
            <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Cấu trúc</label>
            <select v-model="form.structureType" :disabled="isEdit" class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm disabled:bg-neutral-50">
              <option value="SIMPLE">Đơn giản (1 biến thể)</option>
              <option value="VARIABLE">Có nhiều biến thể (màu/size...)</option>
            </select>
          </div>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Loại sản phẩm</label>
          <select
            v-model="form.productTypeId"
            :disabled="isEdit"
            class="w-full rounded-lg border px-3 py-2.5 text-sm disabled:bg-neutral-50 disabled:text-neutral-400"
            :class="fieldErrors.productTypeId ? 'border-red-400' : 'border-neutral-300'"
          >
            <option value="">— Chọn loại sản phẩm —</option>
            <option v-for="pt in productTypes" :key="pt.id" :value="pt.id">{{ pt.name }}</option>
          </select>
          <p v-if="fieldErrors.productTypeId" class="text-xs text-red-600 mt-1">{{ fieldErrors.productTypeId }}</p>
          <p v-else class="text-xs text-neutral-400 mt-1">Quyết định các thuộc tính sẽ hiện ra bên dưới. Không đổi được sau khi tạo sản phẩm.</p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Tên sản phẩm</label>
          <input
            v-model="form.name"
            type="text"
            class="w-full rounded-lg border px-3 py-2.5 text-sm"
            :class="fieldErrors.name ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.name" class="text-xs text-red-600 mt-1">{{ fieldErrors.name }}</p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Slug</label>
          <input
            v-model="form.slug"
            type="text"
            class="w-full rounded-lg border px-3 py-2.5 text-sm"
            :class="fieldErrors.slug ? 'border-red-400' : 'border-neutral-300'"
          />
          <p v-if="fieldErrors.slug" class="text-xs text-red-600 mt-1">{{ fieldErrors.slug }}</p>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Thương hiệu</label>
            <select v-model="form.brandId" class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm">
              <option value="">— Không có —</option>
              <option v-for="brand in brands" :key="brand.id" :value="brand.id">{{ brand.name }}</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Trạng thái</label>
            <select v-model="form.status" class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm">
              <option value="DRAFT">Nháp</option>
              <option value="ACTIVE">Đang bán</option>
              <option value="INACTIVE">Ngừng bán</option>
            </select>
          </div>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Danh mục</label>
          <div class="flex flex-wrap gap-2">
            <label
              v-for="category in categories"
              :key="category.id"
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-sm cursor-pointer"
              :class="form.categoryIds.includes(category.id) ? 'border-brand bg-brand-soft text-brand' : 'border-neutral-300 text-neutral-600'"
            >
              <input v-model="form.categoryIds" type="checkbox" :value="category.id" class="hidden" />
              {{ category.name }}
            </label>
          </div>
          <p v-if="fieldErrors.categoryIds" class="text-xs text-red-600 mt-1">{{ fieldErrors.categoryIds }}</p>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Mô tả ngắn</label>
          <textarea v-model="form.shortDescription" rows="2" class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm"></textarea>
        </div>

        <label class="flex items-center gap-2 text-sm text-neutral-700">
          <input v-model="form.isFeatured" type="checkbox" /> Sản phẩm nổi bật
        </label>

        <!-- Thuoc tinh mo ta (sinh dong tu schema cua Product Type - sua duoc ca luc tao lan sua) -->
        <div v-if="!isEdit && !form.productTypeId" class="border-t border-neutral-200 pt-5">
          <p class="text-xs text-neutral-400">Chọn loại sản phẩm ở trên để hiện các thuộc tính tương ứng.</p>
        </div>
        <div v-else-if="isLoadingSchema" class="border-t border-neutral-200 pt-5 text-xs text-neutral-400">Đang tải thuộc tính...</div>
        <div v-else-if="schemaError" class="border-t border-neutral-200 pt-5 text-xs text-red-600">{{ schemaError }}</div>
        <template v-else-if="descriptiveFields.length > 0">
          <div class="border-t border-neutral-200 pt-5">
            <h2 class="text-sm font-bold text-neutral-900 mb-3">Thuộc tính mô tả</h2>
            <div class="grid grid-cols-2 gap-4">
              <div v-for="field in descriptiveFields" :key="field.attributeId">
                <label class="block text-xs font-semibold text-neutral-600 mb-1.5">
                  {{ field.label }} <span v-if="field.required" class="text-red-500">*</span>
                </label>

                <select
                  v-if="field.values.length > 0 && descriptiveValues[field.attributeId]"
                  v-model="descriptiveValues[field.attributeId].attributeValueId"
                  class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm"
                >
                  <option value="">— Chọn —</option>
                  <option v-for="value in field.values" :key="value.id" :value="value.id">{{ value.displayValue ?? value.value }}</option>
                </select>

                <input
                  v-else-if="(field.dataType === 'INTEGER' || field.dataType === 'DECIMAL') && descriptiveValues[field.attributeId]"
                  v-model="descriptiveValues[field.attributeId].valueNumber"
                  type="number"
                  :placeholder="field.placeholder ?? ''"
                  class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm"
                />

                <label v-else-if="field.dataType === 'BOOLEAN' && descriptiveValues[field.attributeId]" class="flex items-center gap-2 text-sm text-neutral-700 mt-1">
                  <input v-model="descriptiveValues[field.attributeId].valueBoolean" type="checkbox" /> Có
                </label>

                <input
                  v-else-if="(field.dataType === 'DATE' || field.dataType === 'DATETIME') && descriptiveValues[field.attributeId]"
                  v-model="descriptiveValues[field.attributeId].valueDate"
                  type="date"
                  class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm"
                />

                <input
                  v-else-if="descriptiveValues[field.attributeId]"
                  v-model="descriptiveValues[field.attributeId].valueText"
                  type="text"
                  :placeholder="field.placeholder ?? ''"
                  class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm"
                />
                <p v-if="field.helpText" class="text-xs text-neutral-400 mt-1">{{ field.helpText }}</p>
              </div>
            </div>
          </div>
        </template>

        <!-- Thuoc tinh tao variant + bang variant (chi khi tao moi) -->
        <template v-if="!isEdit">
          <div v-if="variantFields.length > 0" class="border-t border-neutral-200 pt-5">
            <h2 class="text-sm font-bold text-neutral-900 mb-3">Thuộc tính tạo biến thể</h2>
            <div class="space-y-3">
              <div v-for="field in variantFields" :key="field.attributeId" class="border border-neutral-200 rounded-lg p-3">
                <p class="text-sm font-medium text-neutral-800 mb-2">
                  {{ field.label }} <span v-if="field.required" class="text-red-500">*</span>
                  <span class="text-xs text-neutral-400 font-mono">{{ field.code }}</span>
                </p>
                <div class="flex flex-wrap gap-2">
                  <label
                    v-for="value in field.values"
                    :key="value.id"
                    class="flex items-center gap-1.5 px-2.5 py-1 rounded-md border text-xs cursor-pointer"
                    :class="variantValueSelections[field.attributeId]?.includes(value.id) ? 'border-brand bg-brand-soft text-brand' : 'border-neutral-300 text-neutral-600'"
                  >
                    <input v-model="variantValueSelections[field.attributeId]" type="checkbox" :value="value.id" class="hidden" />
                    {{ value.displayValue ?? value.value }}
                  </label>
                </div>
              </div>
            </div>

            <Button v-if="form.structureType === 'VARIABLE'" type="button" variant="secondary" class="mt-3" @click="generateVariants">
              Sinh tổ hợp variant
            </Button>
          </div>

          <div class="border-t border-neutral-200 pt-5">
            <h2 class="text-sm font-bold text-neutral-900 mb-3">Biến thể (SKU / giá / tồn kho ban đầu)</h2>
            <p v-if="variantError" class="text-xs text-red-600 mb-2">{{ variantError }}</p>
            <p v-if="form.structureType === 'VARIABLE' && variantRows.length === 0" class="text-xs text-neutral-400">
              Chọn giá trị thuộc tính rồi bấm "Sinh tổ hợp variant" ở trên.
            </p>
            <div v-if="form.structureType === 'SIMPLE'" class="mb-3">
              <Button type="button" variant="secondary" @click="ensureSimpleVariantRow">Tạo variant mặc định</Button>
            </div>

            <table v-if="variantRows.length > 0" class="w-full text-xs border border-neutral-200 rounded-lg overflow-hidden">
              <thead class="bg-neutral-50 text-neutral-500 uppercase text-[10px]">
                <tr>
                  <th class="px-3 py-2 text-left">Tên</th>
                  <th class="px-3 py-2 text-left">SKU</th>
                  <th class="px-3 py-2 text-left">Giá niêm yết</th>
                  <th class="px-3 py-2 text-left">Giá bán</th>
                  <th class="px-3 py-2 text-left">Tồn kho ban đầu</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in variantRows" :key="index" class="border-t border-neutral-100">
                  <td class="px-3 py-2">{{ row.name }}</td>
                  <td class="px-3 py-2"><input v-model="row.sku" class="w-full rounded border border-neutral-300 px-2 py-1" /></td>
                  <td class="px-3 py-2"><input v-model="row.listPrice" type="number" min="0" class="w-24 rounded border border-neutral-300 px-2 py-1" /></td>
                  <td class="px-3 py-2"><input v-model="row.salePrice" type="number" min="0" class="w-24 rounded border border-neutral-300 px-2 py-1" /></td>
                  <td class="px-3 py-2"><input v-model="row.initialQuantity" type="number" min="0" class="w-20 rounded border border-neutral-300 px-2 py-1" /></td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>

        <!-- Xem/nhap kho khi da tao (che do sua) -->
        <div v-else-if="variantRows.length > 0" class="border-t border-neutral-200 pt-5">
          <h2 class="text-sm font-bold text-neutral-900 mb-3">Biến thể &amp; tồn kho</h2>
          <table class="w-full text-xs border border-neutral-200 rounded-lg overflow-hidden">
            <thead class="bg-neutral-50 text-neutral-500 uppercase text-[10px]">
              <tr>
                <th class="px-3 py-2 text-left">Tên</th>
                <th class="px-3 py-2 text-left">SKU</th>
                <th class="px-3 py-2 text-left">Giá bán</th>
                <th class="px-3 py-2 text-left">Tồn khả dụng</th>
                <th class="px-3 py-2"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in variantRows" :key="row.id" class="border-t border-neutral-100">
                <td class="px-3 py-2">{{ row.name }}</td>
                <td class="px-3 py-2 font-mono">{{ row.sku }}</td>
                <td class="px-3 py-2">{{ formatCurrency(Number(row.salePrice)) }}</td>
                <td class="px-3 py-2">{{ row.quantityAvailable }}</td>
                <td class="px-3 py-2">
                  <Button type="button" variant="secondary" @click="openImport(row.id as string)">+ Nhập kho</Button>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="importingVariantId" class="mt-3 flex items-center gap-2">
            <input v-model="importQuantity" type="number" min="1" placeholder="Số lượng nhập" class="w-32 rounded-lg border border-neutral-300 px-3 py-2 text-sm" />
            <Button type="button" variant="primary" @click="submitImport">Xác nhận</Button>
            <Button type="button" variant="secondary" @click="importingVariantId = null">Huỷ</Button>
            <span v-if="importError" class="text-xs text-red-600">{{ importError }}</span>
          </div>
        </div>

        <div class="flex gap-2 pt-2">
          <Button type="submit" variant="primary" :disabled="isSubmitting">
            {{ isSubmitting ? "Đang lưu..." : "Lưu sản phẩm" }}
          </Button>
          <Button variant="secondary" @click="router.push({ name: 'admin-products' })">Huỷ</Button>
        </div>
      </form>
    </div>
  </div>
</template>
