package com.shopee.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entity phang, khong co @ManyToMany toi Permission - danh sach permission
 * cua 1 role duoc doc bang native query rieng (xem RoleRepository), khong
 * dua vao object graph cua JPA. Cot audit ke thua tu Auditable.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Role he thong (CUSTOMER/ADMIN) - danh dau de sau nay chan xoa cung o API quan ly role. */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;
}
