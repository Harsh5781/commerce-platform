package com.crm.commerce.platform.dashboard.service;

import com.crm.commerce.platform.dashboard.dto.DashboardStats;
import com.crm.commerce.platform.order.model.*;
import com.crm.commerce.platform.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService.clearCache();
    }

    @Test
    void getStats_computesTotalsFromOrders() {
        List<Order> orders = List.of(
                buildOrder("WEBSITE", OrderStatus.PENDING, new BigDecimal("100.00")),
                buildOrder("AMAZON", OrderStatus.DELIVERED, new BigDecimal("250.00")),
                buildOrder("WEBSITE", OrderStatus.SHIPPED, new BigDecimal("150.00")),
                buildOrder("BLINKIT", OrderStatus.CANCELLED, new BigDecimal("75.00"))
        );
        when(orderRepository.findAll()).thenReturn(orders);

        DashboardStats stats = dashboardService.getStats();

        assertThat(stats.getTotalOrders()).isEqualTo(4);
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("575.00"));
        assertThat(stats.getPendingOrders()).isEqualTo(1);
        assertThat(stats.getDeliveredOrders()).isEqualTo(1);
        assertThat(stats.getShippedOrders()).isEqualTo(1);
        assertThat(stats.getCancelledOrders()).isEqualTo(1);
    }

    @Test
    void getStats_includesChannelBreakdown() {
        List<Order> orders = List.of(
                buildOrder("WEBSITE", OrderStatus.PENDING, new BigDecimal("100.00")),
                buildOrder("WEBSITE", OrderStatus.DELIVERED, new BigDecimal("200.00")),
                buildOrder("AMAZON", OrderStatus.PROCESSING, new BigDecimal("300.00"))
        );
        when(orderRepository.findAll()).thenReturn(orders);

        DashboardStats stats = dashboardService.getStats();

        assertThat(stats.getChannelBreakdown()).hasSize(3);
        var websiteBreakdown = stats.getChannelBreakdown().stream()
                .filter(cb -> "WEBSITE".equals(cb.getChannel()))
                .findFirst().orElseThrow();
        assertThat(websiteBreakdown.getOrderCount()).isEqualTo(2);
        assertThat(websiteBreakdown.getRevenue()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(websiteBreakdown.getPendingCount()).isEqualTo(1);
        assertThat(websiteBreakdown.getDeliveredCount()).isEqualTo(1);
    }

    @Test
    void getStats_includesStatusBreakdown() {
        List<Order> orders = List.of(
                buildOrder("WEBSITE", OrderStatus.PENDING, BigDecimal.TEN),
                buildOrder("WEBSITE", OrderStatus.PENDING, BigDecimal.TEN)
        );
        when(orderRepository.findAll()).thenReturn(orders);

        DashboardStats stats = dashboardService.getStats();

        assertThat(stats.getStatusBreakdown()).containsEntry("PENDING", 2L);
        assertThat(stats.getStatusBreakdown()).containsEntry("DELIVERED", 0L);
    }

    @Test
    void getStats_returnsCachedResult_onSecondCall() {
        when(orderRepository.findAll()).thenReturn(List.of());

        dashboardService.getStats();
        dashboardService.getStats();

        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void clearCache_forcesRefreshOnNextCall() {
        when(orderRepository.findAll()).thenReturn(List.of());

        dashboardService.getStats();
        dashboardService.clearCache();
        dashboardService.getStats();

        verify(orderRepository, times(2)).findAll();
    }

    @Test
    void getStats_withEmptyOrders_returnsZeros() {
        when(orderRepository.findAll()).thenReturn(List.of());

        DashboardStats stats = dashboardService.getStats();

        assertThat(stats.getTotalOrders()).isZero();
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getPendingOrders()).isZero();
    }

    @Test
    void getStats_countsOrdersToday() {
        Order todayOrder = buildOrder("WEBSITE", OrderStatus.PENDING, BigDecimal.TEN);
        todayOrder.setPlacedAt(LocalDateTime.now());
        Order oldOrder = buildOrder("AMAZON", OrderStatus.DELIVERED, BigDecimal.TEN);
        oldOrder.setPlacedAt(LocalDateTime.now().minusDays(5));

        when(orderRepository.findAll()).thenReturn(List.of(todayOrder, oldOrder));

        DashboardStats stats = dashboardService.getStats();

        assertThat(stats.getOrdersToday()).isEqualTo(1);
    }

    private Order buildOrder(String channel, OrderStatus status, BigDecimal amount) {
        return Order.builder()
                .channel(channel)
                .status(status)
                .totalAmount(amount)
                .placedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .timeline(new ArrayList<>())
                .build();
    }
}
