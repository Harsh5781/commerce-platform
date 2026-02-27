package com.crm.commerce.platform.order.repository;

import com.crm.commerce.platform.order.model.Order;
import com.crm.commerce.platform.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByChannel(String channel, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByChannelAndStatus(String channel, OrderStatus status, Pageable pageable);

    @Query("{'$or': [{'orderNumber': {'$regex': ?0, '$options': 'i'}}, {'customer.name': {'$regex': ?0, '$options': 'i'}}, {'customer.email': {'$regex': ?0, '$options': 'i'}}, {'channelOrderRef': {'$regex': ?0, '$options': 'i'}}]}")
    Page<Order> searchOrders(String keyword, Pageable pageable);

    List<Order> findByPlacedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByChannel(String channel);

    long countByStatus(OrderStatus status);

    long countByChannelAndStatus(String channel, OrderStatus status);

    @Query(value = "{'placedAt': {'$gte': ?0, '$lte': ?1}}", count = true)
    long countByPlacedAtBetween(LocalDateTime start, LocalDateTime end);
}
