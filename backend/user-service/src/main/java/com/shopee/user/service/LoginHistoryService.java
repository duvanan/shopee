package com.shopee.user.service;

import com.shopee.user.entity.LoginAction;
import com.shopee.user.entity.LoginHistory;
import com.shopee.user.entity.LoginStatus;
import com.shopee.user.repository.LoginHistoryRepository;
import com.shopee.user.security.DeviceInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    public LoginHistoryService(LoginHistoryRepository loginHistoryRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Transactional
    public void record(UUID userId, String email, LoginAction action, LoginStatus status,
                        DeviceInfo deviceInfo, String sessionId, String failureReason) {
        LoginHistory history = LoginHistory.builder()
                .userId(userId)
                .email(email)
                .action(action)
                .status(status)
                .sessionId(sessionId)
                .ipAddress(deviceInfo != null ? deviceInfo.ipAddress() : null)
                .userAgent(deviceInfo != null ? deviceInfo.userAgent() : null)
                .failureReason(failureReason)
                .createdAt(Instant.now())
                .build();
        loginHistoryRepository.save(history);
    }
}
