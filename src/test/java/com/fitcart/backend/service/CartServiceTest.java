package com.fitcart.backend.service;

import com.fitcart.backend.dao.CartDao;
import com.fitcart.backend.dao.InventoryDao;
import com.fitcart.backend.dao.ProductDao;
import com.fitcart.backend.dto.CartItemRequest;
import com.fitcart.backend.exception.BadRequestException;
import com.fitcart.backend.model.CartItem;
import com.fitcart.backend.model.Inventory;
import com.fitcart.backend.model.Product;
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
class CartServiceTest {

    @Mock
    private CartDao cartDao;

    @Mock
    private ProductDao productDao;

    @Mock
    private InventoryDao inventoryDao;

    @InjectMocks
    private CartService cartService;

    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = new Product(1L, "Creatine Monohydrate", "Pure Micronized", new BigDecimal("24.99"), 1L, 2L);
        inventory = new Inventory(1L, 1L, 20);
    }

    @Test
    void testAddToCart_NewItem_Success() {
        CartItemRequest req = new CartItemRequest(10L, 1L, 2);
        when(productDao.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryDao.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(cartDao.findByUserIdAndProductId(10L, 1L)).thenReturn(Optional.empty());

        CartItem saved = new CartItem(100L, 10L, 1L, 2);
        when(cartDao.save(any(CartItem.class))).thenReturn(saved);

        CartItem result = cartService.addToCart(req);

        assertNotNull(result);
        assertEquals(2, result.getQuantity());
        verify(cartDao).save(any(CartItem.class));
    }

    @Test
    void testAddToCart_ExistingItem_IncrementsQuantity() {
        CartItemRequest req = new CartItemRequest(10L, 1L, 3);
        CartItem existing = new CartItem(100L, 10L, 1L, 2);

        when(productDao.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryDao.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(cartDao.findByUserIdAndProductId(10L, 1L)).thenReturn(Optional.of(existing));

        CartItem result = cartService.addToCart(req);

        assertNotNull(result);
        assertEquals(5, result.getQuantity());
        verify(cartDao).updateQuantity(100L, 5);
    }

    @Test
    void testAddToCart_ExceedsStock() {
        CartItemRequest req = new CartItemRequest(10L, 1L, 25);
        when(productDao.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryDao.findByProductId(1L)).thenReturn(Optional.of(inventory));

        assertThrows(BadRequestException.class, () -> cartService.addToCart(req));
    }

    @Test
    void testGetCartByUserId() {
        CartItem item = new CartItem(100L, 10L, 1L, 2);
        when(cartDao.findByUserId(10L)).thenReturn(Collections.singletonList(item));

        List<CartItem> cart = cartService.getCartByUserId(10L);

        assertEquals(1, cart.size());
        verify(cartDao).findByUserId(10L);
    }
}
