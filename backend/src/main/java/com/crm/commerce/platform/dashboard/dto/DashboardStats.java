package com.crm.commerce.platform.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private long totalOrders;
    private BigDecimal totalRevenue;
    private long pendingOrders;
    private long processingOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private List<ChannelBreakdown> channelBreakdown;
    private Map<String, Long> statusBreakdown;
    private long ordersToday;
    private long ordersThisWeek;
    private long ordersThisMonth;
}
