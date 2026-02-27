package com.crm.commerce.platform.channel.connector;

import com.crm.commerce.platform.order.model.Order;

import java.util.List;

public interface ChannelConnector {

    String getChannelCode();

    List<Order> fetchNewOrders();

    boolean isAvailable();
}
