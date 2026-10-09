
package com.fitcart.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fitcart.backend.dto.OrderStatusUpdateRequest;
import com.fitcart.backend.dto.PlaceOrderRequest;
import com.fitcart.backend.model.Order;
import com.fitcart.backend.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("@resourceAuthorizationService.canAccessUser(#p0.userId, authentication)")
    public ResponseEntity<Order> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        Order placedOrder = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(placedOrder);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@resourceAuthorizationService.canReadOrder(#p0, authentication)")
    public ResponseEntity<Order> getOrderById(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@resourceAuthorizationService.canAccessUser(#p0, authentication)")
    public ResponseEntity<List<Order>> getUserOrders(
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @GetMapping("/seller/{sellerId}")
    @PreAuthorize("@resourceAuthorizationService.canAccessSellerOrders(#p0, authentication)")
    public ResponseEntity<List<Order>> getSellerOrders(
            @PathVariable("sellerId") Long sellerId) {
        return ResponseEntity.ok(orderService.getSellerOrders(sellerId));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@resourceAuthorizationService.canManageOrderStatus(#p0, authentication)")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(
                orderService.updateOrderStatus(id, request.getStatus()));
    }
}
