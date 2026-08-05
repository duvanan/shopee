package com.shopee.product.repository;

import com.shopee.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByCode(String code);
    boolean existsBySlug(String slug);
    List<Category> findByParentIdIsNull();
    List<Category> findByParentId(UUID parentId);
}
