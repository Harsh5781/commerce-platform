package com.crm.commerce.platform.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String pincode;
    private String country;
}
