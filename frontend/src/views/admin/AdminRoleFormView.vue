<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import Badge from "@/components/ui/Badge.vue";
import Button from "@/components/ui/Button.vue";
import Icon from "@/components/ui/Icon.vue";
import { adminRoleApi } from "@/api/adminRoleApi";
import { extractErrorMessage } from "@/api/http";
import type { PermissionResponse } from "@/types/admin";

const route = useRoute();
const router = useRouter();

const roleId = computed(() => (typeof route.params.id === "string" ? route.params.id : null));
const isEdit = computed(() => roleId.value !== null);

const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref("");
const fieldErrors = reactive<Record<string, string>>({});

const form = reactive({ code: "", name: "", description: "" });

function validate(): boolean {
  fieldErrors.name = form.name.trim() ? "" : "Vui lòng nhập tên role";
  if (!isEdit.value) {
    fieldErrors.code = /^[A-Z0-9_]+$/.test(form.code.trim()) ? "" : "Mã role chỉ gồm chữ hoa, số và dấu gạch dưới";
  }
  return !Object.values(fieldErrors).some(Boolean);
}

async function loadRole() {
  if (!roleId.value) return;
  isLoading.value = true;
  try {
    const role = await adminRoleApi.get(roleId.value);
    form.code = role.code;
    form.name = role.name;
    form.description = role.description ?? "";
    savedPermissionCodes.value = role.permissionCodes;
    checkedPermissionCodes.value = [...role.permissionCodes];
  } catch {
    errorMessage.value = "Không tải được thông tin role.";
  } finally {
    isLoading.value = false;
  }
}

async function handleSubmit() {
  errorMessage.value = "";
  if (!validate()) return;

  isSubmitting.value = true;
  try {
    if (isEdit.value && roleId.value) {
      await adminRoleApi.update(roleId.value, { name: form.name.trim(), description: form.description.trim() || undefined });
      router.push({ name: "admin-roles" });
    } else {
      await adminRoleApi.create({
        code: form.code.trim().toUpperCase(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
      });
      router.push({ name: "admin-roles" });
    }
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, "Không lưu được role. Vui lòng thử lại.");
  } finally {
    isSubmitting.value = false;
  }
}

// ---------- Permission ----------
// Master-detail: danh sach module ben trai (cuon rieng, khong phinh to theo
// so module), chi tiet quyen cua module dang chon ben phai - khong con phai
// keo dai trang khi so module tang len. Kem 2 nut "Chon tat ca"/"Bo chon tat
// ca" TOAN CUC de gan full quyen trong 1 click, khong phai tich tung module.

const allPermissions = ref<PermissionResponse[]>([]);
const savedPermissionCodes = ref<string[]>([]);
const checkedPermissionCodes = ref<string[]>([]);
const permissionSearch = ref("");
const activeModule = ref<string | null>(null);
const permissionError = ref("");
const permissionSuccess = ref("");
const isSavingPermissions = ref(false);

const isPermissionsDirty = computed(() => {
  const a = [...checkedPermissionCodes.value].sort();
  const b = [...savedPermissionCodes.value].sort();
  return a.length !== b.length || a.some((code, i) => code !== b[i]);
});

const moduleNames = computed(() => [...new Set(allPermissions.value.map((p) => p.module))]);

function permissionsInModule(module: string): PermissionResponse[] {
  return allPermissions.value.filter((p) => p.module === module);
}

function checkedCountInModule(module: string): number {
  return permissionsInModule(module).filter((p) => checkedPermissionCodes.value.includes(p.code)).length;
}

function matchesSearch(permission: PermissionResponse): boolean {
  const query = permissionSearch.value.trim().toLowerCase();
  if (!query) return true;
  return permission.name.toLowerCase().includes(query) || permission.code.toLowerCase().includes(query);
}

// Danh sach hien o cot trai: an bot module khong co permission nao khop tim kiem.
const visibleModules = computed(() =>
  moduleNames.value
    .filter((module) => permissionsInModule(module).some(matchesSearch))
    .map((module) => ({ module, total: permissionsInModule(module).length, checked: checkedCountInModule(module) })),
);

const activeModulePermissions = computed(() => (activeModule.value ? permissionsInModule(activeModule.value).filter(matchesSearch) : []));
const activeModuleStats = computed(() => visibleModules.value.find((g) => g.module === activeModule.value) ?? null);

// Neu module dang xem bi loc mat (do go tim kiem) hoac chua chon gi, tu nhay sang module dau tien con hien.
watch(visibleModules, (groups) => {
  if (!groups.some((g) => g.module === activeModule.value) && groups.length > 0) {
    activeModule.value = groups[0].module;
  }
});

