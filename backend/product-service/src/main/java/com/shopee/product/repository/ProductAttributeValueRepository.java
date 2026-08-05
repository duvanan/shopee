package com.shopee.product.repository;

import com.shopee.product.entity.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, UUID> {
    List<ProductAttributeValue> findByProductId(UUID productId);

    /** Bulk delete - xem ghi chu ve thu tu flush cua Hibernate trong ProductCategoryRepository. */
    @Modifying
    @Query(value = "DELETE FROM product_attribute_values WHERE product_id = :productId", nativeQuery = true)
    void deleteByProductId(@Param("productId") UUID productId);
}
