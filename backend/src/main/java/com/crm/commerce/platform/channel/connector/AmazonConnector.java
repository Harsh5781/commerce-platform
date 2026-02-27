package com.crm.commerce.platform.channel.connector;

import com.crm.commerce.platform.common.exception.ExternalServiceException;
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
public class AmazonConnector implements ChannelConnector {

    private static final String CHANNEL = "AMAZON";

    @Override
    public String getChannelCode() {
        return CHANNEL;
    }

    @Override
    @CircuitBreaker(name = "amazonChannel", fallbackMethod = "fetchOrdersFallback")
    @Retry(name = "amazonChannel")
    public List<Order> fetchNewOrders() {
        log.info("Fetching new orders from Amazon Seller API...");
        simulateApiLatency();

        if (ThreadLocalRandom.current().nextInt(10) < 2) {
            throw new ExternalServiceException(CHANNEL, "Amazon SP-API returned 503");
        }

        return generateMockOrders();
    }

    @Override
    @CircuitBreaker(name = "amazonChannel", fallbackMethod = "isAvailableFallback")
    public boolean isAvailable() {
        simulatePingLatency();
        return true;
    }

    @SuppressWarnings("unused")
    private List<Order> fetchOrdersFallback(Throwable t) {
        log.warn("Amazon API unavailable, circuit breaker active: {}", t.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private boolean isAvailableFallback(Throwable t) {
        log.warn("Amazon availability check failed: {}", t.getMessage());
        return false;
    }

    private void simulateApiLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulatePingLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Order> generateMockOrders() {
        int count = ThreadLocalRandom.current().nextInt(0, 4);
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            orders.add(Order.builder()
                    .channel(CHANNEL)
                    .channelOrderRef("402-" + ThreadLocalRandom.current().nextInt(1000000, 9999999) + "-" + ThreadLocalRandom.current().nextInt(1000000, 9999999))
                    .customer(Customer.builder()
                            .name("Amazon Customer " + i)
                            .email("customer" + i + "@amazon.in")
                            .phone("+91-" + (9000000000L + ThreadLocalRandom.current().nextInt(999999999)))
                            .build())
                    .items(List.of(OrderItem.builder()
                            .productName("Organic Honey 500g")
                            .sku("HON-001")
                            .quantity(1)
                            .unitPrice(BigDecimal.valueOf(450))
                            .totalPrice(BigDecimal.valueOf(450))
                            .build()))
                    .status(OrderStatus.PENDING)
                    .totalAmount(BigDecimal.valueOf(450))
                    .channelMetadata(Map.of(
                            "fulfillmentCenter", "BLR-3",
                            "primeOrder", ThreadLocalRandom.current().nextBoolean(),
                            "sellerId", "A1B2C3D4E5"))
                    .placedAt(LocalDateTime.now())
                    .build());
        }
        return orders;
    }
}
