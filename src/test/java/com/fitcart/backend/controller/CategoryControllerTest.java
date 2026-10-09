package com.fitcart.backend.controller;

import com.fitcart.backend.dto.CategoryRequest;
import com.fitcart.backend.model.Category;
import com.fitcart.backend.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category(1L, "Supplements");
    }

    @Test
    void testGetAllCategories() {
        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(category));

        ResponseEntity<List<Category>> response = categoryController.getAllCategories();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testCreateCategory() {
        CategoryRequest req = new CategoryRequest("Supplements");
        when(categoryService.createCategory(req)).thenReturn(category);

        ResponseEntity<Category> response = categoryController.createCategory(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Supplements", response.getBody().getCategoryName());
    }
}
