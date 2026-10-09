package com.fitcart.backend.service;

import com.fitcart.backend.dao.InventoryDao;
import com.fitcart.backend.exception.ResourceNotFoundException;
import com.fitcart.backend.model.Inventory;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final InventoryDao inventoryDao;

    public InventoryService(InventoryDao inventoryDao) {
        this.inventoryDao = inventoryDao;
    }

    public Inventory getInventoryByProductId(Long productId) {
        return inventoryDao.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product id: " + productId));
    }

    public Inventory updateStock(Long productId, Integer quantity) {
        inventoryDao.updateStock(productId, quantity);
        return getInventoryByProductId(productId);
    }

    public boolean decreaseStock(Long productId, Integer quantity) {
        return inventoryDao.decreaseStock(productId, quantity) > 0;
    }

    public void increaseStock(Long productId, Integer quantity) {
        inventoryDao.increaseStock(productId, quantity);
    }
}
