
package com.fitcart.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fitcart.backend.dto.WishlistRequest;
import com.fitcart.backend.model.WishlistItem;
import com.fitcart.backend.service.WishlistService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping
    @PreAuthorize("@resourceAuthorizationService.canAccessUser(#p0.userId, authentication)")
    public ResponseEntity<WishlistItem> addToWishlist(
            @Valid @RequestBody WishlistRequest request) {
        WishlistItem item = wishlistService.addToWishlist(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@resourceAuthorizationService.canAccessUser(#p0, authentication)")
    public ResponseEntity<List<WishlistItem>> getWishlist(
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(
                wishlistService.getWishlistByUserId(userId));
    }

    @DeleteMapping("/{wishlistId}")
    @PreAuthorize("@resourceAuthorizationService.canManageWishlistItem(#p0, authentication)")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable("wishlistId") Long wishlistId) {
        wishlistService.removeFromWishlist(wishlistId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}/product/{productId}")
    @PreAuthorize("@resourceAuthorizationService.canAccessUser(#p0, authentication)")
    public ResponseEntity<Void> removeByProduct(
            @PathVariable("userId") Long userId,
            @PathVariable("productId") Long productId) {
        wishlistService.removeByProduct(userId, productId);
        return ResponseEntity.noContent().build();
    }
}
