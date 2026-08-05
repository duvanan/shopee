package com.shopee.product.repository;

import com.shopee.product.entity.AttributeValue;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AttributeValueRepository extends JpaRepository<AttributeValue, UUID> {
    List<AttributeValue> findByAttributeId(UUID attributeId);
    List<AttributeValue> findByAttributeId(UUID attributeId, Sort sort);
    List<AttributeValue> findByAttributeIdIn(Collection<UUID> attributeIds);
    boolean existsByAttributeIdAndCode(UUID attributeId, String code);
}
