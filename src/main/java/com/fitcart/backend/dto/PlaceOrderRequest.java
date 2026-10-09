package com.fitcart.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlaceOrderRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private List<OrderItemDto> items = new ArrayList<>();

    private Boolean useCart = false;

    @NotBlank(message = "Payment method is required (e.g. CARD, UPI, COD)")
    private String paymentMethod;

    private Long addressId;

    public PlaceOrderRequest() {
    }

    public PlaceOrderRequest(Long userId, List<OrderItemDto> items, Boolean useCart, String paymentMethod, Long addressId) {
        this.userId = userId;
        this.items = items;
        this.useCart = useCart;
        this.paymentMethod = paymentMethod;
        this.addressId = addressId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }

    public Boolean getUseCart() {
        return useCart != null && useCart;
    }

    public void setUseCart(Boolean useCart) {
        this.useCart = useCart;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }
}
