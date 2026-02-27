package com.crm.commerce.platform.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String orderNumber;

    private String channel;

    private String channelOrderRef;

    private Customer customer;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private OrderStatus status;

    private BigDecimal totalAmount;

    private Address shippingAddress;

    private Map<String, Object> channelMetadata;

    @Builder.Default
    private List<OrderTimeline> timeline = new ArrayList<>();

    private LocalDateTime placedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
