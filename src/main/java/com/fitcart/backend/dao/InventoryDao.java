package com.fitcart.backend.dao;

import com.fitcart.backend.model.Inventory;

import java.util.Optional;

public interface InventoryDao {

    Inventory save(Inventory inventory);

    Optional<Inventory> findByProductId(Long productId);

    int updateStock(Long productId, Integer stockQuantity);

    int decreaseStock(Long productId, Integer quantity);

    int increaseStock(Long productId, Integer quantity);

    int deleteByProductId(Long productId);
}
