package com.shopee.product.repository;

import com.shopee.product.entity.VariantAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VariantAttributeValueRepository extends JpaRepository<VariantAttributeValue, UUID> {
    List<VariantAttributeValue> findByVariantId(UUID variantId);
    List<VariantAttributeValue> findByVariantIdIn(Collection<UUID> variantIds);
}
