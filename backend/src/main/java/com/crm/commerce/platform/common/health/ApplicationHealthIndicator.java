package com.crm.commerce.platform.common.health;

import com.crm.commerce.platform.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;
    private final OrderRepository orderRepository;

    @Override
    public Health health() {
        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            long orderCount = orderRepository.count();
            return Health.up()
                    .withDetail("database", mongoTemplate.getDb().getName())
                    .withDetail("totalOrders", orderCount)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
