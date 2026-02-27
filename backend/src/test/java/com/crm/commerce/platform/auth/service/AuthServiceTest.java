package com.crm.commerce.platform.auth.service;

import com.crm.commerce.platform.auth.dto.*;
import com.crm.commerce.platform.auth.jwt.JwtTokenProvider;
import com.crm.commerce.platform.auth.security.CustomUserDetails;
import com.crm.commerce.platform.common.exception.DuplicateResourceException;
import com.crm.commerce.platform.common.exception.ValidationException;
import com.crm.commerce.platform.config.AppProperties;
import com.crm.commerce.platform.user.enums.Role;
import com.crm.commerce.platform.user.model.User;
import com.crm.commerce.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AppProperties appProperties;

    @InjectMocks
    private AuthService authService;

    private AppProperties.Jwt jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new AppProperties.Jwt();
        jwtConfig.setExpirationMs(3600000);
    }

    @Test
    void login_validCredentials_returnsTokens() {
        LoginRequest request = new LoginRequest("admin@test.com", "password");
        User user = buildUser("u-1", "admin@test.com", "Admin", Role.ADMIN);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(appProperties.getJwt()).thenReturn(jwtConfig);

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600000);
        assertThat(response.getUser().getEmail()).isEqualTo("admin@test.com");
        assertThat(response.getUser().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void login_blankEmail_throwsValidation() {
        LoginRequest request = new LoginRequest("", "password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void login_blankPassword_throwsValidation() {
        LoginRequest request = new LoginRequest("admin@test.com", "");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void register_newUser_createsAndReturnsUserInfo() {
        RegisterRequest request = new RegisterRequest("new@test.com", "password123", "New User", "MANAGER");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("generated-id");
            return u;
        });

        UserInfo result = authService.register(request);

        assertThat(result.getEmail()).isEqualTo("new@test.com");
        assertThat(result.getName()).isEqualTo("New User");
        assertThat(result.getRole()).isEqualTo("MANAGER");
        verify(userRepository).save(argThat(user ->
                "hashed".equals(user.getPasswordHash()) && user.getRole() == Role.MANAGER));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResource() {
        RegisterRequest request = new RegisterRequest("existing@test.com", "password123", "User", null);

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");
    }

    @Test
    void register_blankEmail_throwsValidation() {
        RegisterRequest request = new RegisterRequest("", "password123", "User", null);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void register_shortPassword_throwsValidation() {
        RegisterRequest request = new RegisterRequest("user@test.com", "abc", "User", null);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void register_invalidEmail_throwsValidation() {
        RegisterRequest request = new RegisterRequest("not-an-email", "password123", "User", null);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void register_noRole_defaultsToViewer() {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123", "Viewer User", null);

        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("id");
            return u;
        });

        authService.register(request);

        verify(userRepository).save(argThat(user -> user.getRole() == Role.VIEWER));
    }

    @Test
    void register_invalidRole_defaultsToViewer() {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123", "User", "SUPERADMIN");

        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("id");
            return u;
        });

        authService.register(request);

        verify(userRepository).save(argThat(user -> user.getRole() == Role.VIEWER));
    }

    @Test
    void refreshToken_validToken_returnsNewTokens() {
        TokenRefreshRequest request = new TokenRefreshRequest("valid-refresh-token");
        User user = buildUser("u-1", "admin@test.com", "Admin", Role.ADMIN);

        doNothing().when(jwtTokenProvider).validateTokenOrThrow("valid-refresh-token");
        when(jwtTokenProvider.getEmailFromToken("valid-refresh-token")).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("new-refresh");
        when(appProperties.getJwt()).thenReturn(jwtConfig);

        LoginResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refreshToken_blankToken_throwsValidation() {
        TokenRefreshRequest request = new TokenRefreshRequest("");

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getCurrentUser_returnsUserInfo() {
        User user = buildUser("u-1", "admin@test.com", "Admin", Role.ADMIN);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        UserInfo result = authService.getCurrentUser(userDetails);

        assertThat(result.getId()).isEqualTo("u-1");
        assertThat(result.getEmail()).isEqualTo("admin@test.com");
        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    private User buildUser(String id, String email, String name, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .name(name)
                .passwordHash("$2a$12$hashedpassword")
                .role(role)
                .active(true)
                .build();
    }
}
