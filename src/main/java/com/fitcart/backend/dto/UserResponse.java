package com.fitcart.backend.dto;

import com.fitcart.backend.model.User;

public class UserResponse {

    private Long userId;
    private String name;
    private String email;
    private String role;
    private String phone;
    private String createdAt;

    public UserResponse() {
    }

    public UserResponse(Long userId, String name, String email, String role, String phone, String createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    public static UserResponse fromUser(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getCreatedAt()
        );
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
