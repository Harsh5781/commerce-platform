package com.crm.commerce.platform.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private String channel;
    private String channelOrderRef;
    private CustomerDto customer;
    private List<OrderItemDto> items;
    private AddressDto shippingAddress;
    private Map<String, Object> channelMetadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDto {
        private String name;
        private String email;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private String productName;
        private String sku;
        private int quantity;
        private BigDecimal unitPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String pincode;
        private String country;
    }
}
