package com.crm.commerce.platform.auth.jwt;

import com.crm.commerce.platform.auth.security.CustomUserDetails;
import com.crm.commerce.platform.common.exception.InvalidTokenException;
import com.crm.commerce.platform.config.AppProperties;
import com.crm.commerce.platform.user.enums.Role;
import com.crm.commerce.platform.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private CustomUserDetails adminDetails;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("test-secret-key-that-is-at-least-32-characters-long-for-hmac");
        appProperties.getJwt().setExpirationMs(3600000);
        appProperties.getJwt().setRefreshExpirationMs(86400000);

        jwtTokenProvider = new JwtTokenProvider(appProperties);
        jwtTokenProvider.init();

        User adminUser = User.builder()
                .id("user-1")
                .email("admin@test.com")
                .name("Admin User")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .active(true)
                .build();
        adminDetails = new CustomUserDetails(adminUser);
    }

    @Test
    void generateAccessToken_returnsNonBlankToken() {
        String token = jwtTokenProvider.generateAccessToken(adminDetails);

        assertThat(token).isNotBlank();
    }

    @Test
    void generateRefreshToken_returnsNonBlankToken() {
        String token = jwtTokenProvider.generateRefreshToken(adminDetails);

        assertThat(token).isNotBlank();
    }

    @Test
    void validateToken_withValidToken_returnsTrue() {
        String token = jwtTokenProvider.generateAccessToken(adminDetails);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_withInvalidToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    void validateToken_withEmptyToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        AppProperties expiredProps = new AppProperties();
        expiredProps.getJwt().setSecret("test-secret-key-that-is-at-least-32-characters-long-for-hmac");
        expiredProps.getJwt().setExpirationMs(0);
        expiredProps.getJwt().setRefreshExpirationMs(0);

        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);
        expiredProvider.init();

        String token = expiredProvider.generateAccessToken(adminDetails);

        assertThat(expiredProvider.validateToken(token)).isFalse();
    }

    @Test
    void getEmailFromToken_returnsCorrectEmail() {
        String token = jwtTokenProvider.generateAccessToken(adminDetails);

        String email = jwtTokenProvider.getEmailFromToken(token);

        assertThat(email).isEqualTo("admin@test.com");
    }

    @Test
    void getUserIdFromToken_returnsCorrectId() {
        String token = jwtTokenProvider.generateAccessToken(adminDetails);

        String userId = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(userId).isEqualTo("user-1");
    }

    @Test
    void validateTokenOrThrow_withValidToken_doesNotThrow() {
        String token = jwtTokenProvider.generateAccessToken(adminDetails);

        assertThatCode(() -> jwtTokenProvider.validateTokenOrThrow(token))
                .doesNotThrowAnyException();
    }

    @Test
    void validateTokenOrThrow_withInvalidToken_throwsInvalidTokenException() {
        assertThatThrownBy(() -> jwtTokenProvider.validateTokenOrThrow("invalid.token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateTokenOrThrow_withExpiredToken_throwsInvalidTokenException() {
        AppProperties expiredProps = new AppProperties();
        expiredProps.getJwt().setSecret("test-secret-key-that-is-at-least-32-characters-long-for-hmac");
        expiredProps.getJwt().setExpirationMs(0);
        expiredProps.getJwt().setRefreshExpirationMs(0);

        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);
        expiredProvider.init();

        String token = expiredProvider.generateAccessToken(adminDetails);

        assertThatThrownBy(() -> expiredProvider.validateTokenOrThrow(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void differentUsersGetDifferentTokens() {
        User managerUser = User.builder()
                .id("user-2")
                .email("manager@test.com")
                .name("Manager")
                .passwordHash("hash")
                .role(Role.MANAGER)
                .active(true)
                .build();
        CustomUserDetails managerDetails = new CustomUserDetails(managerUser);

        String adminToken = jwtTokenProvider.generateAccessToken(adminDetails);
        String managerToken = jwtTokenProvider.generateAccessToken(managerDetails);

        assertThat(adminToken).isNotEqualTo(managerToken);

        assertThat(jwtTokenProvider.getEmailFromToken(adminToken)).isEqualTo("admin@test.com");
        assertThat(jwtTokenProvider.getEmailFromToken(managerToken)).isEqualTo("manager@test.com");
    }

    @Test
    void accessTokenAndRefreshTokenAreDifferent() {
        String accessToken = jwtTokenProvider.generateAccessToken(adminDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(adminDetails);

        assertThat(accessToken).isNotEqualTo(refreshToken);
    }
}
