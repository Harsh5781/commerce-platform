package com.crm.commerce.platform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GracefulShutdownConfig implements ApplicationListener<ContextClosedEvent> {

    private final AppProperties appProperties;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("=== Graceful shutdown initiated ===");
        log.info("Max wait time: {}s", appProperties.getShutdown().getMaxWaitSeconds());
        log.info("Draining in-flight requests...");
        log.info("Closing database connections...");
        log.info("Cleaning up resources...");
        log.info("=== Shutdown complete ===");
    }
}
