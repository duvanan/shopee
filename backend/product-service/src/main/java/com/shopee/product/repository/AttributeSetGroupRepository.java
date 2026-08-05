package com.shopee.product.repository;

import com.shopee.product.entity.AttributeSetGroup;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AttributeSetGroupRepository extends JpaRepository<AttributeSetGroup, UUID> {
    List<AttributeSetGroup> findByAttributeSetId(UUID attributeSetId, Sort sort);

    /** Bulk delete - xem ghi chu ve thu tu flush cua Hibernate trong ProductCategoryRepository. */
    @Modifying
    @Query(value = "DELETE FROM attribute_set_groups WHERE attribute_set_id = :attributeSetId", nativeQuery = true)
    void deleteByAttributeSetId(@Param("attributeSetId") UUID attributeSetId);
}
