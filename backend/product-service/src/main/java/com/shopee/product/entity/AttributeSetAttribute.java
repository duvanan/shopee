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

import java.time.Instant;
import java.util.UUID;

/**
 * Bang quan trong nhat cua thiet ke - xac dinh 1 Attribute Set co nhung
 * Attribute nao va cau hinh cu the (bat buoc, tao variant, hien thi...)
 * trong set do. Xem docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md muc 11.
 */
@Entity
@Table(name = "attribute_set_attributes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeSetAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "attribute_set_id", nullable = false)
    private UUID attributeSetId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "attribute_id", nullable = false)
    private UUID attributeId;

    @Column(name = "label_override")
    private String labelOverride;

    @Column(name = "help_text", length = 1000)
    private String helpText;

    @Column(name = "placeholder")
    private String placeholder;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private boolean required = false;

    @Column(name = "is_variant_attribute", nullable = false)
    @Builder.Default
    private boolean variantAttribute = false;

    @Column(name = "is_filterable", nullable = false)
    @Builder.Default
    private boolean filterable = false;

    @Column(name = "is_searchable", nullable = false)
    @Builder.Default
    private boolean searchable = false;

    @Column(name = "is_comparable", nullable = false)
    @Builder.Default
    private boolean comparable = false;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private boolean visible = true;

    @Column(name = "is_editable", nullable = false)
    @Builder.Default
    private boolean editable = true;

    @Column(name = "default_value_id")
    private UUID defaultValueId;

    @Column(name = "default_value_text", length = 2000)
    private String defaultValueText;

    @Column(name = "validation_override", columnDefinition = "TEXT")
    private String validationOverride;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
