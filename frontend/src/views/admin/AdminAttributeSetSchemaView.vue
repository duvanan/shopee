<script setup lang="ts">
/**
 * Builder cau hinh Attribute Set: nhom + thuoc tinh trong tung nhom. Khong
 * lam drag-drop (don gian hoa co chu dinh so voi spec goc, xem trao doi voi
 * BA) - dung form/checkbox thuan, sap xep bang o nhap sortOrder.
 */
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import Button from "@/components/ui/Button.vue";
import Icon from "@/components/ui/Icon.vue";
import { attributeSetApi } from "@/api/attributeSetApi";
import { attributeApi } from "@/api/attributeApi";
import { extractErrorMessage } from "@/api/http";
import type { AttributeSetResponse, ProductAttributeResponse, SetAttributeSetSchemaPayload } from "@/types/product";

const route = useRoute();
const router = useRouter();
const attributeSetId = computed(() => route.params.id as string);

const isLoading = ref(false);
const isSaving = ref(false);
const errorMessage = ref("");
const saveSuccess = ref(false);

const attributeSet = ref<AttributeSetResponse | null>(null);
const allAttributes = ref<ProductAttributeResponse[]>([]);

interface EditableAttr {
  attributeId: string;
  code: string;
  name: string;
  labelOverride: string;
  helpText: string;
  placeholder: string;
  isRequired: boolean;
  isVariantAttribute: boolean;
  isFilterable: boolean;
  isSearchable: boolean;
  isComparable: boolean;
  isVisible: boolean;
  isEditable: boolean;
}

interface EditableGroup {
  code: string;
  name: string;
  description: string;
  isCollapsible: boolean;
  isDefaultExpanded: boolean;
  attributes: EditableAttr[];
}

const groups = ref<EditableGroup[]>([]);

const usedAttributeIds = computed(() => new Set(groups.value.flatMap((g) => g.attributes.map((a) => a.attributeId))));
const availableAttributes = computed(() => allAttributes.value.filter((a) => !usedAttributeIds.value.has(a.id)));

function addGroup() {
  groups.value.push({
    code: "",
    name: "",
    description: "",
    isCollapsible: true,
    isDefaultExpanded: true,
    attributes: [],
  });
}

function removeGroup(index: number) {
  groups.value.splice(index, 1);
}

function addAttributeToGroup(group: EditableGroup, attributeId: string) {
  if (!attributeId) return;
  const attribute = allAttributes.value.find((a) => a.id === attributeId);
  if (!attribute) return;
  group.attributes.push({
    attributeId: attribute.id,
    code: attribute.code,
    name: attribute.name,
    labelOverride: attribute.name,
    helpText: "",
    placeholder: "",
    isRequired: false,
    isVariantAttribute: attribute.allowVariant,
    isFilterable: attribute.isFilterable,
    isSearchable: attribute.isSearchable,
    isComparable: attribute.isComparable,
    isVisible: true,
    isEditable: true,
  });
}

function removeAttribute(group: EditableGroup, index: number) {
  group.attributes.splice(index, 1);
}

async function load() {
  isLoading.value = true;
  errorMessage.value = "";
  try {
    const [set, schema, attributes] = await Promise.all([
      attributeSetApi.get(attributeSetId.value),
      attributeSetApi.getSchema(attributeSetId.value),
      attributeApi.list(),
    ]);
    attributeSet.value = set;
    allAttributes.value = attributes;
    groups.value = schema.groups.map((g) => ({
      code: g.code,
      name: g.name,
      description: "",
      isCollapsible: g.collapsible,
      isDefaultExpanded: g.defaultExpanded,
      attributes: g.attributes.map((a) => ({
        attributeId: a.attributeId,
        code: a.code,
        name: a.name,
        labelOverride: a.label,
        helpText: a.helpText ?? "",
        placeholder: a.placeholder ?? "",
        isRequired: a.required,
        isVariantAttribute: a.variantAttribute,
        isFilterable: a.filterable,
        isSearchable: a.searchable,
        isComparable: a.comparable,
        isVisible: a.visible,
        isEditable: a.editable,
      })),
    }));
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, "Không tải được cấu hình bộ thuộc tính.");
  } finally {
    isLoading.value = false;
  }
}

onMounted(load);

async function handleSave() {
  errorMessage.value = "";
  saveSuccess.value = false;

  for (const group of groups.value) {
    if (!group.code.trim() || !group.name.trim()) {
      errorMessage.value = "Mỗi nhóm cần có mã và tên.";
      return;
    }
  }

  const payload: SetAttributeSetSchemaPayload = {
    groups: groups.value.map((g, groupIndex) => ({
      code: g.code.trim(),
      name: g.name.trim(),
      description: g.description.trim() || null,
      isCollapsible: g.isCollapsible,
      isDefaultExpanded: g.isDefaultExpanded,
      sortOrder: groupIndex,
      attributes: g.attributes.map((a, attrIndex) => ({
        attributeId: a.attributeId,
        labelOverride: a.labelOverride.trim() || null,
        helpText: a.helpText.trim() || null,
        placeholder: a.placeholder.trim() || null,
        isRequired: a.isRequired,
        isVariantAttribute: a.isVariantAttribute,
        isFilterable: a.isFilterable,
        isSearchable: a.isSearchable,
        isComparable: a.isComparable,
        isVisible: a.isVisible,
        isEditable: a.isEditable,
        sortOrder: attrIndex,
      })),
    })),
  };

  isSaving.value = true;
  try {
    await attributeSetApi.setSchema(attributeSetId.value, payload);
    saveSuccess.value = true;
    await load();
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, "Không lưu được cấu hình. Vui lòng thử lại.");
  } finally {
    isSaving.value = false;
  }
}
</script>

