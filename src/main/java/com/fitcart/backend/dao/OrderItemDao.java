package com.fitcart.backend.dao;

import com.fitcart.backend.model.OrderItem;

import java.util.List;

public interface OrderItemDao {

    OrderItem save(OrderItem item);

    List<OrderItem> findByOrderId(Long orderId);
}
