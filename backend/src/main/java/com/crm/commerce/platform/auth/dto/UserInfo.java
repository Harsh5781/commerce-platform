package com.crm.commerce.platform.auth.dto;

import com.crm.commerce.platform.auth.security.CustomUserDetails;
import com.crm.commerce.platform.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private String id;
    private String email;
    private String name;
    private String role;

    public static UserInfo from(CustomUserDetails userDetails) {
        return UserInfo.builder()
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .name(userDetails.getName())
                .role(userDetails.getRole())
                .build();
    }

    public static UserInfo from(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}
