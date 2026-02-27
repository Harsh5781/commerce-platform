package com.crm.commerce.platform.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String productName;
    private String sku;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
