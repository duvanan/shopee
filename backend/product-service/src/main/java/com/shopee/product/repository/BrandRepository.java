package com.shopee.product.repository;

import com.shopee.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    boolean existsByCode(String code);
    boolean existsBySlug(String slug);
}
