package com.fitcart.backend.dao;

import com.fitcart.backend.model.WishlistItem;

import java.util.List;
import java.util.Optional;

public interface WishlistDao {

    WishlistItem save(WishlistItem item);

    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    List<WishlistItem> findByUserId(Long userId);

    int deleteById(Long wishlistId);

    int deleteByUserIdAndProductId(Long userId, Long productId);
}
