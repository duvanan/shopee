package com.shopee.product.repository;

import com.shopee.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
    List<ProductCategory> findByProductId(UUID productId);
    List<ProductCategory> findByProductIdIn(Collection<UUID> productIds);
    boolean existsByCategoryId(UUID categoryId);

    /**
     * Bulk delete (khong phai derived entity-remove) - Hibernate mac dinh flush
     * INSERT truoc DELETE trong cung 1 flush du code goi delete truoc insert,
     * nen deleteByProductId() kieu derived se khong tranh duoc loi trung khoa
     * khi saveProductCategories() insert lai ngay sau do (xem ProductService).
     */
    @Modifying
    @Query(value = "DELETE FROM product_categories WHERE product_id = :productId", nativeQuery = true)
    void deleteByProductId(@Param("productId") UUID productId);

    @Query(value = "SELECT c.name FROM categories c JOIN product_categories pc ON pc.category_id = c.id " +
            "WHERE pc.product_id = :productId AND pc.is_primary = TRUE LIMIT 1", nativeQuery = true)
    String findPrimaryCategoryNameByProductId(@Param("productId") UUID productId);
}
