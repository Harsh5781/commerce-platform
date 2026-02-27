package com.crm.commerce.platform.common.health;

import com.crm.commerce.platform.channel.connector.ChannelConnector;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ChannelConnectorHealthIndicator implements HealthIndicator {

    private final Map<String, ChannelConnector> connectors;

    public ChannelConnectorHealthIndicator(List<ChannelConnector> connectorList) {
        this.connectors = connectorList.stream()
                .collect(Collectors.toMap(ChannelConnector::getChannelCode, Function.identity()));
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        int upCount = 0;

        for (Map.Entry<String, ChannelConnector> entry : connectors.entrySet()) {
            boolean available;
            try {
                available = entry.getValue().isAvailable();
            } catch (Exception e) {
                available = false;
            }
            builder.withDetail(entry.getKey(), available ? "UP" : "DOWN (circuit breaker may be open)");
            if (available) upCount++;
        }

        if (upCount == 0 && !connectors.isEmpty()) {
            builder.down().withDetail("reason", "All channel connectors are down");
        } else if (upCount < connectors.size()) {
            builder.status("DEGRADED");
        }

        builder.withDetail("totalChannels", connectors.size());
        builder.withDetail("availableChannels", upCount);
        return builder.build();
    }
}
