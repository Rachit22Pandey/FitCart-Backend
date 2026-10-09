package com.fitcart.backend.service;

import com.fitcart.backend.dao.CategoryDao;
import com.fitcart.backend.dto.CategoryRequest;
import com.fitcart.backend.exception.BadRequestException;
import com.fitcart.backend.exception.ResourceNotFoundException;
import com.fitcart.backend.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryDao categoryDao;

    @InjectMocks
    private CategoryService categoryService;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category(1L, "Supplements");
    }

    @Test
    void testCreateCategory_Success() {
        CategoryRequest req = new CategoryRequest("Supplements");
        when(categoryDao.existsByName("Supplements")).thenReturn(false);
        when(categoryDao.save(any(Category.class))).thenReturn(sampleCategory);

        Category created = categoryService.createCategory(req);

        assertNotNull(created);
        assertEquals("Supplements", created.getCategoryName());
        verify(categoryDao).save(any(Category.class));
    }

    @Test
    void testCreateCategory_DuplicateName() {
        CategoryRequest req = new CategoryRequest("Supplements");
        when(categoryDao.existsByName("Supplements")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> categoryService.createCategory(req));
    }

    @Test
    void testGetAllCategories() {
        Category c2 = new Category(2L, "Equipment");
        when(categoryDao.findAll()).thenReturn(Arrays.asList(sampleCategory, c2));

        List<Category> list = categoryService.getAllCategories();

        assertEquals(2, list.size());
        verify(categoryDao).findAll();
    }

    @Test
    void testGetCategoryById_NotFound() {
        when(categoryDao.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(99L));
    }
}
