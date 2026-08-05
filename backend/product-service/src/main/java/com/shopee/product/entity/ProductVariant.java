package com.shopee.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Phien ban co the ban - SKU/gia/kich thuoc/ton kho rieng, khong dung @ManyToOne cho product. */
@Entity
@Table(name = "product_variants")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String sku;

    private String barcode;

    @Column(nullable = false)
    private String name;

    @Column(name = "variant_signature")
    private String variantSignature;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VariantStatus status = VariantStatus.ACTIVE;

    @Column(name = "list_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal listPrice;

    @Column(name = "sale_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "cost_price", precision = 19, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "sale_start_at")
    private Instant saleStartAt;

    @Column(name = "sale_end_at")
    private Instant saleEndAt;

    @Column(precision = 19, scale = 4)
    private BigDecimal weight;

    @Column(name = "weight_unit")
    private String weightUnit;

    @Column(precision = 19, scale = 4)
    private BigDecimal length;

    @Column(precision = 19, scale = 4)
    private BigDecimal width;

    @Column(precision = 19, scale = 4)
    private BigDecimal height;

    @Column(name = "dimension_unit")
    private String dimensionUnit;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
