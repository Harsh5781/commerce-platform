package com.crm.commerce.platform.dashboard.controller;

import com.crm.commerce.platform.auth.jwt.JwtTokenProvider;
import com.crm.commerce.platform.auth.security.CustomUserDetails;
import com.crm.commerce.platform.auth.security.CustomUserDetailsService;
import com.crm.commerce.platform.config.AppProperties;
import com.crm.commerce.platform.config.CorsConfig;
import com.crm.commerce.platform.config.SecurityConfig;
import com.crm.commerce.platform.dashboard.dto.ChannelBreakdown;
import com.crm.commerce.platform.dashboard.dto.DashboardStats;
import com.crm.commerce.platform.dashboard.service.DashboardService;
import com.crm.commerce.platform.user.enums.Role;
import com.crm.commerce.platform.user.model.User;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@EnableConfigurationProperties(AppProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-that-is-at-least-32-characters-long-for-hmac",
        "app.jwt.expiration-ms=3600000", "app.jwt.refresh-expiration-ms=86400000",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.rate-limit.requests-per-minute=60", "app.shutdown.max-wait-seconds=30"
})
class DashboardControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DashboardService dashboardService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private MeterRegistry meterRegistry;

    @Test
    void getStats_returnsStatistics() throws Exception {
        DashboardStats stats = DashboardStats.builder()
                .totalOrders(150).totalRevenue(new BigDecimal("45000.00"))
                .pendingOrders(20).processingOrders(15).shippedOrders(30)
                .deliveredOrders(70).cancelledOrders(15)
                .ordersToday(5).ordersThisWeek(25).ordersThisMonth(90)
                .statusBreakdown(Map.of("PENDING", 20L, "DELIVERED", 70L))
                .channelBreakdown(List.of(
                        ChannelBreakdown.builder().channel("WEBSITE").orderCount(60)
                                .revenue(new BigDecimal("18000")).pendingCount(8).deliveredCount(30).build()))
                .build();
        when(dashboardService.getStats()).thenReturn(stats);

        CustomUserDetails viewer = new CustomUserDetails(User.builder()
                .id("u-1").email("v@t.com").name("Viewer")
                .passwordHash("h").role(Role.VIEWER).active(true).build());

        mockMvc.perform(get("/api/dashboard/stats").with(user(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(150))
                .andExpect(jsonPath("$.data.totalRevenue").value(45000.00))
                .andExpect(jsonPath("$.data.channelBreakdown").isArray());
    }

    @Test
    void getStats_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().is4xxClientError());
    }
}
