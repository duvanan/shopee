package com.shopee.product.repository;

import com.shopee.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    boolean existsBySku(String sku);
    List<ProductVariant> findByProductIdAndDeletedAtIsNullOrderBySortOrder(UUID productId);
    Optional<ProductVariant> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT MIN(v.salePrice) FROM ProductVariant v WHERE v.productId = :productId AND v.deletedAt IS NULL AND v.status = com.shopee.product.entity.VariantStatus.ACTIVE")
    Optional<BigDecimal> findMinActiveSalePrice(@Param("productId") UUID productId);

    @Query("SELECT MAX(v.salePrice) FROM ProductVariant v WHERE v.productId = :productId AND v.deletedAt IS NULL AND v.status = com.shopee.product.entity.VariantStatus.ACTIVE")
    Optional<BigDecimal> findMaxActiveSalePrice(@Param("productId") UUID productId);
}
