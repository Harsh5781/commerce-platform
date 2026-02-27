package com.crm.commerce.platform.channel.controller;

import com.crm.commerce.platform.auth.jwt.JwtTokenProvider;
import com.crm.commerce.platform.auth.security.CustomUserDetails;
import com.crm.commerce.platform.auth.security.CustomUserDetailsService;
import com.crm.commerce.platform.channel.dto.ChannelResponse;
import com.crm.commerce.platform.channel.service.ChannelService;
import com.crm.commerce.platform.common.exception.ResourceNotFoundException;
import com.crm.commerce.platform.config.AppProperties;
import com.crm.commerce.platform.config.CorsConfig;
import com.crm.commerce.platform.config.SecurityConfig;
import com.crm.commerce.platform.order.model.Order;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChannelController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@EnableConfigurationProperties(AppProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-that-is-at-least-32-characters-long-for-hmac",
        "app.jwt.expiration-ms=3600000", "app.jwt.refresh-expiration-ms=86400000",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.rate-limit.requests-per-minute=60", "app.shutdown.max-wait-seconds=30"
})
class ChannelControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ChannelService channelService;
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
    void getAllChannels_returnsList() throws Exception {
        when(channelService.getAllChannels()).thenReturn(List.of(
                ChannelResponse.builder().id("ch-1").name("Website").code("WEBSITE")
                        .status("ACTIVE").available(true).orderCount(50).build(),
                ChannelResponse.builder().id("ch-2").name("Amazon").code("AMAZON")
                        .status("ACTIVE").available(true).orderCount(30).build()
        ));

        mockMvc.perform(get("/api/channels").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].code").value("WEBSITE"));
    }

    @Test
    void getChannelByCode_returnsChannel() throws Exception {
        when(channelService.getChannelByCode("WEBSITE")).thenReturn(
                ChannelResponse.builder().id("ch-1").name("Website").code("WEBSITE")
                        .status("ACTIVE").available(true).orderCount(50).build());

        mockMvc.perform(get("/api/channels/WEBSITE").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("WEBSITE"));
    }

    @Test
    void getChannelByCode_notFound_returns404() throws Exception {
        when(channelService.getChannelByCode("FLIPKART"))
                .thenThrow(new ResourceNotFoundException("Channel", "code", "FLIPKART"));

        mockMvc.perform(get("/api/channels/FLIPKART").with(user(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void syncChannel_asAdmin_returns200() throws Exception {
        when(channelService.syncChannel("WEBSITE")).thenReturn(List.of(new Order(), new Order()));

        mockMvc.perform(post("/api/channels/WEBSITE/sync").with(user(adminUser)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    void syncChannel_asViewer_returns403() throws Exception {
        mockMvc.perform(post("/api/channels/WEBSITE/sync").with(user(viewerUser)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllChannels_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/channels"))
                .andExpect(status().is4xxClientError());
    }
}
