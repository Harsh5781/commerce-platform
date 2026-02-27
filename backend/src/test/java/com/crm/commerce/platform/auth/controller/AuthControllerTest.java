package com.crm.commerce.platform.auth.controller;

import com.crm.commerce.platform.auth.dto.LoginResponse;
import com.crm.commerce.platform.auth.dto.UserInfo;
import com.crm.commerce.platform.auth.jwt.JwtTokenProvider;
import com.crm.commerce.platform.auth.security.CustomUserDetails;
import com.crm.commerce.platform.auth.security.CustomUserDetailsService;
import com.crm.commerce.platform.auth.service.AuthService;
import com.crm.commerce.platform.common.exception.ValidationException;
import com.crm.commerce.platform.config.AppProperties;
import com.crm.commerce.platform.config.CorsConfig;
import com.crm.commerce.platform.config.SecurityConfig;
import com.crm.commerce.platform.user.enums.Role;
import com.crm.commerce.platform.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@EnableConfigurationProperties(AppProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-that-is-at-least-32-characters-long-for-hmac",
        "app.jwt.expiration-ms=3600000", "app.jwt.refresh-expiration-ms=86400000",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.rate-limit.requests-per-minute=60", "app.shutdown.max-wait-seconds=30"
})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuthService authService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new CustomUserDetails(User.builder()
                .id("u-1").email("admin@test.com").name("Admin")
                .passwordHash("hash").role(Role.ADMIN).active(true).build());
    }

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token").refreshToken("refresh-token")
                .tokenType("Bearer").expiresIn(3600000)
                .user(UserInfo.builder().id("u-1").email("admin@test.com").name("Admin").role("ADMIN").build())
                .build();
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.com","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void login_invalidCredentials_returns400() throws Exception {
        when(authService.login(any())).thenThrow(new ValidationException(Map.of("email", "email must not be blank")));

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":"admin123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_asAdmin_returns201() throws Exception {
        when(authService.register(any())).thenReturn(
                UserInfo.builder().id("u-2").email("new@t.com").name("New").role("MANAGER").build());

        mockMvc.perform(post("/api/auth/register").with(user(adminUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@t.com","password":"pass123","name":"New","role":"MANAGER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("new@t.com"));
    }

    @Test
    void register_asViewer_returns403() throws Exception {
        CustomUserDetails viewer = new CustomUserDetails(User.builder()
                .id("u-3").email("v@t.com").name("V")
                .passwordHash("h").role(Role.VIEWER).active(true).build());

        mockMvc.perform(post("/api/auth/register").with(user(viewer)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refresh_validToken_returns200() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .accessToken("new-access").refreshToken("new-refresh")
                .tokenType("Bearer").expiresIn(3600000)
                .user(UserInfo.builder().id("u-1").email("a@t.com").name("A").role("ADMIN").build())
                .build();
        when(authService.refreshToken(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"valid-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

    @Test
    void me_authenticated_returnsUserInfo() throws Exception {
        when(authService.getCurrentUser(any(CustomUserDetails.class)))
                .thenReturn(UserInfo.builder().id("u-1").email("admin@test.com").name("Admin").role("ADMIN").build());

        mockMvc.perform(get("/api/auth/me").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@test.com"));
    }

    @Test
    void me_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is4xxClientError());
    }
}
