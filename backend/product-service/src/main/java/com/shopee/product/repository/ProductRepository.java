package com.shopee.product.repository;

import com.shopee.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Product khong con quan he JPA voi Category (chi co categoryId phang),
 * nen khong can @EntityGraph nua - moi query o day la truy van don gian
 * tren 1 bang, khong co object graph nao de load kem.
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByCategoryIdAndNameContainingIgnoreCase(UUID categoryId, String name, Pageable pageable);
    boolean existsByCategoryId(UUID categoryId);
}
