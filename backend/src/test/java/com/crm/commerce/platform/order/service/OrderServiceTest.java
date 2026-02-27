package com.crm.commerce.platform.order.service;

import com.crm.commerce.platform.audit.service.AuditService;
import com.crm.commerce.platform.common.exception.BadRequestException;
import com.crm.commerce.platform.common.exception.ResourceNotFoundException;
import com.crm.commerce.platform.common.exception.ValidationException;
import com.crm.commerce.platform.common.service.SequenceGenerator;
import com.crm.commerce.platform.dashboard.service.DashboardService;
import com.crm.commerce.platform.order.dto.CreateOrderRequest;
import com.crm.commerce.platform.order.dto.OrderResponse;
import com.crm.commerce.platform.order.dto.UpdateOrderStatusRequest;
import com.crm.commerce.platform.order.model.*;
import com.crm.commerce.platform.order.repository.OrderRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private DashboardService dashboardService;
    @Mock private SequenceGenerator sequenceGenerator;
    @Mock private AuditService auditService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, mongoTemplate, dashboardService,
                sequenceGenerator, auditService, new SimpleMeterRegistry());
    }

    @Test
    void getOrderById_existingOrder_returnsResponse() {
        Order order = buildSampleOrder("ord-1", "ORD-WEB-00001", OrderStatus.PENDING);
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById("ord-1");

        assertThat(response.getId()).isEqualTo("ord-1");
        assertThat(response.getOrderNumber()).isEqualTo("ORD-WEB-00001");
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void getOrderById_nonExistent_throwsResourceNotFound() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    void getOrderByNumber_existingOrder_returnsResponse() {
        Order order = buildSampleOrder("ord-1", "ORD-AMZ-00001", OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber("ORD-AMZ-00001")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderByNumber("ORD-AMZ-00001");

        assertThat(response.getOrderNumber()).isEqualTo("ORD-AMZ-00001");
    }

    @Test
    void getOrderByNumber_nonExistent_throwsResourceNotFound() {
        when(orderRepository.findByOrderNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByNumber("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrder_validRequest_savesAndReturnsResponse() {
        CreateOrderRequest request = buildValidCreateRequest("WEBSITE");
        when(sequenceGenerator.nextValue("order_sequence")).thenReturn(1L);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("generated-id");
            return o;
        });

        OrderResponse response = orderService.createOrder(request, "admin");

        assertThat(response.getOrderNumber()).isEqualTo("ORD-WEB-00001");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getChannel()).isEqualTo("WEBSITE");
        assertThat(response.getCustomer().getName()).isEqualTo("John Doe");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));

        verify(orderRepository).save(any(Order.class));
        verify(dashboardService).clearCache();
        verify(auditService).logAction(isNull(), eq("admin"), eq("CREATE_ORDER"),
                eq("Order"), any(), anyMap());
    }

    @Test
    void createOrder_amazonChannel_generatesCorrectPrefix() {
        CreateOrderRequest request = buildValidCreateRequest("AMAZON");
        when(sequenceGenerator.nextValue("order_sequence")).thenReturn(42L);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("id");
            return o;
        });

        OrderResponse response = orderService.createOrder(request, "admin");

        assertThat(response.getOrderNumber()).isEqualTo("ORD-AMA-00042");
    }

    @Test
    void createOrder_blankChannel_throwsValidation() {
        CreateOrderRequest request = buildValidCreateRequest("");

        assertThatThrownBy(() -> orderService.createOrder(request, "admin"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createOrder_invalidChannel_throwsValidation() {
        CreateOrderRequest request = buildValidCreateRequest("FLIPKART");

        assertThatThrownBy(() -> orderService.createOrder(request, "admin"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createOrder_nullCustomer_throwsValidation() {
        CreateOrderRequest request = buildValidCreateRequest("WEBSITE");
        request.setCustomer(null);

        assertThatThrownBy(() -> orderService.createOrder(request, "admin"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createOrder_emptyItems_throwsValidation() {
        CreateOrderRequest request = buildValidCreateRequest("WEBSITE");
        request.setItems(List.of());

        assertThatThrownBy(() -> orderService.createOrder(request, "admin"))
                .isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, CONFIRMED",
            "PENDING, CANCELLED",
            "CONFIRMED, PROCESSING",
            "CONFIRMED, CANCELLED",
            "PROCESSING, SHIPPED",
            "PROCESSING, CANCELLED",
            "SHIPPED, DELIVERED",
            "SHIPPED, RETURNED",
            "DELIVERED, RETURNED",
            "DELIVERED, REFUNDED",
            "RETURNED, REFUNDED"
    })
    void updateOrderStatus_validTransitions_succeeds(String from, String to) {
        Order order = buildSampleOrder("ord-1", "ORD-WEB-00001", OrderStatus.valueOf(from));
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(to, "Test note");

        OrderResponse response = orderService.updateOrderStatus("ord-1", request, "admin");

        assertThat(response.getStatus()).isEqualTo(to);
        verify(dashboardService).clearCache();
        verify(auditService).logAction(isNull(), eq("admin"), eq("UPDATE_ORDER_STATUS"),
                eq("Order"), eq("ord-1"), anyMap());
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, DELIVERED",
            "PENDING, SHIPPED",
            "CONFIRMED, DELIVERED",
            "SHIPPED, CONFIRMED",
            "CANCELLED, CONFIRMED",
            "REFUNDED, PENDING"
    })
    void updateOrderStatus_invalidTransitions_throwsBadRequest(String from, String to) {
        Order order = buildSampleOrder("ord-1", "ORD-WEB-00001", OrderStatus.valueOf(from));
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(to, null);

        assertThatThrownBy(() -> orderService.updateOrderStatus("ord-1", request, "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void updateOrderStatus_nonExistentOrder_throwsResourceNotFound() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest("CONFIRMED", null);

        assertThatThrownBy(() -> orderService.updateOrderStatus("missing", request, "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateOrderStatus_blankStatus_throwsValidation() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest("", null);

        assertThatThrownBy(() -> orderService.updateOrderStatus("ord-1", request, "admin"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateOrderStatus_invalidStatusValue_throwsBadRequest() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest("INVALID_STATUS", null);

        assertThatThrownBy(() -> orderService.updateOrderStatus("ord-1", request, "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid order status");
    }

    @Test
    void updateOrderStatus_addsTimelineEntry() {
        Order order = buildSampleOrder("ord-1", "ORD-WEB-00001", OrderStatus.PENDING);
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest("CONFIRMED", "Approved by admin");

        OrderResponse response = orderService.updateOrderStatus("ord-1", request, "admin");

        assertThat(response.getTimeline()).hasSize(2);
    }

    @Test
    void updateOrderStatus_capturesPreviousStatusForAudit() {
        Order order = buildSampleOrder("ord-1", "ORD-WEB-00001", OrderStatus.PENDING);
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest("CONFIRMED", null);
        orderService.updateOrderStatus("ord-1", request, "admin");

        verify(auditService).logAction(isNull(), eq("admin"), eq("UPDATE_ORDER_STATUS"),
                eq("Order"), eq("ord-1"), argThat(details ->
                        "PENDING".equals(details.get("previousStatus")) &&
                                "CONFIRMED".equals(details.get("newStatus"))
                ));
    }

    // --- Helper methods ---

    private Order buildSampleOrder(String id, String orderNumber, OrderStatus status) {
        return Order.builder()
                .id(id)
                .orderNumber(orderNumber)
                .channel("WEBSITE")
                .customer(Customer.builder().name("John Doe").email("john@test.com").phone("1234567890").build())
                .items(List.of(OrderItem.builder()
                        .productName("Widget").sku("WGT-001").quantity(2)
                        .unitPrice(new BigDecimal("100.00")).totalPrice(new BigDecimal("200.00")).build()))
                .status(status)
                .totalAmount(new BigDecimal("200.00"))
                .shippingAddress(Address.builder().line1("123 Main St").city("Delhi").state("DL").pincode("110001").country("IN").build())
                .timeline(new ArrayList<>(List.of(OrderTimeline.builder()
                        .status(OrderStatus.PENDING).changedBy("system").notes("Order created")
                        .timestamp(LocalDateTime.now()).build())))
                .placedAt(LocalDateTime.now())
                .build();
    }

    private CreateOrderRequest buildValidCreateRequest(String channel) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setChannel(channel);
        request.setChannelOrderRef("EXT-001");
        request.setCustomer(new CreateOrderRequest.CustomerDto("John Doe", "john@test.com", "1234567890"));
        request.setItems(List.of(new CreateOrderRequest.OrderItemDto("Widget", "WGT-001", 2, new BigDecimal("100.00"))));
        request.setShippingAddress(new CreateOrderRequest.AddressDto("123 Main St", null, "Delhi", "DL", "110001", "IN"));
        request.setChannelMetadata(Map.of("source", "test"));
        return request;
    }
}
