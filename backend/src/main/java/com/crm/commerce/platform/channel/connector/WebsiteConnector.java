package com.crm.commerce.platform.channel.connector;

import com.crm.commerce.platform.order.model.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class WebsiteConnector implements ChannelConnector {

    private static final String CHANNEL = "WEBSITE";

    @Override
    public String getChannelCode() {
        return CHANNEL;
    }

    @Override
    @CircuitBreaker(name = "websiteChannel", fallbackMethod = "fetchOrdersFallback")
    @Retry(name = "websiteChannel")
    public List<Order> fetchNewOrders() {
        log.info("Fetching new orders from organic website...");
        simulateApiLatency();
        return generateMockOrders();
    }

    @Override
    @CircuitBreaker(name = "websiteChannel", fallbackMethod = "isAvailableFallback")
    public boolean isAvailable() {
        simulateApiLatency();
        return true;
    }

    @SuppressWarnings("unused")
    private List<Order> fetchOrdersFallback(Throwable t) {
        log.warn("Website API unavailable: {}", t.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private boolean isAvailableFallback(Throwable t) {
        return false;
    }

    private void simulateApiLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Order> generateMockOrders() {
        int count = ThreadLocalRandom.current().nextInt(0, 3);
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            orders.add(Order.builder()
                    .channel(CHANNEL)
                    .channelOrderRef("WEB-" + ThreadLocalRandom.current().nextInt(300000, 999999))
                    .customer(Customer.builder()
                            .name("Web Customer " + i)
                            .email("customer" + i + "@organicstore.com")
                            .phone("+91-" + (9000000000L + ThreadLocalRandom.current().nextInt(999999999)))
                            .build())
                    .items(List.of(OrderItem.builder()
                            .productName("Organic Green Tea 100g")
                            .sku("TEA-007")
                            .quantity(3)
                            .unitPrice(BigDecimal.valueOf(320))
                            .totalPrice(BigDecimal.valueOf(960))
                            .build()))
                    .status(OrderStatus.PENDING)
                    .totalAmount(BigDecimal.valueOf(960))
                    .channelMetadata(Map.of(
                            "source", "organic",
                            "utmCampaign", "summer_sale",
                            "couponApplied", false))
                    .placedAt(LocalDateTime.now())
                    .build());
        }
        return orders;
    }
}
