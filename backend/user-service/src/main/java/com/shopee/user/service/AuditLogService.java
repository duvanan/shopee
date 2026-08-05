package com.shopee.user.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.common.response.PageData;
import com.shopee.user.dto.AuditLogResponse;
import com.shopee.user.entity.AuditLog;
import com.shopee.user.repository.AuditLogRepository;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Goi truc tiep tu service (khong dung AOP aspect) - khop voi cach du an
 * hien tai lam moi thu (goi thang, khong co lop aspect nao khac trong
 * codebase). Dung AuditLog.builder() de dien cac field nghiep vu, con
 * traceId/createdAt duoc lop nay tu dien.
 */
@Service
public class AuditLogService {

    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(AuditLog.AuditLogBuilder entry) {
        AuditLog auditLog = entry
                .traceId(MDC.get(TRACE_ID_MDC_KEY))
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    public PageData<AuditLogResponse> list(String module, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Page<AuditLog> result = (module == null || module.isBlank())
                ? auditLogRepository.findAll(pageable)
                : auditLogRepository.findByModuleIgnoreCase(module, pageable);
        return PageData.from(result, AuditLogResponse::from);
    }

    public AuditLogResponse get(UUID id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy audit log"));
        return AuditLogResponse.from(log);
    }

    private Pageable toPageable(int page, int size) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
