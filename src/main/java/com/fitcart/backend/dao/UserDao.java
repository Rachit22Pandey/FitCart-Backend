package com.fitcart.backend.dao;

import com.fitcart.backend.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    User save(User user);

    List<User> findAll();

    Optional<User> findById(Long userId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    int update(User user);

    int deleteById(Long userId);
}
