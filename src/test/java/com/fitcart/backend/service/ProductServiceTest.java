package com.fitcart.backend.service;

import com.fitcart.backend.dao.CategoryDao;
import com.fitcart.backend.dao.InventoryDao;
import com.fitcart.backend.dao.ProductDao;
import com.fitcart.backend.dao.UserDao;
import com.fitcart.backend.dto.ProductRequest;
import com.fitcart.backend.dto.ProductResponse;
import com.fitcart.backend.exception.BadRequestException;
import com.fitcart.backend.exception.ResourceNotFoundException;
import com.fitcart.backend.model.Category;
import com.fitcart.backend.model.Inventory;
import com.fitcart.backend.model.Product;
import com.fitcart.backend.model.User;
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
class ProductServiceTest {

    @Mock
    private ProductDao productDao;

    @Mock
    private CategoryDao categoryDao;

    @Mock
    private UserDao userDao;

    @Mock
    private InventoryDao inventoryDao;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private User seller;
    private Product product;

    @BeforeEach
    void setUp() {
        category = new Category(1L, "Supplements");
        seller = new User(2L, "FitnessHub", "seller@hub.com", "pass", "SELLER", "1234567890", "2026-10-09");
        product = new Product(10L, "Whey Gold Standard", "100% Whey Isolate", new BigDecimal("49.99"), 1L, 2L);
        product.setCategoryName("Supplements");
        product.setSellerName("FitnessHub");
        product.setStockQuantity(50);
    }

    @Test
    void testCreateProduct_Success() {
        ProductRequest req = new ProductRequest("Whey Gold Standard", "100% Whey Isolate", new BigDecimal("49.99"), 1L, 2L, 50);

        when(categoryDao.findById(1L)).thenReturn(Optional.of(category));
        when(userDao.findById(2L)).thenReturn(Optional.of(seller));
        when(productDao.save(any(Product.class))).thenReturn(product);
        when(inventoryDao.save(any(Inventory.class))).thenReturn(new Inventory(1L, 10L, 50));

        ProductResponse response = productService.createProduct(req);

        assertNotNull(response);
        assertEquals("Whey Gold Standard", response.getProductName());
        assertEquals(50, response.getStockQuantity());
        verify(productDao).save(any(Product.class));
        verify(inventoryDao).save(any(Inventory.class));
    }

    @Test
    void testCreateProduct_InvalidSellerRole() {
        User buyer = new User(3L, "Bob Buyer", "bob@buyer.com", "pass", "BUYER", "12345", "2026-10-09");
        ProductRequest req = new ProductRequest("Whey Gold Standard", "100% Whey Isolate", new BigDecimal("49.99"), 1L, 3L, 50);

        when(categoryDao.findById(1L)).thenReturn(Optional.of(category));
        when(userDao.findById(3L)).thenReturn(Optional.of(buyer));

        assertThrows(BadRequestException.class, () -> productService.createProduct(req));
    }

    @Test
    void testCreateProduct_CategoryNotFound() {
        ProductRequest req = new ProductRequest("Whey", "Desc", new BigDecimal("10.00"), 99L, 2L, 10);
        when(categoryDao.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(req));
    }

    @Test
    void testGetAllProducts() {
        when(productDao.findAll()).thenReturn(Collections.singletonList(product));

        List<ProductResponse> products = productService.getAllProducts(null, null);

        assertEquals(1, products.size());
        assertEquals("Whey Gold Standard", products.get(0).getProductName());
    }

    @Test
    void testSearchProducts() {
        when(productDao.search("Whey")).thenReturn(Collections.singletonList(product));

        List<ProductResponse> products = productService.getAllProducts("Whey", null);

        assertEquals(1, products.size());
        verify(productDao).search("Whey");
    }
}
