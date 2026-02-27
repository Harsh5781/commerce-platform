package com.crm.commerce.platform.channel.controller;

import com.crm.commerce.platform.channel.dto.ChannelResponse;
import com.crm.commerce.platform.channel.service.ChannelService;
import com.crm.commerce.platform.common.dto.ApiResponse;
import com.crm.commerce.platform.order.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
@Tag(name = "Channels", description = "Sales channel management and sync")
public class ChannelController {

    private final ChannelService channelService;

    @GetMapping
    @Operation(summary = "List all sales channels with availability status")
    public ResponseEntity<ApiResponse<List<ChannelResponse>>> getAllChannels() {
        List<ChannelResponse> channels = channelService.getAllChannels();
        return ResponseEntity.ok(ApiResponse.success(channels));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get channel details by code")
    public ResponseEntity<ApiResponse<ChannelResponse>> getChannel(@PathVariable String code) {
        ChannelResponse channel = channelService.getChannelByCode(code);
        return ResponseEntity.ok(ApiResponse.success(channel));
    }

    @PostMapping("/{code}/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Sync orders from a channel (Admin/Manager)")
    public ResponseEntity<ApiResponse<Integer>> syncChannel(@PathVariable String code) {
        List<Order> synced = channelService.syncChannel(code);
        return ResponseEntity.ok(ApiResponse.success("Synced " + synced.size() + " orders from " + code, synced.size()));
    }
}
