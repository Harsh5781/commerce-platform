package com.crm.commerce.platform.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();
    private FeatureFlags featureFlags = new FeatureFlags();
    private Shutdown shutdown = new Shutdown();

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(jwt.getSecret())) {
            throw new IllegalStateException("app.jwt.secret must not be blank");
        }
        if (jwt.getSecret().length() < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters for HMAC-SHA256");
        }
        if (jwt.getExpirationMs() < 60000) {
            throw new IllegalStateException("app.jwt.expiration-ms must be at least 60000 (1 minute)");
        }
        if (jwt.getRefreshExpirationMs() < jwt.getExpirationMs()) {
            throw new IllegalStateException("app.jwt.refresh-expiration-ms must be greater than expiration-ms");
        }
        if (rateLimit.getRequestsPerMinute() < 1) {
            throw new IllegalStateException("app.rate-limit.requests-per-minute must be at least 1");
        }
        if (shutdown.getMaxWaitSeconds() < 5) {
            throw new IllegalStateException("app.shutdown.max-wait-seconds must be at least 5");
        }
    }

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 3600000;
        private long refreshExpirationMs = 86400000;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }

    @Data
    public static class RateLimit {
        private int requestsPerMinute = 60;
    }

    @Data
    public static class FeatureFlags {
        private boolean enableChannelSync = true;
        private boolean enableAuditLog = true;
        private boolean enableNotifications = false;
    }

    @Data
    public static class Shutdown {
        private int maxWaitSeconds = 30;
    }
}
