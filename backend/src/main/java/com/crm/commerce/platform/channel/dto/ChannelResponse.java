package com.crm.commerce.platform.channel.dto;

import com.crm.commerce.platform.channel.model.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResponse {
    private String id;
    private String name;
    private String code;
    private String status;
    private String description;
    private String logoUrl;
    private boolean available;
    private long orderCount;
    private LocalDateTime createdAt;

    public static ChannelResponse from(Channel channel, boolean available, long orderCount) {
        return ChannelResponse.builder()
                .id(channel.getId())
                .name(channel.getName())
                .code(channel.getCode().name())
                .status(channel.getStatus().name())
                .description(channel.getDescription())
                .logoUrl(channel.getLogoUrl())
                .available(available)
                .orderCount(orderCount)
                .createdAt(channel.getCreatedAt())
                .build();
    }
}
