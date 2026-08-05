package com.shopee.product.repository;

import com.shopee.product.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {
    boolean existsByCode(String code);
    Optional<ProductAttribute> findByCode(String code);
}
