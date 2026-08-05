package com.shopee.user.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.common.response.PageData;
import com.shopee.user.dto.AdminCreateUserRequest;
import com.shopee.user.dto.AdminUpdateUserRequest;
import com.shopee.user.dto.AdminUserResponse;
import com.shopee.user.dto.AssignRoleRequest;
import com.shopee.user.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API quan tri User/Role - permission USER_MANAGE (xem api-gateway.yml).
 * Khong dung @PreAuthorize - Gateway da chan truoc khi request toi duoc,
 * cung quy uoc voi ProductController/CategoryController.
 */
@RestController
@RequestMapping("/api/users/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public ApiResponse<PageData<AdminUserResponse>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminUserService.listUsers(search, page, size));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable UUID id) {
        return ApiResponse.success(adminUserService.getUser(id));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createUser(
            @RequestHeader("X-User-Id") UUID actorId, @Valid @RequestBody AdminCreateUserRequest request) {
        AdminUserResponse response = adminUserService.createUser(actorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("USER_CREATED", "Đã tạo tài khoản", response));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<AdminUserResponse> updateUser(
            @RequestHeader("X-User-Id") UUID actorId, @PathVariable UUID id, @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.success("USER_UPDATED", "Đã cập nhật thông tin", adminUserService.updateUser(actorId, id, request));
    }

    @PostMapping("/users/{id}/lock")
    public ApiResponse<Void> lockUser(@RequestHeader("X-User-Id") UUID actorId, @PathVariable UUID id) {
        adminUserService.lockUser(actorId, id);
        return ApiResponse.success("USER_LOCKED", "Đã khóa tài khoản");
    }

    @PostMapping("/users/{id}/unlock")
    public ApiResponse<Void> unlockUser(@RequestHeader("X-User-Id") UUID actorId, @PathVariable UUID id) {
        adminUserService.unlockUser(actorId, id);
        return ApiResponse.success("USER_UNLOCKED", "Đã mở khóa tài khoản");
    }

    @PostMapping("/users/{id}/roles")
    public ApiResponse<Void> assignRole(@RequestHeader("X-User-Id") UUID actorId, @PathVariable UUID id,
                                         @Valid @RequestBody AssignRoleRequest request) {
        adminUserService.assignRole(actorId, id, request);
        return ApiResponse.success("ROLE_ASSIGNED", "Đã gán role cho người dùng");
    }

    @DeleteMapping("/users/{id}/roles/{roleId}")
    public ResponseEntity<Void> removeRole(@RequestHeader("X-User-Id") UUID actorId,
                                            @PathVariable UUID id, @PathVariable UUID roleId) {
        adminUserService.removeRole(actorId, id, roleId);
        return ResponseEntity.noContent().build();
    }
}
