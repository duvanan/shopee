package com.shopee.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Khong dung @ManyToOne/@JoinColumn cho brand/category - chi luu id tho, tu
 * join bang native query + batch-fetch trong Service (xem
 * docs/features/03-dynamic-product-catalog.md). Khong luu SKU/gia/ton kho o
 * day - nam o ProductVariant.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    @Builder.Default
    private ProductStructureType structureType = ProductStructureType.SIMPLE;

    /** FK toi product_types (loai san pham nhu SMARTPHONE/T_SHIRT) - quyet dinh attributeSetId mac dinh khi tao san pham. */
    @Column(name = "product_type_id")
    private UUID productTypeId;

    /** Attribute Set dang ap dung cho san pham nay - gan 1 lan luc tao, khong tu doi khi Attribute Set duoc sua sau nay. */
    @Column(name = "attribute_set_id")
    private UUID attributeSetId;

    /** version_no cua Attribute Set tai thoi diem tao san pham - xem docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md muc 12. */
    @Column(name = "attribute_set_version")
    private Integer attributeSetVersion;

    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "min_price", precision = 19, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 19, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "seo_title")
    private String seoTitle;

    @Column(name = "seo_description")
    private String seoDescription;

    @Column(name = "seo_keywords")
    private String seoKeywords;

    @Column(name = "canonical_url")
    private String canonicalUrl;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "new_until")
    private Instant newUntil;

    @Column(name = "sales_count", nullable = false)
    @Builder.Default
    private Long salesCount = 0L;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
