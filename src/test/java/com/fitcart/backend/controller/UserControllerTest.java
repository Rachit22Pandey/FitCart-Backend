package com.fitcart.backend.controller;

import com.fitcart.backend.dto.UserResponse;
import com.fitcart.backend.model.User;
import com.fitcart.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "Rachit", "rachit@example.com", "pass123", "ADMIN", "9999999999", "2026-10-09 10:00:00");
    }

    @Test
    void testCreateUser() {
        when(userService.saveUser(sampleUser)).thenReturn(sampleUser);

        ResponseEntity<UserResponse> response = userController.createUser(sampleUser);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Rachit", response.getBody().getName());
        verify(userService, times(1)).saveUser(sampleUser);
    }

    @Test
    void testGetAllUsers() {
        when(userService.getAllUsers()).thenReturn(Collections.singletonList(sampleUser));

        ResponseEntity<List<UserResponse>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void testGetUserById() {
        UserResponse userResponse = UserResponse.fromUser(sampleUser);
        when(userService.getUserResponseById(1L)).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = userController.getUserById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Rachit", response.getBody().getName());
        verify(userService, times(1)).getUserResponseById(1L);
    }
}
