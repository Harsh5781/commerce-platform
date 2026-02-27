package com.crm.commerce.platform.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
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
@CompoundIndexes({
    @CompoundIndex(name = "channel_status_date", def = "{'channel': 1, 'status': 1, 'placedAt': -1}")
})
public class Order {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderNumber;

    @Indexed
    private String channel;

    private String channelOrderRef;

    private Customer customer;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Indexed
    private OrderStatus status;

    private BigDecimal totalAmount;

    private Address shippingAddress;

    private Map<String, Object> channelMetadata;

    @Builder.Default
    private List<OrderTimeline> timeline = new ArrayList<>();

    @Indexed(direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING)
    private LocalDateTime placedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
