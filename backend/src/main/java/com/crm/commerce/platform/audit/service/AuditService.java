package com.crm.commerce.platform.audit.service;

import com.crm.commerce.platform.audit.model.AuditLog;
import com.crm.commerce.platform.audit.repository.AuditLogRepository;
import com.crm.commerce.platform.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AppProperties appProperties;

    @Async
    public void logAction(String userId, String userName, String action,
                          String entityType, String entityId, Map<String, Object> details) {
        if (!appProperties.getFeatureFlags().isEnableAuditLog()) {
            return;
        }

        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userName(userName)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} {} on {} {}", userName, action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<AuditLog> getAuditLogsForEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }
}
