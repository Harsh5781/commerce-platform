package com.crm.commerce.platform.order.controller;

import com.crm.commerce.platform.auth.security.CustomUserDetails;
import com.crm.commerce.platform.common.dto.ApiResponse;
import com.crm.commerce.platform.common.dto.PageResponse;
import com.crm.commerce.platform.order.dto.CreateOrderRequest;
import com.crm.commerce.platform.order.dto.OrderResponse;
import com.crm.commerce.platform.order.dto.UpdateOrderStatusRequest;
import com.crm.commerce.platform.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List orders with filtering, sorting, search, and pagination")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "placedAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Sort.Direction sortDir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(sortDir, sort));

        LocalDateTime start = parseDate(startDate);
        LocalDateTime end = parseDate(endDate);

        Page<OrderResponse> orders = orderService.getOrders(channel, status, search, start, end, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(orders)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponse order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new order (Admin/Manager)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderResponse order = orderService.createOrder(request, userDetails.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update order status (Admin/Manager)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable String id,
            @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderResponse order = orderService.updateOrderStatus(id, request, userDetails.getName());
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }

    private LocalDateTime parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(dateStr + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }
}
