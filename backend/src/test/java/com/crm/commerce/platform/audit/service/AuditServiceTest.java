package com.crm.commerce.platform.audit.service;

import com.crm.commerce.platform.audit.model.AuditLog;
import com.crm.commerce.platform.audit.repository.AuditLogRepository;
import com.crm.commerce.platform.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AppProperties appProperties;

    @InjectMocks
    private AuditService auditService;

    private AppProperties.FeatureFlags featureFlags;

    @BeforeEach
    void setUp() {
        featureFlags = new AppProperties.FeatureFlags();
    }

    @Test
    void logAction_whenAuditEnabled_savesLog() {
        featureFlags.setEnableAuditLog(true);
        when(appProperties.getFeatureFlags()).thenReturn(featureFlags);

        Map<String, Object> details = Map.of("orderNumber", "ORD-001");
        auditService.logAction("user-1", "Admin User", "CREATE_ORDER",
                "Order", "order-1", details);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getUserName()).isEqualTo("Admin User");
        assertThat(saved.getAction()).isEqualTo("CREATE_ORDER");
        assertThat(saved.getEntityType()).isEqualTo("Order");
        assertThat(saved.getEntityId()).isEqualTo("order-1");
        assertThat(saved.getDetails()).containsEntry("orderNumber", "ORD-001");
    }

    @Test
    void logAction_whenAuditDisabled_skipsSave() {
        featureFlags.setEnableAuditLog(false);
        when(appProperties.getFeatureFlags()).thenReturn(featureFlags);

        auditService.logAction("user-1", "Admin", "CREATE_ORDER", "Order", "ord-1", Map.of());

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void logAction_whenSaveFails_doesNotThrow() {
        featureFlags.setEnableAuditLog(true);
        when(appProperties.getFeatureFlags()).thenReturn(featureFlags);
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        auditService.logAction("user-1", "Admin", "CREATE_ORDER", "Order", "ord-1", Map.of());

        verify(auditLogRepository).save(any());
    }

    @Test
    void getAuditLogs_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        AuditLog log = AuditLog.builder().id("log-1").action("CREATE_ORDER").build();
        Page<AuditLog> page = new PageImpl<>(List.of(log), pageable, 1);
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        Page<AuditLog> result = auditService.getAuditLogs(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("CREATE_ORDER");
    }

    @Test
    void getAuditLogsForEntity_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        AuditLog log = AuditLog.builder().id("log-1").entityType("Order").entityId("ord-1").build();
        Page<AuditLog> page = new PageImpl<>(List.of(log), pageable, 1);
        when(auditLogRepository.findByEntityTypeAndEntityId("Order", "ord-1", pageable)).thenReturn(page);

        Page<AuditLog> result = auditService.getAuditLogsForEntity("Order", "ord-1", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEntityId()).isEqualTo("ord-1");
    }
}