function toggleModule(module: string, checkAll: boolean) {
  const codes = permissionsInModule(module).map((p) => p.code);
  if (checkAll) {
    checkedPermissionCodes.value = [...new Set([...checkedPermissionCodes.value, ...codes])];
  } else {
    checkedPermissionCodes.value = checkedPermissionCodes.value.filter((code) => !codes.includes(code));
  }
}

function selectAllPermissions() {
  checkedPermissionCodes.value = allPermissions.value.map((p) => p.code);
}

function clearAllPermissions() {
  checkedPermissionCodes.value = [];
}

async function loadPermissions() {
  try {
    allPermissions.value = await adminRoleApi.listPermissions();
    activeModule.value = moduleNames.value[0] ?? null;
  } catch {
    allPermissions.value = [];
  }
}

onMounted(() => {
  loadPermissions();
  loadRole();
});

async function savePermissions() {
  if (!roleId.value) return;
  permissionError.value = "";
  permissionSuccess.value = "";
  isSavingPermissions.value = true;
  try {
    const result = await adminRoleApi.setPermissions(roleId.value, { permissionCodes: checkedPermissionCodes.value });
    savedPermissionCodes.value = result.permissionCodes;
    checkedPermissionCodes.value = [...result.permissionCodes];
    permissionSuccess.value = "Đã lưu thay đổi permission.";
  } catch (error) {
    permissionError.value = extractErrorMessage(error, "Không lưu được thay đổi permission.");
  } finally {
    isSavingPermissions.value = false;
  }
}

function resetPermissions() {
  checkedPermissionCodes.value = [...savedPermissionCodes.value];
  permissionError.value = "";
  permissionSuccess.value = "";
}
</script>

