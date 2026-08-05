import { http } from "@/api/http";
import type { ApiResponse } from "@/types/api";
import type { InventoryImportPayload, InventoryResponse } from "@/types/product";

export const inventoryApi = {
  importStock(variantId: string, payload: InventoryImportPayload) {
    return http
      .post<ApiResponse<InventoryResponse>>(`/api/v1/admin/products/variants/${variantId}/inventory/import`, payload)
      .then((res) => res.data.data);
  },
};
