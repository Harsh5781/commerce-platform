package com.crm.commerce.platform.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelBreakdown {
    private String channel;
    private long orderCount;
    private BigDecimal revenue;
    private long pendingCount;
    private long deliveredCount;
}
