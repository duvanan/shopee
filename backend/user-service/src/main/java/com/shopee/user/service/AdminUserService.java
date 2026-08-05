package com.shopee.user.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.common.response.PageData;
import com.shopee.user.dto.AdminCreateUserRequest;
import com.shopee.user.dto.AdminUpdateUserRequest;
import com.shopee.user.dto.AdminUserResponse;
import com.shopee.user.dto.AssignRoleRequest;
import com.shopee.user.entity.AuditAction;
import com.shopee.user.entity.AuditActorType;
import com.shopee.user.entity.AuditLog;
import com.shopee.user.entity.AuditStatus;
import com.shopee.user.entity.Role;
import com.shopee.user.entity.User;
import com.shopee.user.entity.UserStatus;
import com.shopee.user.repository.RoleRepository;
import com.shopee.user.repository.UserRepository;
import com.shopee.user.security.RefreshTokenService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Quan tri User/Role gan cho user - permission USER_MANAGE (xem api-gateway.yml). */
@Service
public class AdminUserService {

    private static final String DEFAULT_ROLE_CODE = "CUSTOMER";
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, RoleRepository roleRepository,
                             AuditLogService auditLogService, RefreshTokenService refreshTokenService,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public PageData<AdminUserResponse> listUsers(String search, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Page<User> result = (search == null || search.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(search, search, pageable);

        return PageData.from(result, user ->
                AdminUserResponse.from(user, roleRepository.findRoleCodesByUserId(user.getId())));
    }

    public AdminUserResponse getUser(UUID id) {
        User user = findUserOrThrow(id);
        return AdminUserResponse.from(user, roleRepository.findRoleCodesByUserId(id));
    }

    /** Admin tao user truc tiep (dat san mat khau, khong qua luong tu dang ky). */
    @Transactional
    public AdminUserResponse createUser(UUID actorId, AdminCreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        String roleCode = (request.roleCode() == null || request.roleCode().isBlank())
                ? DEFAULT_ROLE_CODE : request.roleCode();
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy role"));

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .build();
        User saved = userRepository.save(user);
        roleRepository.assignRoleToUser(saved.getId(), role.getId(), actorId);

        auditLogService.record(AuditLog.builder()
                .actorId(actorId).actorType(AuditActorType.ADMIN).action(AuditAction.CREATE_USER)
                .module("USER").objectType("User").objectId(saved.getId().toString())
                .description("Tạo tài khoản " + saved.getEmail() + " với role " + role.getCode())
                .status(AuditStatus.SUCCESS));

        return AdminUserResponse.from(saved, List.of(role.getCode()));
    }

    @Transactional
    public AdminUserResponse updateUser(UUID actorId, UUID id, AdminUpdateUserRequest request) {
        User user = findUserOrThrow(id);
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        User saved = userRepository.save(user);

        auditLogService.record(AuditLog.builder()
                .actorId(actorId).actorType(AuditActorType.ADMIN).action(AuditAction.UPDATE_USER)
                .module("USER").objectType("User").objectId(id.toString())
                .description("Cập nhật hồ sơ " + user.getEmail())
                .status(AuditStatus.SUCCESS));

        return AdminUserResponse.from(saved, roleRepository.findRoleCodesByUserId(id));
    }

    @Transactional
    public void lockUser(UUID actorId, UUID id) {
        User user = findUserOrThrow(id);
        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);
        refreshTokenService.revokeAllByUserId(id);

        auditLogService.record(AuditLog.builder()
                .actorId(actorId).actorType(AuditActorType.ADMIN).action(AuditAction.LOCK_USER)
                .module("USER").objectType("User").objectId(id.toString())
                .description("Khóa tài khoản " + user.getEmail())
                .status(AuditStatus.SUCCESS));
    }

    @Transactional
    public void unlockUser(UUID actorId, UUID id) {
        User user = findUserOrThrow(id);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        auditLogService.record(AuditLog.builder()
                .actorId(actorId).actorType(AuditActorType.ADMIN).action(AuditAction.UNLOCK_USER)
                .module("USER").objectType("User").objectId(id.toString())
                .description("Mở khóa tài khoản " + user.getEmail())
                .status(AuditStatus.SUCCESS));
    }

    @Transactional
    public void assignRole(UUID actorId, UUID id, AssignRoleRequest request) {
        User user = findUserOrThrow(id);
        Role role = roleRepository.findByCode(request.roleCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy role"));

        roleRepository.assignRoleToUser(id, role.getId(), actorId);

        auditLogService.record(AuditLog.builder()
                .actorId(actorId).actorType(AuditActorType.ADMIN).action(AuditAction.ASSIGN_ROLE)
                .module("USER").objectType("User").objectId(id.toString())
                .description("Gán role " + role.getCode() + " cho " + user.getEmail())
                .newValue(role.getCode())
                .status(AuditStatus.SUCCESS));
    }

    @Transactional
    public void removeRole(UUID actorId, UUID id, UUID roleId) {
        User user = findUserOrThrow(id);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy role"));

        if ("ADMIN".equals(role.getCode()) && roleRepository.countUsersWithRoleCode("ADMIN") <= 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Không thể gỡ quyền ADMIN của người dùng cuối cùng còn quyền quản trị");
        }

        roleRepository.removeRoleFromUser(id, roleId);

        auditLogService.record(AuditLog.builder()
                .actorId(actorId).actorType(AuditActorType.ADMIN).action(AuditAction.REMOVE_ROLE)
                .module("USER").objectType("User").objectId(id.toString())
                .description("Gỡ role " + role.getCode() + " khỏi " + user.getEmail())
                .oldValue(role.getCode())
                .status(AuditStatus.SUCCESS));
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Pageable toPageable(int page, int size) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
