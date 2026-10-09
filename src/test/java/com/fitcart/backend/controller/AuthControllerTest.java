package com.fitcart.backend.controller;

import com.fitcart.backend.dto.AuthResponse;
import com.fitcart.backend.dto.LoginRequest;
import com.fitcart.backend.dto.RegisterRequest;
import com.fitcart.backend.dto.UserResponse;
import com.fitcart.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private UserResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new UserResponse(1L, "Alice", "alice@example.com", "BUYER", "1234567890", "2026-10-09 10:00:00");
    }

    @Test
    void testRegister() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "secret123", "BUYER", "1234567890");
        when(userService.register(req)).thenReturn(sampleResponse);

        ResponseEntity<AuthResponse> response = authController.register(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Registration successful", response.getBody().getMessage());
        assertEquals("Alice", response.getBody().getUser().getName());
        verify(userService, times(1)).register(req);
    }

    @Test
    void testLogin() {
        LoginRequest req = new LoginRequest("alice@example.com", "secret123");
        when(userService.login(req)).thenReturn(sampleResponse);

        ResponseEntity<AuthResponse> response = authController.login(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Login successful", response.getBody().getMessage());
        assertEquals("Alice", response.getBody().getUser().getName());
        verify(userService, times(1)).login(req);
    }
}
