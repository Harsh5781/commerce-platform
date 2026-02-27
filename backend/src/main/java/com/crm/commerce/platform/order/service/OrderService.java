package com.crm.commerce.platform.order.service;

import com.crm.commerce.platform.audit.service.AuditService;
import com.crm.commerce.platform.common.exception.BadRequestException;
import com.crm.commerce.platform.common.exception.ResourceNotFoundException;
import com.crm.commerce.platform.common.service.SequenceGenerator;
import com.crm.commerce.platform.common.util.ValidationUtils;
import com.crm.commerce.platform.dashboard.service.DashboardService;
import com.crm.commerce.platform.order.dto.CreateOrderRequest;
import com.crm.commerce.platform.order.dto.OrderResponse;
import com.crm.commerce.platform.order.dto.UpdateOrderStatusRequest;
import com.crm.commerce.platform.order.model.*;
import com.crm.commerce.platform.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MongoTemplate mongoTemplate;
    private final DashboardService dashboardService;
    private final SequenceGenerator sequenceGenerator;
    private final AuditService auditService;

    public Page<OrderResponse> getOrders(String channel, String status, String search,
                                         LocalDateTime startDate, LocalDateTime endDate,
                                         Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(channel)) {
            criteriaList.add(Criteria.where("channel").is(channel.toUpperCase()));
        }
        if (StringUtils.hasText(status)) {
            try {
                OrderStatus.valueOf(status.toUpperCase());
                criteriaList.add(Criteria.where("status").is(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid order status: " + status);
            }
        }
        if (StringUtils.hasText(search)) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("orderNumber").regex(search, "i"),
                    Criteria.where("customer.name").regex(search, "i"),
                    Criteria.where("customer.email").regex(search, "i"),
                    Criteria.where("channelOrderRef").regex(search, "i")
            ));
        }
        if (startDate != null) {
            criteriaList.add(Criteria.where("placedAt").gte(startDate));
        }
        if (endDate != null) {
            criteriaList.add(Criteria.where("placedAt").lte(endDate));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Order.class);
        query.with(pageable);
        List<Order> orders = mongoTemplate.find(query, Order.class);

        Page<Order> page = new PageImpl<>(orders, pageable, total);
        return page.map(OrderResponse::from);
    }

    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrderById(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return OrderResponse.from(order);
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return OrderResponse.from(order);
    }

    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse createOrder(CreateOrderRequest request, String createdBy) {
        validateCreateRequest(request);

        List<OrderItem> items = request.getItems().stream()
                .map(dto -> OrderItem.builder()
                        .productName(dto.getProductName())
                        .sku(dto.getSku())
                        .quantity(dto.getQuantity())
                        .unitPrice(dto.getUnitPrice())
                        .totalPrice(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantity())))
                        .build())
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String channel = request.getChannel().toUpperCase();
        String orderNumber = generateOrderNumber(channel);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .channel(channel)
                .channelOrderRef(request.getChannelOrderRef())
                .customer(Customer.builder()
                        .name(request.getCustomer().getName())
                        .email(request.getCustomer().getEmail())
                        .phone(request.getCustomer().getPhone())
                        .build())
                .items(items)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingAddress(Address.builder()
                        .line1(request.getShippingAddress().getLine1())
                        .line2(request.getShippingAddress().getLine2())
                        .city(request.getShippingAddress().getCity())
                        .state(request.getShippingAddress().getState())
                        .pincode(request.getShippingAddress().getPincode())
                        .country(request.getShippingAddress().getCountry())
                        .build())
                .channelMetadata(request.getChannelMetadata())
                .timeline(new ArrayList<>(List.of(OrderTimeline.builder()
                        .status(OrderStatus.PENDING)
                        .changedBy(createdBy)
                        .notes("Order created")
                        .timestamp(LocalDateTime.now())
                        .build())))
                .placedAt(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);
        dashboardService.clearCache();
        auditService.logAction(null, createdBy, "CREATE_ORDER", "Order", order.getId(),
                java.util.Map.of("orderNumber", order.getOrderNumber(), "channel", channel));
        log.info("Order created: {} on channel {}", order.getOrderNumber(), channel);
        return OrderResponse.from(order);
    }

    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse updateOrderStatus(String id, UpdateOrderStatusRequest request, String changedBy) {
        ValidationUtils.validate()
                .requireNonBlank(request.getStatus(), "status")
                .execute();

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status: " + request.getStatus());
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        validateStatusTransition(order.getStatus(), newStatus);

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(newStatus);
        order.getTimeline().add(OrderTimeline.builder()
                .status(newStatus)
                .changedBy(changedBy)
                .notes(StringUtils.hasText(request.getNotes()) ? request.getNotes() : getDefaultNote(newStatus))
                .timestamp(LocalDateTime.now())
                .build());
        order = orderRepository.save(order);
        dashboardService.clearCache();
        auditService.logAction(null, changedBy, "UPDATE_ORDER_STATUS", "Order", order.getId(),
                java.util.Map.of("orderNumber", order.getOrderNumber(),
                        "previousStatus", previousStatus.name(), "newStatus", newStatus.name()));
        log.info("Order {} status updated to {} by {}", order.getOrderNumber(), newStatus, changedBy);
        return OrderResponse.from(order);
    }

    private void validateCreateRequest(CreateOrderRequest request) {
        ValidationUtils.validate()
                .requireNonBlank(request.getChannel(), "channel")
                .requireOneOf(request.getChannel(), "channel", "WEBSITE", "AMAZON", "BLINKIT")
                .requireNonNull(request.getCustomer(), "customer")
                .requireNonEmpty(request.getItems(), "items")
                .requireNonNull(request.getShippingAddress(), "shippingAddress")
                .execute();

        ValidationUtils.validate()
                .requireNonBlank(request.getCustomer().getName(), "customer.name")
                .requireNonBlank(request.getCustomer().getEmail(), "customer.email")
                .requireValidEmail(request.getCustomer().getEmail(), "customer.email")
                .requireNonBlank(request.getShippingAddress().getLine1(), "shippingAddress.line1")
                .requireNonBlank(request.getShippingAddress().getCity(), "shippingAddress.city")
                .requireNonBlank(request.getShippingAddress().getState(), "shippingAddress.state")
                .requireNonBlank(request.getShippingAddress().getPincode(), "shippingAddress.pincode")
                .execute();

        for (int i = 0; i < request.getItems().size(); i++) {
            CreateOrderRequest.OrderItemDto item = request.getItems().get(i);
            String prefix = "items[" + i + "].";
            ValidationUtils.validate()
                    .requireNonBlank(item.getProductName(), prefix + "productName")
                    .requireNonBlank(item.getSku(), prefix + "sku")
                    .requirePositiveInt(item.getQuantity(), prefix + "quantity")
                    .requirePositive(item.getUnitPrice(), prefix + "unitPrice")
                    .execute();
        }
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED || next == OrderStatus.RETURNED;
            case DELIVERED -> next == OrderStatus.RETURNED || next == OrderStatus.REFUNDED;
            case RETURNED -> next == OrderStatus.REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    String.format("Cannot transition from %s to %s", current, next));
        }
    }

    private String generateOrderNumber(String channel) {
        String prefix = channel.substring(0, 3);
        long seq = sequenceGenerator.nextValue("order_sequence");
        return String.format("ORD-%s-%05d", prefix, seq);
    }

    private String getDefaultNote(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Order received";
            case CONFIRMED -> "Order confirmed";
            case PROCESSING -> "Order is being prepared";
            case SHIPPED -> "Order shipped";
            case DELIVERED -> "Order delivered";
            case CANCELLED -> "Order cancelled";
            case RETURNED -> "Return initiated";
            case REFUNDED -> "Refund processed";
        };
    }
}
