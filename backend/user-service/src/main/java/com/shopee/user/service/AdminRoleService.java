package com.shopee.user.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.user.dto.PermissionResponse;
import com.shopee.user.dto.RoleCreateRequest;
import com.shopee.user.dto.RoleDetailResponse;
import com.shopee.user.dto.RoleResponse;
import com.shopee.user.dto.RoleUpdateRequest;
import com.shopee.user.dto.SetRolePermissionsRequest;
import com.shopee.user.entity.AuditAction;
import com.shopee.user.entity.AuditActorType;
import com.shopee.user.entity.AuditLog;
import com.shopee.user.entity.AuditStatus;
import com.shopee.user.entity.Permission;
import com.shopee.user.entity.Role;
import com.shopee.user.repository.PermissionRepository;
import com.shopee.user.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Quan ly Role/Permission - permission ROLE_MANAGE (xem api-gateway.yml). */
@Service
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogService auditLogService;

    public AdminRoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                             AuditLogService auditLogService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogService = auditLogService;
    }

    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream().map(RoleResponse::from).toList();
    }

    public RoleDetailResponse getRole(UUID id) {
        Role role = findRoleOrThrow(id);
        return RoleDetailResponse.from(role, roleRepository.findPermissionCodesByRoleId(id));
    }

    public RoleResponse createRole(UUID actorId, RoleCreateRequest request) {
        if (roleRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Mã role đã tồn tại");
        }
        Role role = Role.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .isSystem(false)
                .build();
        Role saved = roleRepository.save(role);

        auditLogService.record(AuditLog.builder()
                .actorId(actorId).actorType(AuditActorType.ADMIN).action(AuditAction.CREATE_ROLE)
                .module("ROLE").objectType("Role").objectId(saved.getId().toString())
                .description("Tạo role " + saved.getCode())
                .status(AuditStatus.SUCCESS));

        return RoleResponse.from(saved);
    }

    public RoleResponse updateRole(UUID actorId, UUID id, RoleUpdateRequest request) {
        Role role = findRoleOrThrow(id);
        role.setName(request.name());
        role.setDescription(request.description());
        Role saved = roleRepository.save(role);

        auditLogService.record(AuditLog.builder()
                .actorId(actorId).actorType(AuditActorType.ADMIN).action(AuditAction.UPDATE_ROLE)
                .module("ROLE").objectType("Role").objectId(id.toString())
                .description("Cập nhật role " + role.getCode())
                .status(AuditStatus.SUCCESS));

        return RoleResponse.from(saved);
    }

    public void deleteRole(UUID actorId, UUID id) {
        Role role = findRoleOrThrow(id);
        if (role.isSystem()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Không thể xóa role hệ thống");
        }
        if (roleRepository.countUsersWithRoleId(id) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Role đang được gán cho người dùng, không thể xóa");
        }

        roleRepository.deleteById(id);

        auditLogService.record(AuditLog.builder()
                .actorId(actorId).actorType(AuditActorType.ADMIN).action(AuditAction.DELETE_ROLE)
                .module("ROLE").objectType("Role").objectId(id.toString())
                .description("Xóa role " + role.getCode())
                .status(AuditStatus.SUCCESS));
    }

    /**
     * Thay toan bo tap permission cua role trong 1 request (frontend tich
     * chon xong moi bam Luu, khong goi API tung o tick mot) - tu tinh diff
     * (them nhung cai moi tick, bo nhung cai bi bo tick) trong 1 transaction.
     */
    @Transactional
    public RoleDetailResponse setPermissions(UUID actorId, UUID id, SetRolePermissionsRequest request) {
        Role role = findRoleOrThrow(id);
        List<String> currentCodes = roleRepository.findPermissionCodesByRoleId(id);
        Set<String> currentSet = new HashSet<>(currentCodes);
        Set<String> newSet = new HashSet<>(request.permissionCodes());

        Set<String> toAdd = new HashSet<>(newSet);
        toAdd.removeAll(currentSet);
        Set<String> toRemove = new HashSet<>(currentSet);
        toRemove.removeAll(newSet);

        if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
            var permissionsByCode = permissionRepository.findAll().stream()
                    .collect(Collectors.toMap(Permission::getCode, p -> p));

            for (String code : toAdd) {
                Permission permission = permissionsByCode.get(code);
                if (permission == null) {
                    throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy permission " + code);
                }
                roleRepository.assignPermissionToRole(id, permission.getId(), actorId);
            }
            for (String code : toRemove) {
                Permission permission = permissionsByCode.get(code);
                if (permission != null) {
                    roleRepository.removePermissionFromRole(id, permission.getId());
                }
            }

            auditLogService.record(AuditLog.builder()
                    .actorId(actorId).actorType(AuditActorType.ADMIN).action(AuditAction.UPDATE_ROLE_PERMISSIONS)
                    .module("ROLE").objectType("Role").objectId(id.toString())
                    .description("Cập nhật permission của role " + role.getCode()
                            + (toAdd.isEmpty() ? "" : " (+" + String.join(",", toAdd) + ")")
                            + (toRemove.isEmpty() ? "" : " (-" + String.join(",", toRemove) + ")"))
                    .oldValue(String.join(",", currentCodes))
                    .newValue(String.join(",", newSet))
                    .status(AuditStatus.SUCCESS));
        }

        return RoleDetailResponse.from(role, roleRepository.findPermissionCodesByRoleId(id));
    }

    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream().map(PermissionResponse::from).toList();
    }

    private Role findRoleOrThrow(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy role"));
    }
}
