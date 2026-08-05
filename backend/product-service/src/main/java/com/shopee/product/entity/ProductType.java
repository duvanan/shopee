package com.shopee.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Xac dinh ban chat ky thuat cua san pham (SMARTPHONE, T_SHIRT...), quyet
 * dinh Attribute Set mac dinh. Khac Category (chi phuc vu dieu huong/SEO) -
 * xem docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md
 * muc 3 va 42.
 */
@Entity
@Table(name = "product_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductType extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "default_attribute_set_id")
    private UUID defaultAttributeSetId;

    @Column(name = "icon_url")
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommonStatus status = CommonStatus.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
