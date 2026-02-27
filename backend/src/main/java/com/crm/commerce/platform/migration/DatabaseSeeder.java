package com.crm.commerce.platform.migration;

import com.crm.commerce.platform.channel.model.Channel;
import com.crm.commerce.platform.channel.model.ChannelCode;
import com.crm.commerce.platform.channel.model.ChannelStatus;
import com.crm.commerce.platform.channel.repository.ChannelRepository;
import com.crm.commerce.platform.common.service.SequenceGenerator;
import com.crm.commerce.platform.order.model.*;
import com.crm.commerce.platform.order.repository.OrderRepository;
import com.crm.commerce.platform.user.enums.Role;
import com.crm.commerce.platform.user.model.User;
import com.crm.commerce.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final SequenceGenerator sequenceGenerator;

    @Override
    public void run(ApplicationArguments args) {
        seedChannels();
        seedUsers();
        seedOrders();
    }

    private void seedChannels() {
        if (channelRepository.count() > 0) {
            log.info("Channels already seeded, skipping");
            return;
        }

        log.info("Seeding channels...");
        List<Channel> channels = List.of(
                Channel.builder()
                        .name("Organic Website")
                        .code(ChannelCode.WEBSITE)
                        .status(ChannelStatus.ACTIVE)
                        .description("Direct-to-consumer organic website")
                        .logoUrl("/images/channels/website.png")
                        .apiConfig(Map.of("baseUrl", "https://api.organicstore.com", "version", "v2"))
                        .build(),
                Channel.builder()
                        .name("Amazon")
                        .code(ChannelCode.AMAZON)
                        .status(ChannelStatus.ACTIVE)
                        .description("Amazon marketplace integration")
                        .logoUrl("/images/channels/amazon.png")
                        .apiConfig(Map.of("baseUrl", "https://sellingpartnerapi.amazon.in", "region", "ap-south-1"))
                        .build(),
                Channel.builder()
                        .name("Blinkit")
                        .code(ChannelCode.BLINKIT)
                        .status(ChannelStatus.ACTIVE)
                        .description("Blinkit quick-commerce integration")
                        .logoUrl("/images/channels/blinkit.png")
                        .apiConfig(Map.of("baseUrl", "https://api.blinkit.com", "deliveryZone", "NCR"))
                        .build()
        );

        channelRepository.saveAll(channels);
        log.info("Seeded {} channels", channels.size());
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already seeded, skipping");
            return;
        }

        log.info("Seeding users...");
        List<User> users = List.of(
                User.builder()
                        .email("admin@commerce.com")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .name("Admin User")
                        .role(Role.ADMIN)
                        .active(true)
                        .build(),
                User.builder()
                        .email("manager@commerce.com")
                        .passwordHash(passwordEncoder.encode("manager123"))
                        .name("Manager User")
                        .role(Role.MANAGER)
                        .active(true)
                        .build(),
                User.builder()
                        .email("viewer@commerce.com")
                        .passwordHash(passwordEncoder.encode("viewer123"))
                        .name("Viewer User")
                        .role(Role.VIEWER)
                        .active(true)
                        .build()
        );

        userRepository.saveAll(users);
        log.info("Seeded {} users", users.size());
    }

    private void seedOrders() {
        if (orderRepository.count() > 0) {
            log.info("Orders already seeded, skipping");
            return;
        }

        log.info("Seeding orders...");
        List<com.crm.commerce.platform.order.model.Order> orders = new ArrayList<>();
        String[] channels = {"WEBSITE", "AMAZON", "BLINKIT"};
        OrderStatus[] statuses = OrderStatus.values();

        for (int i = 1; i <= 150; i++) {
            String channel = channels[i % 3];
            OrderStatus status = statuses[ThreadLocalRandom.current().nextInt(statuses.length)];
            LocalDateTime placedAt = LocalDateTime.now()
                    .minusDays(ThreadLocalRandom.current().nextInt(1, 60))
                    .minusHours(ThreadLocalRandom.current().nextInt(0, 24));

            List<OrderItem> items = generateItems(channel);
            BigDecimal total = items.stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            com.crm.commerce.platform.order.model.Order order =
                    com.crm.commerce.platform.order.model.Order.builder()
                            .orderNumber(String.format("ORD-%s-%05d", channel.substring(0, 3), sequenceGenerator.nextValue("order_sequence")))
                            .channel(channel)
                            .channelOrderRef(generateChannelRef(channel, i))
                            .customer(generateCustomer(i))
                            .items(items)
                            .status(status)
                            .totalAmount(total)
                            .shippingAddress(generateAddress(i))
                            .channelMetadata(generateChannelMetadata(channel))
                            .timeline(generateTimeline(status, placedAt))
                            .placedAt(placedAt)
                            .build();

            orders.add(order);
        }

        orderRepository.saveAll(orders);
        log.info("Seeded {} orders", orders.size());
    }

    private List<OrderItem> generateItems(String channel) {
        String[][] products = {
                {"Organic Honey", "HON-001", "450.00"},
                {"Cold Pressed Coconut Oil", "OIL-002", "350.00"},
                {"Quinoa Seeds", "QUI-003", "550.00"},
                {"Chia Seeds", "CHI-004", "399.00"},
                {"Organic Turmeric Powder", "TUR-005", "280.00"},
                {"Raw Almond Butter", "ALM-006", "620.00"},
                {"Organic Green Tea", "TEA-007", "320.00"},
                {"Flax Seeds", "FLX-008", "250.00"},
                {"Apple Cider Vinegar", "ACV-009", "410.00"},
                {"Organic Ghee", "GHE-010", "580.00"}
        };

        int itemCount = ThreadLocalRandom.current().nextInt(1, 4);
        List<OrderItem> items = new ArrayList<>();
        Set<Integer> used = new HashSet<>();

        for (int j = 0; j < itemCount; j++) {
            int idx;
            do { idx = ThreadLocalRandom.current().nextInt(products.length); } while (used.contains(idx));
            used.add(idx);

            int qty = ThreadLocalRandom.current().nextInt(1, 5);
            BigDecimal price = new BigDecimal(products[idx][2]);

            items.add(OrderItem.builder()
                    .productName(products[idx][0])
                    .sku(products[idx][1])
                    .quantity(qty)
                    .unitPrice(price)
                    .totalPrice(price.multiply(BigDecimal.valueOf(qty)))
                    .build());
        }
        return items;
    }

    private Customer generateCustomer(int index) {
        String[] firstNames = {"Aarav", "Priya", "Rohit", "Sneha", "Vikram", "Anita", "Karan", "Meera", "Arjun", "Divya"};
        String[] lastNames = {"Sharma", "Patel", "Gupta", "Singh", "Kumar", "Reddy", "Nair", "Joshi", "Das", "Mehta"};
        String first = firstNames[index % firstNames.length];
        String last = lastNames[(index * 3) % lastNames.length];

        return Customer.builder()
                .name(first + " " + last)
                .email(first.toLowerCase() + "." + last.toLowerCase() + "@email.com")
                .phone("+91-" + (9000000000L + ThreadLocalRandom.current().nextInt(999999999)))
                .build();
    }

    private Address generateAddress(int index) {
        String[][] cities = {
                {"Bangalore", "Karnataka", "560001"},
                {"Mumbai", "Maharashtra", "400001"},
                {"Delhi", "Delhi", "110001"},
                {"Hyderabad", "Telangana", "500001"},
                {"Chennai", "Tamil Nadu", "600001"},
                {"Pune", "Maharashtra", "411001"},
                {"Kolkata", "West Bengal", "700001"},
                {"Ahmedabad", "Gujarat", "380001"}
        };
        String[] city = cities[index % cities.length];

        return Address.builder()
                .line1((100 + index) + " MG Road")
                .city(city[0])
                .state(city[1])
                .pincode(city[2])
                .country("India")
                .build();
    }

    private String generateChannelRef(String channel, int index) {
        return switch (channel) {
            case "AMAZON" -> "402-" + (1000000 + index) + "-" + (8000000 + index);
            case "BLINKIT" -> "BLK-" + (200000 + index);
            default -> "WEB-" + (300000 + index);
        };
    }

    private Map<String, Object> generateChannelMetadata(String channel) {
        return switch (channel) {
            case "AMAZON" -> Map.of(
                    "fulfillmentCenter", "BLR-3",
                    "primeOrder", ThreadLocalRandom.current().nextBoolean(),
                    "sellerId", "A1B2C3D4E5"
            );
            case "BLINKIT" -> Map.of(
                    "deliverySlot", "10min",
                    "darkStoreId", "DS-" + ThreadLocalRandom.current().nextInt(100, 999),
                    "deliveryPartnerId", "DP-" + ThreadLocalRandom.current().nextInt(1000, 9999)
            );
            default -> Map.of(
                    "source", "organic",
                    "utmCampaign", "summer_sale",
                    "couponApplied", ThreadLocalRandom.current().nextBoolean()
            );
        };
    }

    private List<OrderTimeline> generateTimeline(OrderStatus currentStatus, LocalDateTime placedAt) {
        List<OrderTimeline> timeline = new ArrayList<>();
        OrderStatus[] progression = {
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
                OrderStatus.SHIPPED, OrderStatus.DELIVERED
        };

        LocalDateTime time = placedAt;
        for (OrderStatus step : progression) {
            timeline.add(OrderTimeline.builder()
                    .status(step)
                    .changedBy("system")
                    .notes(getTimelineNote(step))
                    .timestamp(time)
                    .build());

            if (step == currentStatus) break;
            time = time.plusHours(ThreadLocalRandom.current().nextInt(2, 48));
        }

        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.RETURNED
                || currentStatus == OrderStatus.REFUNDED) {
            timeline.add(OrderTimeline.builder()
                    .status(currentStatus)
                    .changedBy("system")
                    .notes(getTimelineNote(currentStatus))
                    .timestamp(time.plusHours(ThreadLocalRandom.current().nextInt(1, 24)))
                    .build());
        }

        return timeline;
    }

    private String getTimelineNote(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Order received";
            case CONFIRMED -> "Order confirmed and payment verified";
            case PROCESSING -> "Order is being prepared";
            case SHIPPED -> "Order shipped via courier";
            case DELIVERED -> "Order delivered successfully";
            case CANCELLED -> "Order cancelled by customer";
            case RETURNED -> "Return initiated by customer";
            case REFUNDED -> "Refund processed";
        };
    }
}
