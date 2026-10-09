package com.fitcart.backend.service;

import com.fitcart.backend.dao.ProductDao;
import com.fitcart.backend.dao.WishlistDao;
import com.fitcart.backend.dto.WishlistRequest;
import com.fitcart.backend.model.Product;
import com.fitcart.backend.model.WishlistItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistDao wishlistDao;

    @Mock
    private ProductDao productDao;

    @InjectMocks
    private WishlistService wishlistService;

    private Product product;
    private WishlistItem wishlistItem;

    @BeforeEach
    void setUp() {
        product = new Product(1L, "Lifting Straps", "Heavy duty straps", new BigDecimal("15.00"), 1L, 2L);
        wishlistItem = new WishlistItem(50L, 10L, 1L);
    }

    @Test
    void testAddToWishlist_Success() {
        WishlistRequest req = new WishlistRequest(10L, 1L);
        when(productDao.findById(1L)).thenReturn(Optional.of(product));
        when(wishlistDao.findByUserIdAndProductId(10L, 1L)).thenReturn(Optional.empty());
        when(wishlistDao.save(any(WishlistItem.class))).thenReturn(wishlistItem);

        WishlistItem result = wishlistService.addToWishlist(req);

        assertNotNull(result);
        assertEquals(50L, result.getWishlistId());
        verify(wishlistDao).save(any(WishlistItem.class));
    }

    @Test
    void testGetWishlistByUserId() {
        when(wishlistDao.findByUserId(10L)).thenReturn(Collections.singletonList(wishlistItem));

        List<WishlistItem> list = wishlistService.getWishlistByUserId(10L);

        assertEquals(1, list.size());
        verify(wishlistDao).findByUserId(10L);
    }
}
