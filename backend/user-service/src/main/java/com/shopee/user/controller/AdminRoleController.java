package com.shopee.user.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.user.dto.PermissionResponse;
import com.shopee.user.dto.RoleCreateRequest;
import com.shopee.user.dto.RoleDetailResponse;
import com.shopee.user.dto.RoleResponse;
import com.shopee.user.dto.RoleUpdateRequest;
import com.shopee.user.dto.SetRolePermissionsRequest;
import com.shopee.user.service.AdminRoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Quan ly Role/Permission - permission ROLE_MANAGE (xem api-gateway.yml). */
@RestController
@RequestMapping("/api/users/admin")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    public AdminRoleController(AdminRoleService adminRoleService) {
        this.adminRoleService = adminRoleService;
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success(adminRoleService.listRoles());
    }

    @GetMapping("/roles/{id}")
    public ApiResponse<RoleDetailResponse> getRole(@PathVariable UUID id) {
        return ApiResponse.success(adminRoleService.getRole(id));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @RequestHeader("X-User-Id") UUID actorId, @Valid @RequestBody RoleCreateRequest request) {
        RoleResponse response = adminRoleService.createRole(actorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("ROLE_CREATED", "Đã tạo role", response));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<RoleResponse> updateRole(
            @RequestHeader("X-User-Id") UUID actorId, @PathVariable UUID id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success("ROLE_UPDATED", "Đã cập nhật role", adminRoleService.updateRole(actorId, id, request));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@RequestHeader("X-User-Id") UUID actorId, @PathVariable UUID id) {
        adminRoleService.deleteRole(actorId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/roles/{id}/permissions")
    public ApiResponse<RoleDetailResponse> setPermissions(
            @RequestHeader("X-User-Id") UUID actorId, @PathVariable UUID id,
            @Valid @RequestBody SetRolePermissionsRequest request) {
        return ApiResponse.success("ROLE_PERMISSIONS_UPDATED", "Đã cập nhật permission",
                adminRoleService.setPermissions(actorId, id, request));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ApiResponse.success(adminRoleService.listPermissions());
    }
}
