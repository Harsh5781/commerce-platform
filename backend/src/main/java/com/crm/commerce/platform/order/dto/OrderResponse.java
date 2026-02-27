package com.crm.commerce.platform.order.dto;

import com.crm.commerce.platform.order.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String id;
    private String orderNumber;
    private String channel;
    private String channelOrderRef;
    private Customer customer;
    private List<OrderItem> items;
    private String status;
    private BigDecimal totalAmount;
    private Address shippingAddress;
    private Map<String, Object> channelMetadata;
    private List<OrderTimeline> timeline;
    private LocalDateTime placedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .channel(order.getChannel())
                .channelOrderRef(order.getChannelOrderRef())
                .customer(order.getCustomer())
                .items(order.getItems())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .channelMetadata(order.getChannelMetadata())
                .timeline(order.getTimeline())
                .placedAt(order.getPlacedAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
