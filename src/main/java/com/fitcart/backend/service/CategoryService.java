package com.fitcart.backend.service;

import com.fitcart.backend.dao.CategoryDao;
import com.fitcart.backend.dto.CategoryRequest;
import com.fitcart.backend.exception.BadRequestException;
import com.fitcart.backend.exception.ResourceNotFoundException;
import com.fitcart.backend.model.Category;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryDao categoryDao;

    public CategoryService(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    public Category createCategory(CategoryRequest request) {
        String name = request.getCategoryName().trim();
        if (categoryDao.existsByName(name)) {
            throw new BadRequestException("Category '" + name + "' already exists");
        }
        Category category = new Category();
        category.setCategoryName(name);
        return categoryDao.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryDao.findAll();
    }

    public Category getCategoryById(Long categoryId) {
        return categoryDao.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    public Category updateCategory(Long categoryId, CategoryRequest request) {
        Category existing = getCategoryById(categoryId);
        String newName = request.getCategoryName().trim();

        if (!existing.getCategoryName().equalsIgnoreCase(newName) && categoryDao.existsByName(newName)) {
            throw new BadRequestException("Category '" + newName + "' already exists");
        }

        existing.setCategoryName(newName);
        categoryDao.update(existing);
        return existing;
    }

    public void deleteCategory(Long categoryId) {
        getCategoryById(categoryId); // ensure exists
        categoryDao.deleteById(categoryId);
    }
}
