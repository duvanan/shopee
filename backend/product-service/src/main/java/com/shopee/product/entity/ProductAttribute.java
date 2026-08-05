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

@Entity
@Table(name = "product_attributes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private AttributeDataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false)
    private AttributeInputType inputType;

    private String unit;

    @Column(name = "attribute_group")
    private String attributeGroup;

    @Column(name = "is_filterable", nullable = false)
    @Builder.Default
    private boolean filterable = false;

    @Column(name = "is_searchable", nullable = false)
    @Builder.Default
    private boolean searchable = false;

    @Column(name = "is_comparable", nullable = false)
    @Builder.Default
    private boolean comparable = false;

    @Column(name = "allow_variant", nullable = false)
    @Builder.Default
    private boolean allowVariant = false;

    @Column(name = "allow_predefined_values", nullable = false)
    @Builder.Default
    private boolean allowPredefinedValues = false;

    @Column(name = "allow_custom_value", nullable = false)
    @Builder.Default
    private boolean allowCustomValue = true;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean system = false;

    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommonStatus status = CommonStatus.ACTIVE;

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
