package com.crm.commerce.platform.auth.service;

import com.crm.commerce.platform.auth.dto.*;
import com.crm.commerce.platform.auth.jwt.JwtTokenProvider;
import com.crm.commerce.platform.auth.security.CustomUserDetails;
import com.crm.commerce.platform.common.exception.DuplicateResourceException;
import com.crm.commerce.platform.common.util.ValidationUtils;
import com.crm.commerce.platform.config.AppProperties;
import com.crm.commerce.platform.user.enums.Role;
import com.crm.commerce.platform.user.model.User;
import com.crm.commerce.platform.user.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
                       UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AppProperties appProperties, MeterRegistry meterRegistry) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
        this.loginSuccessCounter = Counter.builder("auth.login.success.total")
                .description("Successful login attempts").register(meterRegistry);
        this.loginFailureCounter = Counter.builder("auth.login.failure.total")
                .description("Failed login attempts").register(meterRegistry);
    }

    public LoginResponse login(LoginRequest request) {
        ValidationUtils.validate()
                .requireNonBlank(request.getEmail(), "email")
                .requireNonBlank(request.getPassword(), "password")
                .execute();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            loginFailureCounter.increment();
            throw e;
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        loginSuccessCounter.increment();
        log.info("User logged in: {}", userDetails.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getExpirationMs())
                .user(UserInfo.from(userDetails))
                .build();
    }

    public UserInfo register(RegisterRequest request) {
        ValidationUtils.validate()
                .requireNonBlank(request.getEmail(), "email")
                .requireValidEmail(request.getEmail(), "email")
                .requireNonBlank(request.getPassword(), "password")
                .requireMinLength(request.getPassword(), 6, "password")
                .requireNonBlank(request.getName(), "name")
                .execute();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role role = Role.VIEWER;
        if (StringUtils.hasText(request.getRole())) {
            try {
                role = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                role = Role.VIEWER;
            }
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(role)
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {} with role {}", user.getEmail(), user.getRole());

        return UserInfo.from(user);
    }

    public LoginResponse refreshToken(TokenRefreshRequest request) {
        ValidationUtils.validate()
                .requireNonBlank(request.getRefreshToken(), "refreshToken")
                .execute();

        jwtTokenProvider.validateTokenOrThrow(request.getRefreshToken());

        String email = jwtTokenProvider.getEmailFromToken(request.getRefreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.crm.commerce.platform.common.exception.UnauthorizedException("User not found"));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getExpirationMs())
                .user(UserInfo.from(userDetails))
                .build();
    }

    public UserInfo getCurrentUser(CustomUserDetails userDetails) {
        return UserInfo.from(userDetails);
    }
}
