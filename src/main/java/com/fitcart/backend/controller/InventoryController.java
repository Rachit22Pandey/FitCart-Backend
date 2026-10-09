
package com.fitcart.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fitcart.backend.dto.InventoryRequest;
import com.fitcart.backend.model.Inventory;
import com.fitcart.backend.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // Product stock is visible for catalog browsing.
    @GetMapping("/product/{productId}")
    public ResponseEntity<Inventory> getInventory(
            @PathVariable("productId") Long productId) {

        return ResponseEntity.ok(
                inventoryService.getInventoryByProductId(productId));
    }

    // Only the product's seller or an admin can change its stock.
    @PutMapping("/product/{productId}")
    @PreAuthorize(
        "@productAuthorizationService.canManageProduct(#productId, authentication)"
    )
    public ResponseEntity<Inventory> updateStock(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody InventoryRequest request) {

        Inventory updated = inventoryService.updateStock(
                productId, request.getStockQuantity());

        return ResponseEntity.ok(updated);
    }
}
