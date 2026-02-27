package com.crm.commerce.platform.audit.controller;

import com.crm.commerce.platform.audit.model.AuditLog;
import com.crm.commerce.platform.audit.service.AuditService;
import com.crm.commerce.platform.auth.jwt.JwtTokenProvider;
import com.crm.commerce.platform.auth.security.CustomUserDetails;
import com.crm.commerce.platform.auth.security.CustomUserDetailsService;
import com.crm.commerce.platform.config.AppProperties;
import com.crm.commerce.platform.config.CorsConfig;
import com.crm.commerce.platform.config.SecurityConfig;
import com.crm.commerce.platform.user.enums.Role;
import com.crm.commerce.platform.user.model.User;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@EnableConfigurationProperties(AppProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-that-is-at-least-32-characters-long-for-hmac",
        "app.jwt.expiration-ms=3600000", "app.jwt.refresh-expiration-ms=86400000",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.rate-limit.requests-per-minute=60", "app.shutdown.max-wait-seconds=30"
})
class AuditControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuditService auditService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private MeterRegistry meterRegistry;

    private CustomUserDetails adminUser;
    private CustomUserDetails viewerUser;

    @BeforeEach
    void setUp() {
        adminUser = new CustomUserDetails(User.builder()
                .id("u-1").email("admin@test.com").name("Admin")
                .passwordHash("hash").role(Role.ADMIN).active(true).build());
        viewerUser = new CustomUserDetails(User.builder()
                .id("u-3").email("viewer@test.com").name("Viewer")
                .passwordHash("hash").role(Role.VIEWER).active(true).build());
    }

    @Test
    void getAuditLogs_asAdmin_returnsPaginatedLogs() throws Exception {
        AuditLog log = AuditLog.builder()
                .id("log-1").action("CREATE_ORDER").entityType("Order").entityId("ord-1")
                .details(Map.of("orderNumber", "ORD-WEB-00001"))
                .createdAt(LocalDateTime.now()).build();
        when(auditService.getAuditLogs(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/api/audit").with(user(adminUser)).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].action").value("CREATE_ORDER"));
    }

    @Test
    void getAuditLogs_asViewer_returns403() throws Exception {
        mockMvc.perform(get("/api/audit").with(user(viewerUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAuditLogs_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getEntityAuditLogs_asAdmin_returnsLogs() throws Exception {
        AuditLog log = AuditLog.builder()
                .id("log-2").action("UPDATE_ORDER_STATUS").entityType("Order").entityId("ord-1")
                .createdAt(LocalDateTime.now()).build();
        when(auditService.getAuditLogsForEntity(eq("Order"), eq("ord-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/api/audit/entity/Order/ord-1").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].entityId").value("ord-1"));
    }

    @Test
    void getEntityAuditLogs_asViewer_returns403() throws Exception {
        mockMvc.perform(get("/api/audit/entity/Order/ord-1").with(user(viewerUser)))
                .andExpect(status().isForbidden());
    }
}
