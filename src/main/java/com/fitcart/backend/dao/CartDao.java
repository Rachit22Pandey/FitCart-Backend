package com.fitcart.backend.dao;

import com.fitcart.backend.model.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartDao {

    CartItem save(CartItem item);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    Optional<CartItem> findById(Long cartId);

    List<CartItem> findByUserId(Long userId);

    int updateQuantity(Long cartId, Integer quantity);

    int deleteById(Long cartId);

    int deleteByUserId(Long userId);
}
