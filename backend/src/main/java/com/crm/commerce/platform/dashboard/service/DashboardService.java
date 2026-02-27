package com.crm.commerce.platform.dashboard.service;

import com.crm.commerce.platform.dashboard.dto.ChannelBreakdown;
import com.crm.commerce.platform.dashboard.dto.DashboardStats;
import com.crm.commerce.platform.order.model.Order;
import com.crm.commerce.platform.order.model.OrderStatus;
import com.crm.commerce.platform.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final long CACHE_TTL_MS = 60_000;

    private final OrderRepository orderRepository;

    private volatile DashboardStats cachedStats;
    private volatile long cachedAt;

    public DashboardStats getStats() {
        if (cachedStats != null && (System.currentTimeMillis() - cachedAt) < CACHE_TTL_MS) {
            log.debug("Returning cached dashboard stats (age: {}ms)", System.currentTimeMillis() - cachedAt);
            return cachedStats;
        }
        return refreshStats();
    }

    public void clearCache() {
        cachedStats = null;
        cachedAt = 0;
        log.debug("Dashboard stats cache cleared");
    }

    private synchronized DashboardStats refreshStats() {
        if (cachedStats != null && (System.currentTimeMillis() - cachedAt) < CACHE_TTL_MS) {
            return cachedStats;
        }

        log.debug("Computing dashboard stats from MongoDB");
        List<Order> allOrders = orderRepository.findAll();

        long total = allOrders.size();
        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            long count = allOrders.stream().filter(o -> o.getStatus() == s).count();
            statusBreakdown.put(s.name(), count);
        }

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now()
                .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                .atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long ordersToday = allOrders.stream()
                .filter(o -> o.getPlacedAt() != null && o.getPlacedAt().isAfter(todayStart))
                .count();
        long ordersThisWeek = allOrders.stream()
                .filter(o -> o.getPlacedAt() != null && o.getPlacedAt().isAfter(weekStart))
                .count();
        long ordersThisMonth = allOrders.stream()
                .filter(o -> o.getPlacedAt() != null && o.getPlacedAt().isAfter(monthStart))
                .count();

        List<ChannelBreakdown> channelBreakdowns = buildChannelBreakdowns(allOrders);

        DashboardStats stats = DashboardStats.builder()
                .totalOrders(total)
                .totalRevenue(totalRevenue)
                .pendingOrders(statusBreakdown.getOrDefault("PENDING", 0L))
                .processingOrders(statusBreakdown.getOrDefault("PROCESSING", 0L))
                .shippedOrders(statusBreakdown.getOrDefault("SHIPPED", 0L))
                .deliveredOrders(statusBreakdown.getOrDefault("DELIVERED", 0L))
                .cancelledOrders(statusBreakdown.getOrDefault("CANCELLED", 0L))
                .channelBreakdown(channelBreakdowns)
                .statusBreakdown(statusBreakdown)
                .ordersToday(ordersToday)
                .ordersThisWeek(ordersThisWeek)
                .ordersThisMonth(ordersThisMonth)
                .build();

        this.cachedStats = stats;
        this.cachedAt = System.currentTimeMillis();
        return stats;
    }

    private List<ChannelBreakdown> buildChannelBreakdowns(List<Order> allOrders) {
        String[] channels = {"WEBSITE", "AMAZON", "BLINKIT"};
        List<ChannelBreakdown> breakdowns = new ArrayList<>();

        for (String channel : channels) {
            List<Order> channelOrders = allOrders.stream()
                    .filter(o -> channel.equals(o.getChannel()))
                    .toList();

            BigDecimal revenue = channelOrders.stream()
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long pending = channelOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
            long delivered = channelOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();

            breakdowns.add(ChannelBreakdown.builder()
                    .channel(channel)
                    .orderCount(channelOrders.size())
                    .revenue(revenue)
                    .pendingCount(pending)
                    .deliveredCount(delivered)
                    .build());
        }

        return breakdowns;
    }
}