<template>
  <div class="max-w-4xl">
    <button type="button" class="text-xs text-neutral-500 hover:text-brand mb-3" @click="router.push({ name: 'admin-attribute-sets' })">
      ← Quay lại danh sách bộ thuộc tính
    </button>

    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <div class="px-6 py-4 border-b border-neutral-200 flex items-center justify-between">
        <div>
          <h1 class="text-sm font-bold text-neutral-900">Cấu hình "{{ attributeSet?.name }}"</h1>
          <p class="text-xs text-neutral-400 mt-0.5">Phiên bản hiện tại: v{{ attributeSet?.versionNo }} — lưu sẽ tăng phiên bản, sản phẩm cũ không tự đổi theo.</p>
        </div>
        <Button type="button" variant="primary" :disabled="isSaving" @click="handleSave">{{ isSaving ? "Đang lưu..." : "Lưu cấu hình" }}</Button>
      </div>

      <div v-if="isLoading" class="p-6 text-sm text-neutral-400">Đang tải...</div>

      <div v-else class="p-6 space-y-5">
        <div v-if="errorMessage" class="flex gap-2 items-start bg-red-50 border border-red-300 text-red-600 text-sm rounded-lg px-3 py-2.5">
          <span aria-hidden="true">⚠</span>
          <span>{{ errorMessage }}</span>
        </div>
        <div v-if="saveSuccess" class="bg-emerald-50 border border-emerald-300 text-emerald-700 text-sm rounded-lg px-3 py-2.5">
          ✓ Đã lưu cấu hình.
        </div>

        <div v-for="(group, groupIndex) in groups" :key="groupIndex" class="border border-neutral-200 rounded-lg overflow-hidden">
          <div class="bg-neutral-50 px-4 py-3 flex flex-wrap items-center gap-2">
            <input v-model="group.code" placeholder="Mã nhóm (VD: GENERAL)" class="w-40 rounded-lg border border-neutral-300 px-2.5 py-1.5 text-xs" />
            <input v-model="group.name" placeholder="Tên nhóm (VD: Thông tin chung)" class="flex-1 min-w-[180px] rounded-lg border border-neutral-300 px-2.5 py-1.5 text-xs" />
            <label class="flex items-center gap-1.5 text-xs text-neutral-600"><input v-model="group.isCollapsible" type="checkbox" /> Thu gọn được</label>
            <label class="flex items-center gap-1.5 text-xs text-neutral-600"><input v-model="group.isDefaultExpanded" type="checkbox" /> Mở sẵn</label>
            <Button type="button" variant="danger" @click="removeGroup(groupIndex)"><Icon name="trash" class="w-3.5 h-3.5" /></Button>
          </div>

          <div v-if="group.attributes.length === 0" class="px-4 py-4 text-xs text-neutral-400">Chưa có thuộc tính nào trong nhóm này.</div>

          <table v-else class="w-full text-xs">
            <thead class="bg-white text-neutral-400 uppercase text-[10px] border-b border-neutral-100">
              <tr>
                <th class="px-3 py-2 text-left">Thuộc tính</th>
                <th class="px-3 py-2 text-left">Nhãn hiển thị</th>
                <th class="px-2 py-2 text-center">Bắt buộc</th>
                <th class="px-2 py-2 text-center">Tạo variant</th>
                <th class="px-2 py-2 text-center">Lọc</th>
                <th class="px-2 py-2 text-center">Hiển thị</th>
                <th class="px-2 py-2"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(attr, attrIndex) in group.attributes" :key="attr.attributeId" class="border-b border-neutral-50">
                <td class="px-3 py-2">
                  {{ attr.name }} <span class="text-neutral-400 font-mono">{{ attr.code }}</span>
                </td>
                <td class="px-3 py-2"><input v-model="attr.labelOverride" class="w-full rounded border border-neutral-300 px-2 py-1" /></td>
                <td class="px-2 py-2 text-center"><input v-model="attr.isRequired" type="checkbox" /></td>
                <td class="px-2 py-2 text-center"><input v-model="attr.isVariantAttribute" type="checkbox" /></td>
                <td class="px-2 py-2 text-center"><input v-model="attr.isFilterable" type="checkbox" /></td>
                <td class="px-2 py-2 text-center"><input v-model="attr.isVisible" type="checkbox" /></td>
                <td class="px-2 py-2 text-center">
                  <button type="button" class="text-neutral-400 hover:text-red-600" @click="removeAttribute(group, attrIndex)">
                    <Icon name="trash" class="w-3.5 h-3.5" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="px-4 py-3 border-t border-neutral-100">
            <select
              class="rounded-lg border border-neutral-300 px-2.5 py-1.5 text-xs"
              @change="addAttributeToGroup(group, ($event.target as HTMLSelectElement).value); ($event.target as HTMLSelectElement).value = ''"
            >
              <option value="">+ Thêm thuộc tính vào nhóm...</option>
              <option v-for="attribute in availableAttributes" :key="attribute.id" :value="attribute.id">{{ attribute.name }} ({{ attribute.code }})</option>
            </select>
          </div>
        </div>

        <Button type="button" variant="secondary" @click="addGroup"><Icon name="plus" class="w-4 h-4" /> Thêm nhóm</Button>
      </div>
    </div>
  </div>
</template>
