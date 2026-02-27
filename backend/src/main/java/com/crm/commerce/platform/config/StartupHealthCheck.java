package com.crm.commerce.platform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class StartupHealthCheck implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;
    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Running pre-flight health checks ===");
        checkMongoDB();
        checkRedis();
        validateConfiguration();
        log.info("=== All health checks passed. Application is ready ===");
    }

    private void checkMongoDB() {
        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            log.info("[OK] MongoDB connection verified - database: {}", mongoTemplate.getDb().getName());
        } catch (Exception e) {
            log.error("[FAIL] MongoDB connection failed: {}", e.getMessage());
            throw new IllegalStateException("Cannot start: MongoDB is unreachable", e);
        }
    }

    private void checkRedis() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("[OK] Redis connection verified - response: {} (used for order caching)", pong);
        } catch (Exception e) {
            log.warn("[WARN] Redis connection failed: {}. Order caching will fall through to MongoDB.", e.getMessage());
        }
    }

    private void validateConfiguration() {
        log.info("[OK] JWT secret configured (length: {})", appProperties.getJwt().getSecret().length());
        log.info("[OK] CORS origins: {}", appProperties.getCors().getAllowedOrigins());
        log.info("[OK] Rate limit: {} req/min", appProperties.getRateLimit().getRequestsPerMinute());
        log.info("[OK] Feature flags - channelSync: {}, auditLog: {}, notifications: {}",
                appProperties.getFeatureFlags().isEnableChannelSync(),
                appProperties.getFeatureFlags().isEnableAuditLog(),
                appProperties.getFeatureFlags().isEnableNotifications());
    }
}
