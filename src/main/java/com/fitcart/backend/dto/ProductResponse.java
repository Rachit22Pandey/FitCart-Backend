package com.fitcart.backend.dto;

import com.fitcart.backend.model.Product;

import java.math.BigDecimal;

public class ProductResponse {

    private Long productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerName;
    private Integer stockQuantity;

    public ProductResponse() {
    }

    public ProductResponse(Long productId, String productName, String description, BigDecimal price, Long categoryId, String categoryName, Long sellerId, String sellerName, Integer stockQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.stockQuantity = stockQuantity;
    }

    public static ProductResponse fromProduct(Product p) {
        if (p == null) return null;
        return new ProductResponse(
                p.getProductId(),
                p.getProductName(),
                p.getDescription(),
                p.getPrice(),
                p.getCategoryId(),
                p.getCategoryName(),
                p.getSellerId(),
                p.getSellerName(),
                p.getStockQuantity() != null ? p.getStockQuantity() : 0
        );
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}
