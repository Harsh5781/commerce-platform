package com.crm.commerce.platform.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeline {
    private OrderStatus status;
    private String changedBy;
    private String notes;
    private LocalDateTime timestamp;
}
