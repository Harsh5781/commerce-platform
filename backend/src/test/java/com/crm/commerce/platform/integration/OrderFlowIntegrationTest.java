package com.crm.commerce.platform.integration;

import com.crm.commerce.platform.auth.dto.LoginRequest;
import com.crm.commerce.platform.user.enums.Role;
import com.crm.commerce.platform.user.model.User;
import com.crm.commerce.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private CacheManager cacheManager;
    @MockBean private StringRedisTemplate stringRedisTemplate;

    private static String accessToken;
    private static String createdOrderId;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache(anyString())).thenAnswer(inv ->
                new ConcurrentMapCache(inv.getArgument(0)));

        if (userRepository.findByEmail("inttest@test.com").isEmpty()) {
            userRepository.save(User.builder()
                    .email("inttest@test.com")
                    .passwordHash(passwordEncoder.encode("testpass123"))
                    .name("Integration Test Admin")
                    .role(Role.ADMIN)
                    .active(true)
                    .build());
        }
    }

    @Test
    @Order(1)
    void login_returnsTokens() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("inttest@test.com", "testpass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        accessToken = node.get("data").get("accessToken").asText();
        assertThat(accessToken).isNotBlank();
    }

    @Test
    @Order(2)
    void createOrder_returnsCreatedOrder() throws Exception {
        ensureLoggedIn();

        String orderPayload = """
                {
                    "channel": "WEBSITE",
                    "channelOrderRef": "INT-TEST-001",
                    "customer": {
                        "name": "Integration Test Customer",
                        "email": "customer@inttest.com",
                        "phone": "9876543210"
                    },
                    "items": [
                        {"productName": "Test Product", "sku": "TST-001", "quantity": 2, "unitPrice": 250.00}
                    ],
                    "shippingAddress": {
                        "line1": "456 Test Avenue", "city": "Mumbai", "state": "MH", "pincode": "400001", "country": "IN"
                    }
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.channel").value("WEBSITE"))
                .andExpect(jsonPath("$.data.totalAmount").value(500.00))
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        createdOrderId = node.get("data").get("id").asText();
        assertThat(createdOrderId).isNotBlank();
    }

    @Test
    @Order(3)
    void getOrderById_returnsCreatedOrder() throws Exception {
        ensureOrderCreated();

        mockMvc.perform(get("/api/orders/" + createdOrderId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdOrderId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @Order(4)
    void updateOrderStatus_pendingToConfirmed() throws Exception {
        ensureOrderCreated();

        mockMvc.perform(patch("/api/orders/" + createdOrderId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CONFIRMED", "notes": "Integration test confirmation"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.timeline.length()").value(2));
    }

    @Test
    @Order(5)
    void updateOrderStatus_confirmedToProcessing() throws Exception {
        ensureOrderCreated();

        mockMvc.perform(patch("/api/orders/" + createdOrderId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PROCESSING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    @Order(6)
    void updateOrderStatus_invalidTransition_returns400() throws Exception {
        ensureOrderCreated();

        mockMvc.perform(patch("/api/orders/" + createdOrderId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DELIVERED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(7)
    void listOrders_returnsPagedResults() throws Exception {
        ensureLoggedIn();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    @Order(8)
    void listOrders_filterByChannel() throws Exception {
        ensureLoggedIn();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("channel", "WEBSITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(9)
    void getDashboardStats_returnsStats() throws Exception {
        ensureLoggedIn();

        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").isNumber())
                .andExpect(jsonPath("$.data.channelBreakdown").isArray());
    }

    @Test
    @Order(10)
    void getChannels_returnsChannelList() throws Exception {
        ensureLoggedIn();

        mockMvc.perform(get("/api/channels")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(11)
    void unauthenticatedRequest_returns401or403() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(12)
    void healthEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    private void ensureLoggedIn() throws Exception {
        if (accessToken == null) {
            login_returnsTokens();
        }
    }

    private void ensureOrderCreated() throws Exception {
        ensureLoggedIn();
        if (createdOrderId == null) {
            createOrder_returnsCreatedOrder();
        }
    }
}
