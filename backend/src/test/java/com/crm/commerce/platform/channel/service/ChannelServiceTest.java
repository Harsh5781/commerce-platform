package com.crm.commerce.platform.channel.service;

import com.crm.commerce.platform.audit.service.AuditService;
import com.crm.commerce.platform.channel.connector.ChannelConnector;
import com.crm.commerce.platform.channel.dto.ChannelResponse;
import com.crm.commerce.platform.channel.model.Channel;
import com.crm.commerce.platform.channel.model.ChannelCode;
import com.crm.commerce.platform.channel.model.ChannelStatus;
import com.crm.commerce.platform.channel.repository.ChannelRepository;
import com.crm.commerce.platform.common.exception.ResourceNotFoundException;
import com.crm.commerce.platform.common.service.SequenceGenerator;
import com.crm.commerce.platform.dashboard.service.DashboardService;
import com.crm.commerce.platform.order.model.Customer;
import com.crm.commerce.platform.order.model.Order;
import com.crm.commerce.platform.order.repository.OrderRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock private ChannelRepository channelRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private DashboardService dashboardService;
    @Mock private SequenceGenerator sequenceGenerator;
    @Mock private AuditService auditService;
    @Mock private ChannelConnector websiteConnector;
    @Mock private ChannelConnector amazonConnector;

    private ChannelService channelService;

    @BeforeEach
    void setUp() {
        when(websiteConnector.getChannelCode()).thenReturn("WEBSITE");
        when(amazonConnector.getChannelCode()).thenReturn("AMAZON");

        channelService = new ChannelService(
                channelRepository, orderRepository, dashboardService,
                sequenceGenerator, auditService,
                List.of(websiteConnector, amazonConnector),
                new SimpleMeterRegistry());
    }

    @Test
    void getAllChannels_returnsAllWithAvailabilityAndCount() {
        Channel websiteChannel = buildChannel("ch-1", "Organic Website", ChannelCode.WEBSITE);
        Channel amazonChannel = buildChannel("ch-2", "Amazon", ChannelCode.AMAZON);

        when(channelRepository.findAll()).thenReturn(List.of(websiteChannel, amazonChannel));
        when(websiteConnector.isAvailable()).thenReturn(true);
        when(amazonConnector.isAvailable()).thenReturn(false);
        when(orderRepository.countByChannel("WEBSITE")).thenReturn(50L);
        when(orderRepository.countByChannel("AMAZON")).thenReturn(30L);

        List<ChannelResponse> channels = channelService.getAllChannels();

        assertThat(channels).hasSize(2);
        assertThat(channels.get(0).isAvailable()).isTrue();
        assertThat(channels.get(0).getOrderCount()).isEqualTo(50);
        assertThat(channels.get(1).isAvailable()).isFalse();
        assertThat(channels.get(1).getOrderCount()).isEqualTo(30);
    }

    @Test
    void getChannelByCode_existingChannel_returnsResponse() {
        Channel channel = buildChannel("ch-1", "Organic Website", ChannelCode.WEBSITE);
        when(channelRepository.findByCode(ChannelCode.WEBSITE)).thenReturn(Optional.of(channel));
        when(websiteConnector.isAvailable()).thenReturn(true);
        when(orderRepository.countByChannel("WEBSITE")).thenReturn(25L);

        ChannelResponse response = channelService.getChannelByCode("WEBSITE");

        assertThat(response.getCode()).isEqualTo("WEBSITE");
        assertThat(response.getName()).isEqualTo("Organic Website");
        assertThat(response.isAvailable()).isTrue();
        assertThat(response.getOrderCount()).isEqualTo(25);
    }

    @Test
    void getChannelByCode_invalidCode_throwsResourceNotFound() {
        assertThatThrownBy(() -> channelService.getChannelByCode("FLIPKART"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getChannelByCode_notInDb_throwsResourceNotFound() {
        when(channelRepository.findByCode(ChannelCode.WEBSITE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.getChannelByCode("WEBSITE"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void syncChannel_fetchesAndSavesOrders() {
        Order mockOrder = Order.builder()
                .channel("WEBSITE")
                .customer(Customer.builder().name("Jane").email("jane@test.com").build())
                .totalAmount(new BigDecimal("500.00"))
                .items(new ArrayList<>())
                .build();

        when(websiteConnector.fetchNewOrders()).thenReturn(List.of(mockOrder));
        when(sequenceGenerator.nextValue("order_sequence")).thenReturn(10L);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("saved-id");
            return o;
        });

        List<Order> synced = channelService.syncChannel("WEBSITE");

        assertThat(synced).hasSize(1);
        assertThat(synced.get(0).getOrderNumber()).isEqualTo("ORD-WEB-00010");
        verify(dashboardService).clearCache();
        verify(auditService).logAction(isNull(), eq("channel-sync"), eq("CHANNEL_SYNC"),
                eq("Channel"), eq("WEBSITE"), anyMap());
    }

    @Test
    void syncChannel_invalidCode_throwsResourceNotFound() {
        assertThatThrownBy(() -> channelService.syncChannel("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void syncChannel_noConnector_throwsResourceNotFound() {
        assertThatThrownBy(() -> channelService.syncChannel("BLINKIT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Connector");
    }

    private Channel buildChannel(String id, String name, ChannelCode code) {
        return Channel.builder()
                .id(id)
                .name(name)
                .code(code)
                .status(ChannelStatus.ACTIVE)
                .description("Test channel")
                .build();
    }
}
