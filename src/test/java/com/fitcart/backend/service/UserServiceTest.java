package com.fitcart.backend.service;

import com.fitcart.backend.dao.UserDao;
import com.fitcart.backend.dto.LoginRequest;
import com.fitcart.backend.dto.RegisterRequest;
import com.fitcart.backend.dto.UserResponse;
import com.fitcart.backend.exception.BadRequestException;
import com.fitcart.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "Alice", "alice@example.com", "encoded_pass", "BUYER", "1234567890", "2026-10-09 10:00:00");
    }

    @Test
    void testRegister_Success() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "secret123", "BUYER", "1234567890");
        when(userDao.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_pass");
        when(userDao.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.register(req);

        assertNotNull(response);
        assertEquals("Alice", response.getName());
        assertEquals("BUYER", response.getRole());
        verify(userDao).save(any(User.class));
    }

    @Test
    void testRegister_DuplicateEmail() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "secret123", "BUYER", "1234567890");
        when(userDao.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.register(req));
    }

    @Test
    void testLogin_Success() {
        LoginRequest req = new LoginRequest("alice@example.com", "secret123");
        when(userDao.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("secret123", "encoded_pass")).thenReturn(true);

        UserResponse response = userService.login(req);

        assertNotNull(response);
        assertEquals("Alice", response.getName());
    }

    @Test
    void testLogin_InvalidPassword() {
        LoginRequest req = new LoginRequest("alice@example.com", "wrongpass");
        when(userDao.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpass", "encoded_pass")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.login(req));
    }

    @Test
    void testGetAllUsers() {
        User user2 = new User(2L, "Bob", "bob@example.com", "pass456", "SELLER", "0987654321", "2026-10-09 10:05:00");
        when(userDao.findAll()).thenReturn(Arrays.asList(sampleUser, user2));

        List<User> users = userService.getAllUsers();

        assertEquals(2, users.size());
        verify(userDao, times(1)).findAll();
    }
}
