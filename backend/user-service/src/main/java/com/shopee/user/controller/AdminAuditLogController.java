package com.shopee.user.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.common.response.PageData;
import com.shopee.user.dto.AuditLogResponse;
import com.shopee.user.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Xem lai audit log - permission AUDIT_LOG_VIEW (xem api-gateway.yml). Chi doc, khong sua/xoa. */
@RestController
@RequestMapping("/api/users/admin/audit-logs")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageData<AuditLogResponse>> list(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(auditLogService.list(module, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AuditLogResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(auditLogService.get(id));
    }
}
