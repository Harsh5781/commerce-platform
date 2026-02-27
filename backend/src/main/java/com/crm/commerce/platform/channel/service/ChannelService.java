package com.crm.commerce.platform.channel.service;

import com.crm.commerce.platform.channel.connector.ChannelConnector;
import com.crm.commerce.platform.channel.dto.ChannelResponse;
import com.crm.commerce.platform.channel.model.Channel;
import com.crm.commerce.platform.channel.model.ChannelCode;
import com.crm.commerce.platform.channel.repository.ChannelRepository;
import com.crm.commerce.platform.audit.service.AuditService;
import com.crm.commerce.platform.common.exception.ResourceNotFoundException;
import com.crm.commerce.platform.common.service.SequenceGenerator;
import com.crm.commerce.platform.dashboard.service.DashboardService;
import com.crm.commerce.platform.order.model.Order;
import com.crm.commerce.platform.order.model.OrderStatus;
import com.crm.commerce.platform.order.model.OrderTimeline;
import com.crm.commerce.platform.order.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final OrderRepository orderRepository;
    private final DashboardService dashboardService;
    private final SequenceGenerator sequenceGenerator;
    private final AuditService auditService;
    private final Map<String, ChannelConnector> connectors;
    private final Counter channelSyncCounter;
    private final Counter channelSyncOrdersCounter;
    private final AtomicInteger availableChannels = new AtomicInteger(0);

    public ChannelService(ChannelRepository channelRepository,
                          OrderRepository orderRepository,
                          DashboardService dashboardService,
                          SequenceGenerator sequenceGenerator,
                          AuditService auditService,
                          List<ChannelConnector> connectorList,
                          MeterRegistry meterRegistry) {
        this.channelRepository = channelRepository;
        this.orderRepository = orderRepository;
        this.dashboardService = dashboardService;
        this.sequenceGenerator = sequenceGenerator;
        this.auditService = auditService;
        this.connectors = connectorList.stream()
                .collect(Collectors.toMap(ChannelConnector::getChannelCode, Function.identity()));
        this.channelSyncCounter = Counter.builder("channels.sync.total")
                .description("Total channel sync operations").register(meterRegistry);
        this.channelSyncOrdersCounter = Counter.builder("channels.sync.orders.total")
                .description("Total orders synced from channels").register(meterRegistry);
        Gauge.builder("channels.available", availableChannels, AtomicInteger::get)
                .description("Number of available channels").register(meterRegistry);
    }

    public List<ChannelResponse> getAllChannels() {
        List<Channel> channels = channelRepository.findAll();
        List<ChannelResponse> responses = channels.stream()
                .map(ch -> {
                    ChannelConnector connector = connectors.get(ch.getCode().name());
                    boolean available = connector != null && connector.isAvailable();
                    long count = orderRepository.countByChannel(ch.getCode().name());
                    return ChannelResponse.from(ch, available, count);
                })
                .toList();
        availableChannels.set((int) responses.stream().filter(ChannelResponse::isAvailable).count());
        return responses;
    }

    public ChannelResponse getChannelByCode(String code) {
        ChannelCode channelCode;
        try {
            channelCode = ChannelCode.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Channel", "code", code);
        }

        Channel channel = channelRepository.findByCode(channelCode)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "code", code));

        ChannelConnector connector = connectors.get(channel.getCode().name());
        boolean available = connector != null && connector.isAvailable();
        long count = orderRepository.countByChannel(channel.getCode().name());

        return ChannelResponse.from(channel, available, count);
    }

    @CacheEvict(value = "orders", allEntries = true)
    public List<Order> syncChannel(String code) {
        ChannelCode channelCode;
        try {
            channelCode = ChannelCode.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Channel", "code", code);
        }

        ChannelConnector connector = connectors.get(channelCode.name());
        if (connector == null) {
            throw new ResourceNotFoundException("Connector", "channel", code);
        }

        log.info("Syncing orders from channel: {}", code);
        List<Order> fetched = connector.fetchNewOrders();

        List<Order> saved = new ArrayList<>();
        for (Order order : fetched) {
            String prefix = channelCode.name().substring(0, 3);
            long seq = sequenceGenerator.nextValue("order_sequence");
            order.setOrderNumber(String.format("ORD-%s-%05d", prefix, seq));
            order.setStatus(OrderStatus.PENDING);
            order.setTimeline(new ArrayList<>(List.of(OrderTimeline.builder()
                    .status(OrderStatus.PENDING)
                    .changedBy("channel-sync")
                    .notes("Order synced from " + channelCode.name())
                    .timestamp(LocalDateTime.now())
                    .build())));
            saved.add(orderRepository.save(order));
        }

        channelSyncCounter.increment();
        channelSyncOrdersCounter.increment(saved.size());
        dashboardService.clearCache();
        auditService.logAction(null, "channel-sync", "CHANNEL_SYNC", "Channel", code,
                java.util.Map.of("channel", code, "ordersSynced", saved.size()));
        log.info("Synced {} orders from {}", saved.size(), code);
        return saved;
    }
}
