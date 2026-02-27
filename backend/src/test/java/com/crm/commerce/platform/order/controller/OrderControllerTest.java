package com.crm.commerce.platform.order.controller;

import com.crm.commerce.platform.auth.jwt.JwtTokenProvider;
import com.crm.commerce.platform.auth.security.CustomUserDetails;
import com.crm.commerce.platform.auth.security.CustomUserDetailsService;
import com.crm.commerce.platform.common.exception.BadRequestException;
import com.crm.commerce.platform.common.exception.ResourceNotFoundException;
import com.crm.commerce.platform.config.AppProperties;
import com.crm.commerce.platform.config.CorsConfig;
import com.crm.commerce.platform.config.SecurityConfig;
import com.crm.commerce.platform.order.dto.OrderResponse;
import com.crm.commerce.platform.order.model.*;
import com.crm.commerce.platform.order.service.OrderService;
import com.crm.commerce.platform.user.enums.Role;
import com.crm.commerce.platform.user.model.User;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@EnableConfigurationProperties(AppProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-that-is-at-least-32-characters-long-for-hmac",
        "app.jwt.expiration-ms=3600000",
        "app.jwt.refresh-expiration-ms=86400000",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.rate-limit.requests-per-minute=60",
        "app.shutdown.max-wait-seconds=30"
})
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private OrderService orderService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private MeterRegistry meterRegistry;

    private CustomUserDetails adminUser;
    private CustomUserDetails viewerUser;

    @BeforeEach
    void setUp() {
        adminUser = new CustomUserDetails(User.builder()
                .id("u-1").email("admin@test.com").name("Admin")
                .passwordHash("hash").role(Role.ADMIN).active(true).build());
        viewerUser = new CustomUserDetails(User.builder()
                .id("u-3").email("viewer@test.com").name("Viewer")
                .passwordHash("hash").role(Role.VIEWER).active(true).build());
    }

    @Test
    void getOrders_returnsPagedOrders() throws Exception {
        OrderResponse order = buildOrderResponse("ord-1", "ORD-WEB-00001");
        Page<OrderResponse> page = new PageImpl<>(List.of(order));
        when(orderService.getOrders(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/orders").with(user(adminUser)).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-WEB-00001"));
    }

    @Test
    void getOrders_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getOrderById_returnsOrder() throws Exception {
        OrderResponse order = buildOrderResponse("ord-1", "ORD-WEB-00001");
        when(orderService.getOrderById("ord-1")).thenReturn(order);

        mockMvc.perform(get("/api/orders/ord-1").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ord-1"))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-WEB-00001"));
    }

    @Test
    void getOrderById_notFound_returns404() throws Exception {
        when(orderService.getOrderById("missing")).thenThrow(new ResourceNotFoundException("Order", "id", "missing"));

        mockMvc.perform(get("/api/orders/missing").with(user(adminUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getOrderByNumber_returnsOrder() throws Exception {
        OrderResponse order = buildOrderResponse("ord-1", "ORD-AMZ-00001");
        when(orderService.getOrderByNumber("ORD-AMZ-00001")).thenReturn(order);

        mockMvc.perform(get("/api/orders/number/ORD-AMZ-00001").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-AMZ-00001"));
    }

    @Test
    void createOrder_asAdmin_returns201() throws Exception {
        OrderResponse order = buildOrderResponse("ord-1", "ORD-WEB-00001");
        when(orderService.createOrder(any(), eq("Admin"))).thenReturn(order);

        String body = """
                {"channel":"WEBSITE","customer":{"name":"John","email":"j@t.com","phone":"1"},"items":[{"productName":"W","sku":"W-1","quantity":1,"unitPrice":100}],"shippingAddress":{"line1":"123","city":"D","state":"DL","pincode":"110001","country":"IN"}}
                """;

        mockMvc.perform(post("/api/orders").with(user(adminUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createOrder_asViewer_returns403() throws Exception {
        mockMvc.perform(post("/api/orders").with(user(viewerUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_asAdmin_returns200() throws Exception {
        OrderResponse order = buildOrderResponse("ord-1", "ORD-WEB-00001");
        order.setStatus("CONFIRMED");
        when(orderService.updateOrderStatus(eq("ord-1"), any(), eq("Admin"))).thenReturn(order);

        mockMvc.perform(patch("/api/orders/ord-1/status").with(user(adminUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED","notes":"Approved"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void updateStatus_invalidTransition_returns400() throws Exception {
        when(orderService.updateOrderStatus(eq("ord-1"), any(), any()))
                .thenThrow(new BadRequestException("Cannot transition from PENDING to DELIVERED"));

        mockMvc.perform(patch("/api/orders/ord-1/status").with(user(adminUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DELIVERED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateStatus_asViewer_returns403() throws Exception {
        mockMvc.perform(patch("/api/orders/ord-1/status").with(user(viewerUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isForbidden());
    }

    private OrderResponse buildOrderResponse(String id, String orderNumber) {
        return OrderResponse.builder()
                .id(id).orderNumber(orderNumber).channel("WEBSITE")
                .customer(Customer.builder().name("John").email("j@t.com").phone("1").build())
                .items(List.of(OrderItem.builder().productName("W").sku("W-1").quantity(1)
                        .unitPrice(new BigDecimal("100")).totalPrice(new BigDecimal("100")).build()))
                .status("PENDING").totalAmount(new BigDecimal("100.00"))
                .shippingAddress(Address.builder().line1("123").city("D").state("DL").pincode("110001").build())
                .timeline(List.of(OrderTimeline.builder().status(OrderStatus.PENDING)
                        .changedBy("system").timestamp(LocalDateTime.now()).build()))
                .placedAt(LocalDateTime.now()).build();
    }
}
