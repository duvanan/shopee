package com.shopee.product.repository;

import com.shopee.product.entity.Product;
import com.shopee.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByCode(String code);

    boolean existsBySlug(String slug);

    boolean existsByProductTypeIdAndDeletedAtIsNull(UUID productTypeId);

    boolean existsByAttributeSetIdAndDeletedAtIsNull(UUID attributeSetId);

    long countByProductTypeIdAndDeletedAtIsNull(UUID productTypeId);

    long countByAttributeSetIdAndDeletedAtIsNull(UUID attributeSetId);

    Page<Product> findByStatusAndDeletedAtIsNull(ProductStatus status, Pageable pageable);

    Page<Product> findByStatusAndDeletedAtIsNullAndNameContainingIgnoreCase(
            ProductStatus status, String name, Pageable pageable);

    Page<Product> findByDeletedAtIsNull(Pageable pageable);

    Page<Product> findByDeletedAtIsNullAndNameContainingIgnoreCase(String name, Pageable pageable);

    @Query(value = "SELECT p.* FROM products p JOIN product_categories pc ON pc.product_id = p.id " +
            "WHERE pc.category_id = :categoryId AND p.status = 'ACTIVE' AND p.deleted_at IS NULL",
            countQuery = "SELECT COUNT(*) FROM products p JOIN product_categories pc ON pc.product_id = p.id " +
                    "WHERE pc.category_id = :categoryId AND p.status = 'ACTIVE' AND p.deleted_at IS NULL",
            nativeQuery = true)
    Page<Product> findPublicByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    @Query(value = "SELECT p.* FROM products p JOIN product_categories pc ON pc.product_id = p.id " +
            "WHERE pc.category_id = :categoryId AND p.status = 'ACTIVE' AND p.deleted_at IS NULL " +
            "AND p.name ILIKE CONCAT('%', :search, '%')",
            countQuery = "SELECT COUNT(*) FROM products p JOIN product_categories pc ON pc.product_id = p.id " +
                    "WHERE pc.category_id = :categoryId AND p.status = 'ACTIVE' AND p.deleted_at IS NULL " +
                    "AND p.name ILIKE CONCAT('%', :search, '%')",
            nativeQuery = true)
    Page<Product> findPublicByCategoryIdAndSearch(@Param("categoryId") UUID categoryId, @Param("search") String search, Pageable pageable);
}
