package com.shopee.user.repository;

import com.shopee.user.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    /** JPQL tuong minh thay vi derived-method-name de tranh nham lan property "isDefault" vs "default". */
    @Query("SELECT a FROM CustomerAddress a WHERE a.userId = :userId ORDER BY a.isDefault DESC, a.createdAt DESC")
    List<CustomerAddress> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT a FROM CustomerAddress a WHERE a.userId = :userId AND a.isDefault = true")
    List<CustomerAddress> findDefaultByUserId(@Param("userId") UUID userId);
}
