package com.fitcart.backend.service;

import com.fitcart.backend.dao.ProductDao;
import com.fitcart.backend.dao.WishlistDao;
import com.fitcart.backend.dto.WishlistRequest;
import com.fitcart.backend.exception.ResourceNotFoundException;
import com.fitcart.backend.model.Product;
import com.fitcart.backend.model.WishlistItem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WishlistService {

    private final WishlistDao wishlistDao;
    private final ProductDao productDao;

    public WishlistService(WishlistDao wishlistDao, ProductDao productDao) {
        this.wishlistDao = wishlistDao;
        this.productDao = productDao;
    }

    public WishlistItem addToWishlist(WishlistRequest request) {
        Product product = productDao.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Optional<WishlistItem> existing = wishlistDao.findByUserIdAndProductId(request.getUserId(), request.getProductId());
        if (existing.isPresent()) {
            return existing.get();
        }

        WishlistItem item = new WishlistItem(null, request.getUserId(), request.getProductId());
        item.setProductName(product.getProductName());
        item.setPrice(product.getPrice());
        item.setCategoryName(product.getCategoryName());
        return wishlistDao.save(item);
    }

    public List<WishlistItem> getWishlistByUserId(Long userId) {
        return wishlistDao.findByUserId(userId);
    }

    public void removeFromWishlist(Long wishlistId) {
        wishlistDao.deleteById(wishlistId);
    }

    public void removeByProduct(Long userId, Long productId) {
        wishlistDao.deleteByUserIdAndProductId(userId, productId);
    }
}
