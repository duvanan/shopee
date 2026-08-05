package com.shopee.user.repository;

import com.shopee.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cac quan he user_roles / role_permissions duoc doc/ghi bang native SQL
 * truc tiep, khong dung @ManyToMany - tranh phu thuoc object graph cua
 * JPA khi he thong lon dan (xem docs/architecture/rbac.md).
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    @Query(value = "SELECT COUNT(*) FROM user_roles WHERE role_id = :roleId", nativeQuery = true)
    long countUsersWithRoleId(@Param("roleId") UUID roleId);

    @Query(value = "SELECT p.code FROM permissions p JOIN role_permissions rp ON rp.permission_id = p.id " +
            "WHERE rp.role_id = :roleId", nativeQuery = true)
    List<String> findPermissionCodesByRoleId(@Param("roleId") UUID roleId);

    @Modifying
    @Query(value = "INSERT INTO role_permissions (role_id, permission_id, assigned_by) VALUES (:roleId, :permissionId, :assignedBy) " +
            "ON CONFLICT (role_id, permission_id) DO NOTHING", nativeQuery = true)
    void assignPermissionToRole(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId,
                                 @Param("assignedBy") UUID assignedBy);

    @Modifying
    @Query(value = "DELETE FROM role_permissions WHERE role_id = :roleId AND permission_id = :permissionId", nativeQuery = true)
    void removePermissionFromRole(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);

    @Query(value = "SELECT r.code FROM roles r JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_id = :userId",
            nativeQuery = true)
    List<String> findRoleCodesByUserId(@Param("userId") UUID userId);

    @Query(value = "SELECT DISTINCT p.code FROM permissions p " +
            "JOIN role_permissions rp ON rp.permission_id = p.id " +
            "JOIN user_roles ur ON ur.role_id = rp.role_id " +
            "WHERE ur.user_id = :userId",
            nativeQuery = true)
    List<String> findPermissionCodesByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)", nativeQuery = true)
    void assignRoleToUser(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /** Admin gan role cho user khac - idempotent (khong loi neu da co san), co ghi nguoi gan. */
    @Modifying
    @Query(value = "INSERT INTO user_roles (user_id, role_id, assigned_by) VALUES (:userId, :roleId, :assignedBy) " +
            "ON CONFLICT (user_id, role_id) DO NOTHING", nativeQuery = true)
    void assignRoleToUser(@Param("userId") UUID userId, @Param("roleId") UUID roleId, @Param("assignedBy") UUID assignedBy);

    @Modifying
    @Query(value = "DELETE FROM user_roles WHERE user_id = :userId AND role_id = :roleId", nativeQuery = true)
    void removeRoleFromUser(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /** Dung de chan go role ADMIN cua nguoi dung ADMIN cuoi cung con lai. */
    @Query(value = "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE r.code = :roleCode",
            nativeQuery = true)
    long countUsersWithRoleCode(@Param("roleCode") String roleCode);
}
