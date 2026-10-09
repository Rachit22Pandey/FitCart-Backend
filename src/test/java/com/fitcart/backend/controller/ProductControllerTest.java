package com.fitcart.backend.controller;

import com.fitcart.backend.dto.ProductRequest;
import com.fitcart.backend.dto.ProductResponse;
import com.fitcart.backend.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productResponse = new ProductResponse(10L, "Dumbbell Set 20kg", "Adjustable cast iron dumbbells",
                new BigDecimal("59.99"), 1L, "Equipment", 2L, "IronGym", 25);
    }

    @Test
    void testGetAllProducts() {
        when(productService.getAllProducts(null, null)).thenReturn(Collections.singletonList(productResponse));

        ResponseEntity<List<ProductResponse>> response = productController.getAllProducts(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Dumbbell Set 20kg", response.getBody().get(0).getProductName());
    }

    @Test
    void testGetProductById() {
        when(productService.getProductById(10L)).thenReturn(productResponse);

        ResponseEntity<ProductResponse> response = productController.getProductById(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Dumbbell Set 20kg", response.getBody().getProductName());
    }

    @Test
    void testCreateProduct() {
        ProductRequest req = new ProductRequest("Dumbbell Set 20kg", "Adjustable cast iron dumbbells",
                new BigDecimal("59.99"), 1L, 2L, 25);
        when(productService.createProduct(req)).thenReturn(productResponse);

        ResponseEntity<ProductResponse> response = productController.createProduct(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(25, response.getBody().getStockQuantity());
    }
}
