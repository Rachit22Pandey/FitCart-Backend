package com.fitcart.backend.dao;

import com.fitcart.backend.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryDao {

    Category save(Category category);

    List<Category> findAll();

    Optional<Category> findById(Long categoryId);

    Optional<Category> findByName(String categoryName);

    boolean existsByName(String categoryName);

    int update(Category category);

    int deleteById(Long categoryId);
}
