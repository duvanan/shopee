package com.shopee.user.repository;

import com.shopee.user.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {
}
