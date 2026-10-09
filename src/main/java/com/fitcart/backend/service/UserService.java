
package com.fitcart.backend.service;

import com.fitcart.backend.dao.UserDao;
import com.fitcart.backend.dto.LoginRequest;
import com.fitcart.backend.dto.RegisterRequest;
import com.fitcart.backend.dto.UserResponse;
import com.fitcart.backend.exception.BadRequestException;
import com.fitcart.backend.exception.ResourceNotFoundException;
import com.fitcart.backend.model.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserDao userDao,
            PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    // Public registration: ADMIN accounts cannot be created here.
    public UserResponse register(RegisterRequest request) {

        if (request.getEmail() == null
                || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }

        if (request.getName() == null
                || request.getName().isBlank()) {
            throw new BadRequestException("Name is required");
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userDao.existsByEmail(email)) {
            throw new BadRequestException(
                "An account with this email already exists"
            );
        }

        String role = request.getRole() == null
                || request.getRole().isBlank()
                ? "BUYER"
                : request.getRole().trim().toUpperCase(Locale.ROOT);

        if (!role.equals("BUYER") && !role.equals("SELLER")) {
            throw new BadRequestException(
                "Public registration supports BUYER or SELLER only"
            );
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(
            passwordEncoder.encode(request.getPassword())
        );
        user.setRole(role);
        user.setPhone(request.getPhone());

        User saved = userDao.save(user);

        return UserResponse.fromUser(saved);
    }

    // API login
    public UserResponse login(LoginRequest request) {

        if (request.getEmail() == null
                || request.getPassword() == null) {
            throw new BadRequestException(
                "Invalid email or password"
            );
        }

        User user = userDao.findByEmail(
                request.getEmail().trim().toLowerCase(Locale.ROOT)
        ).orElseThrow(() ->
                new BadRequestException("Invalid email or password")
        );

        String storedPassword = user.getPassword();

        if (storedPassword == null
                || !passwordEncoder.matches(
                    request.getPassword(), storedPassword)) {
            throw new BadRequestException(
                "Invalid email or password"
            );
        }

        return UserResponse.fromUser(user);
    }

    // Save a user while avoiding accidental double-hashing.
    // Authorization must be enforced by the controller/service caller.
    public User saveUser(User user) {

        if (user.getPassword() != null
                && !user.getPassword().isBlank()
                && !user.getPassword().startsWith("$2")) {
            user.setPassword(
                passwordEncoder.encode(user.getPassword())
            );
        }

        return userDao.save(user);
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    public Optional<User> getUserById(Long userId) {
        return userDao.findById(userId);
    }

    public UserResponse getUserResponseById(Long userId) {
        return userDao.findById(userId)
                .map(UserResponse::fromUser)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "User not found with id: " + userId
                    )
                );
    }

    public Optional<User> getUserByEmail(String email) {
        return userDao.findByEmail(
            email.trim().toLowerCase(Locale.ROOT)
        );
    }
}