<template>
  <div class="max-w-3xl">
    <button type="button" class="flex items-center gap-1.5 text-xs text-neutral-500 hover:text-brand mb-3" @click="router.push({ name: 'admin-roles' })">
      <Icon name="arrow-left" class="w-3.5 h-3.5" /> Quay lại danh sách role
    </button>

    <div class="bg-white border border-neutral-200 rounded-xl overflow-hidden">
      <div class="px-6 py-4 border-b border-neutral-200">
        <h1 class="text-sm font-bold text-neutral-900">{{ isEdit ? "Sửa role" : "Thêm role" }}</h1>
      </div>

      <div v-if="isLoading" class="p-6 text-sm text-neutral-400">Đang tải...</div>

      <form v-else class="p-6 space-y-4" novalidate @submit.prevent="handleSubmit">
        <div v-if="errorMessage" class="flex gap-2 items-start bg-red-50 border border-red-300 text-red-600 text-sm rounded-lg px-3 py-2.5">
          <span aria-hidden="true">⚠</span>
          <span>{{ errorMessage }}</span>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Mã role</label>
            <input
              v-model="form.code"
              type="text"
              :disabled="isEdit"
              placeholder="VD: WAREHOUSE_STAFF"
              class="w-full rounded-lg border px-3 py-2.5 text-sm uppercase focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand disabled:bg-neutral-100 disabled:text-neutral-500"
              :class="fieldErrors.code ? 'border-red-400' : 'border-neutral-300'"
            />
            <p v-if="fieldErrors.code" class="text-xs text-red-600 mt-1">{{ fieldErrors.code }}</p>
            <p v-if="isEdit" class="text-xs text-neutral-400 mt-1">Không thể đổi sau khi tạo</p>
          </div>

          <div>
            <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Tên hiển thị</label>
            <input
              v-model="form.name"
              type="text"
              class="w-full rounded-lg border px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
              :class="fieldErrors.name ? 'border-red-400' : 'border-neutral-300'"
            />
            <p v-if="fieldErrors.name" class="text-xs text-red-600 mt-1">{{ fieldErrors.name }}</p>
          </div>
        </div>

        <div>
          <label class="block text-xs font-semibold text-neutral-600 mb-1.5">Mô tả</label>
          <textarea
            v-model="form.description"
            rows="2"
            class="w-full rounded-lg border border-neutral-300 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
          ></textarea>
        </div>

        <div class="flex gap-2 pt-2">
          <Button type="submit" variant="primary" :disabled="isSubmitting">
            {{ isSubmitting ? "Đang lưu..." : "Lưu role" }}
          </Button>
          <Button variant="secondary" @click="router.push({ name: 'admin-roles' })">Huỷ</Button>
        </div>
      </form>
    </div>

    <div v-if="isEdit && !isLoading" class="bg-white border border-neutral-200 rounded-xl overflow-hidden mt-4">
      <div class="px-6 py-4 border-b border-neutral-200 flex items-center justify-between gap-3 flex-wrap">
        <div>
          <h2 class="text-sm font-bold text-neutral-900">Permission</h2>
          <p class="text-xs text-neutral-500 mt-0.5">
            {{ checkedPermissionCodes.length }}/{{ allPermissions.length }} quyền đã chọn
            <span v-if="allPermissions.length > 0 && checkedPermissionCodes.length === allPermissions.length" class="text-brand-dark font-semibold">
              · Toàn quyền
            </span>
          </p>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <div class="relative">
            <Icon name="search" class="w-4 h-4 absolute left-2.5 top-1/2 -translate-y-1/2 text-neutral-400" />
            <input
              v-model="permissionSearch"
              type="search"
              placeholder="Tìm permission..."
              class="w-48 rounded-lg border border-neutral-300 pl-8 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            />
          </div>
          <Button variant="secondary" @click="selectAllPermissions"><Icon name="check" class="w-3.5 h-3.5" /> Chọn tất cả</Button>
          <Button variant="ghost" @click="clearAllPermissions">Bỏ chọn tất cả</Button>
        </div>
      </div>

      <div class="p-6">
        <div v-if="permissionError" class="text-sm text-red-600 bg-red-50 border border-red-300 rounded-lg px-3 py-2.5 mb-4">
          {{ permissionError }}
        </div>
        <div v-if="permissionSuccess" class="text-sm text-emerald-700 bg-emerald-50 border border-emerald-300 rounded-lg px-3 py-2.5 mb-4">
          {{ permissionSuccess }}
        </div>

        <div v-if="visibleModules.length === 0" class="text-sm text-neutral-400 text-center py-6">Không tìm thấy permission nào khớp.</div>

        <!-- Master-detail: danh sach module trai + chi tiet quyen phai. Khung tu
             co theo noi dung (it module/it quyen thi khong con khoang trang thua),
             chi cham max-height moi cuon rieng tung cot - du sau nay co hang tram
             module cung khong lam trang dai ra. -->
        <div v-else class="flex border border-neutral-200 rounded-lg overflow-hidden max-h-[420px]">
          <div class="w-52 shrink-0 border-r border-neutral-200 bg-neutral-50 overflow-y-auto min-h-0">
            <div
              v-for="group in visibleModules"
              :key="group.module"
              class="flex items-center gap-2 px-3 py-2.5 text-sm border-b border-neutral-100 last:border-b-0 cursor-pointer"
              :class="activeModule === group.module ? 'bg-white font-semibold text-brand-dark' : 'text-neutral-700 hover:bg-white/70'"
              @click="activeModule = group.module"
            >
              <input
                type="checkbox"
                class="rounded border-neutral-300 text-brand focus:ring-brand shrink-0"
                :checked="group.checked === group.total"
                @click.stop
                @change="toggleModule(group.module, ($event.target as HTMLInputElement).checked)"
              />
              <span class="flex-1 truncate">{{ group.module }}</span>
              <Badge :variant="group.checked === group.total ? 'success' : group.checked > 0 ? 'brand' : 'neutral'">
                {{ group.checked }}/{{ group.total }}
              </Badge>
            </div>
          </div>

          <div class="flex-1 overflow-y-auto min-h-0 p-4">
            <template v-if="activeModule">
              <div class="flex items-center justify-between mb-3">
                <h3 class="text-sm font-semibold text-neutral-800">{{ activeModule }}</h3>
                <label v-if="activeModuleStats" class="flex items-center gap-1.5 text-xs text-neutral-500 font-medium cursor-pointer">
                  <input
                    type="checkbox"
                    class="rounded border-neutral-300 text-brand focus:ring-brand"
                    :checked="activeModuleStats.checked === activeModuleStats.total"
                    @change="toggleModule(activeModule, ($event.target as HTMLInputElement).checked)"
                  />
                  Chọn tất cả nhóm này
                </label>
              </div>
              <div class="grid grid-cols-2 gap-1">
                <label
                  v-for="permission in activeModulePermissions"
                  :key="permission.id"
                  class="flex items-start gap-2 text-sm px-2.5 py-1.5 rounded-lg hover:bg-neutral-50 cursor-pointer"
                >
                  <input
                    v-model="checkedPermissionCodes"
                    type="checkbox"
                    :value="permission.code"
                    class="mt-0.5 rounded border-neutral-300 text-brand focus:ring-brand"
                  />
                  <span>
                    <span class="block text-neutral-700">{{ permission.name }}</span>
                    <span class="block text-[10px] text-neutral-400 font-mono">{{ permission.code }}</span>
                  </span>
                </label>
              </div>
            </template>
          </div>
        </div>

        <div class="flex gap-2 pt-5 mt-5 border-t border-neutral-200">
          <Button variant="primary" :disabled="!isPermissionsDirty || isSavingPermissions" @click="savePermissions">
            {{ isSavingPermissions ? "Đang lưu..." : "Lưu thay đổi permission" }}
          </Button>
          <Button variant="secondary" :disabled="!isPermissionsDirty || isSavingPermissions" @click="resetPermissions">Huỷ thay đổi</Button>
        </div>
      </div>
    </div>
  </div>
</template>
