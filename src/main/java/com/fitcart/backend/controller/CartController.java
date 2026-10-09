
package com.fitcart.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fitcart.backend.dto.CartItemRequest;
import com.fitcart.backend.model.CartItem;
import com.fitcart.backend.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    @PreAuthorize("@resourceAuthorizationService.canAccessUser(#p0.userId, authentication)")
    public ResponseEntity<CartItem> addToCart(
            @Valid @RequestBody CartItemRequest request) {
        CartItem item = cartService.addToCart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@resourceAuthorizationService.canAccessUser(#p0, authentication)")
    public ResponseEntity<List<CartItem>> getCart(
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PutMapping("/{cartId}")
    @PreAuthorize("@resourceAuthorizationService.canManageCartItem(#p0, authentication)")
    public ResponseEntity<CartItem> updateQuantity(
            @PathVariable("cartId") Long cartId,
            @RequestParam("quantity") Integer quantity) {
        return ResponseEntity.ok(
                cartService.updateQuantity(cartId, quantity));
    }

    @DeleteMapping("/{cartId}")
    @PreAuthorize("@resourceAuthorizationService.canManageCartItem(#p0, authentication)")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable("cartId") Long cartId) {
        cartService.removeFromCart(cartId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    @PreAuthorize("@resourceAuthorizationService.canAccessUser(#p0, authentication)")
    public ResponseEntity<Void> clearCart(
            @PathVariable("userId") Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
