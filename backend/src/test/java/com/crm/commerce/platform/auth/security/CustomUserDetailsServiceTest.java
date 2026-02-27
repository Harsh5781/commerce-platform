package com.crm.commerce.platform.auth.security;

import com.crm.commerce.platform.user.enums.Role;
import com.crm.commerce.platform.user.model.User;
import com.crm.commerce.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_existingUser_returnsCustomUserDetails() {
        User user = User.builder()
                .id("u-1")
                .email("admin@test.com")
                .name("Admin")
                .passwordHash("$2a$12$hash")
                .role(Role.ADMIN)
                .active(true)
                .build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("admin@test.com");

        assertThat(details).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails custom = (CustomUserDetails) details;
        assertThat(custom.getId()).isEqualTo("u-1");
        assertThat(custom.getEmail()).isEqualTo("admin@test.com");
        assertThat(custom.getName()).isEqualTo("Admin");
        assertThat(custom.getRole()).isEqualTo("ADMIN");
        assertThat(custom.isEnabled()).isTrue();
        assertThat(custom.isAccountNonLocked()).isTrue();
        assertThat(custom.getAuthorities()).hasSize(1);
        assertThat(custom.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_nonExistentUser_throwsUsernameNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@test.com");
    }

    @Test
    void loadUserByUsername_inactiveUser_returnsLockedAccount() {
        User user = User.builder()
                .id("u-2")
                .email("inactive@test.com")
                .name("Inactive")
                .passwordHash("hash")
                .role(Role.VIEWER)
                .active(false)
                .build();
        when(userRepository.findByEmail("inactive@test.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("inactive@test.com");

        assertThat(details.isAccountNonLocked()).isFalse();
        assertThat(details.isEnabled()).isFalse();
    }
}
