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
}
