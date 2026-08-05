package com.shopee.product.repository;

import com.shopee.product.entity.AttributeSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttributeSetRepository extends JpaRepository<AttributeSet, UUID> {
    boolean existsByCode(String code);
    Optional<AttributeSet> findByIdAndDeletedAtIsNull(UUID id);
}
