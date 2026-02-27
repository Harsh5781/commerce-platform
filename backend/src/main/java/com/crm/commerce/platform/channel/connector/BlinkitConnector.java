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
public class BlinkitConnector implements ChannelConnector {

    private static final String CHANNEL = "BLINKIT";

    @Override
    public String getChannelCode() {
        return CHANNEL;
    }

    @Override
    @CircuitBreaker(name = "blinkitChannel", fallbackMethod = "fetchOrdersFallback")
    @Retry(name = "blinkitChannel")
    public List<Order> fetchNewOrders() {
        log.info("Fetching new orders from Blinkit API...");
        simulateApiLatency();

        if (ThreadLocalRandom.current().nextInt(10) < 1) {
            throw new ExternalServiceException(CHANNEL, "Blinkit API returned 500");
        }

        return generateMockOrders();
    }

    @Override
    @CircuitBreaker(name = "blinkitChannel", fallbackMethod = "isAvailableFallback")
    public boolean isAvailable() {
        simulatePingLatency();
        return true;
    }

    @SuppressWarnings("unused")
    private List<Order> fetchOrdersFallback(Throwable t) {
        log.warn("Blinkit API unavailable, circuit breaker active: {}", t.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private boolean isAvailableFallback(Throwable t) {
        log.warn("Blinkit availability check failed: {}", t.getMessage());
        return false;
    }

    private void simulateApiLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(50, 300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulatePingLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(5, 40));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Order> generateMockOrders() {
        int count = ThreadLocalRandom.current().nextInt(0, 5);
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            orders.add(Order.builder()
                    .channel(CHANNEL)
                    .channelOrderRef("BLK-" + ThreadLocalRandom.current().nextInt(200000, 999999))
                    .customer(Customer.builder()
                            .name("Blinkit Customer " + i)
                            .email("customer" + i + "@blinkit.com")
                            .phone("+91-" + (9000000000L + ThreadLocalRandom.current().nextInt(999999999)))
                            .build())
                    .items(List.of(OrderItem.builder()
                            .productName("Cold Pressed Coconut Oil 1L")
                            .sku("OIL-002")
                            .quantity(2)
                            .unitPrice(BigDecimal.valueOf(350))
                            .totalPrice(BigDecimal.valueOf(700))
                            .build()))
                    .status(OrderStatus.PENDING)
                    .totalAmount(BigDecimal.valueOf(700))
                    .channelMetadata(Map.of(
                            "deliverySlot", "10min",
                            "darkStoreId", "DS-" + ThreadLocalRandom.current().nextInt(100, 999),
                            "deliveryPartnerId", "DP-" + ThreadLocalRandom.current().nextInt(1000, 9999)))
                    .placedAt(LocalDateTime.now())
                    .build());
        }
        return orders;
    }
}
