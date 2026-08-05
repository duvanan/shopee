package com.shopee.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Cot audit ke thua tu Auditable, Spring Data JPA Auditing tu gan gia tri. */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Nhom nghiep vu (VD "Sản phẩm", "Đơn hàng") - dung de gom nhom hien thi tren man Phan quyen. */
    @Column(nullable = false)
    private String module;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = true;
}
