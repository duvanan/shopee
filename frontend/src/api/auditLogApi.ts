import { http } from "@/api/http";
import type { ApiResponse, PageData } from "@/types/api";
import type { AuditLogResponse } from "@/types/admin";

export interface AuditLogListParams {
  module?: string;
  page?: number;
  size?: number;
}

export const auditLogApi = {
  list(params: AuditLogListParams = {}) {
    return http
      .get<ApiResponse<PageData<AuditLogResponse>>>("/api/users/admin/audit-logs", { params })
      .then((res) => res.data.data);
  },
};
