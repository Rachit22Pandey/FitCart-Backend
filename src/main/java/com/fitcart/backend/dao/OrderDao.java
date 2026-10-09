package com.fitcart.backend.dao;

import com.fitcart.backend.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderDao {

    Order save(Order order);

    Optional<Order> findById(Long orderId);

    List<Order> findByUserId(Long userId);

    List<Order> findBySellerId(Long sellerId);

    List<Order> findAll();

    int updateStatus(Long orderId, String status);
}
