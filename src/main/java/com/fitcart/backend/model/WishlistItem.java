package com.fitcart.backend.model;

import java.math.BigDecimal;

public class WishlistItem {

    private Long wishlistId;
    private Long userId;
    private Long productId;

    // Joined Product details
    private String productName;
    private BigDecimal price;
    private String categoryName;

    public WishlistItem() {
    }

    public WishlistItem(Long wishlistId, Long userId, Long productId) {
        this.wishlistId = wishlistId;
        this.userId = userId;
        this.productId = productId;
    }

    public Long getWishlistId() {
        return wishlistId;
    }

    public void setWishlistId(Long wishlistId) {
        this.wishlistId = wishlistId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
