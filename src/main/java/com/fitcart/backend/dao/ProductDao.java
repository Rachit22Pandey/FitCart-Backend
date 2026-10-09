package com.fitcart.backend.dao;

import com.fitcart.backend.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductDao {

    Product save(Product product);

    List<Product> findAll();

    Optional<Product> findById(Long productId);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findBySellerId(Long sellerId);

    List<Product> search(String keyword);

    int update(Product product);

    int deleteById(Long productId);
}
