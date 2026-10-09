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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final UserDao userDao;
    private final InventoryDao inventoryDao;

    public ProductService(ProductDao productDao, CategoryDao categoryDao, UserDao userDao, InventoryDao inventoryDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
        this.userDao = userDao;
        this.inventoryDao = inventoryDao;
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryDao.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        User seller = userDao.findById(request.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + request.getSellerId()));

        if (!"SELLER".equalsIgnoreCase(seller.getRole()) && !"ADMIN".equalsIgnoreCase(seller.getRole())) {
            throw new BadRequestException("User with id " + request.getSellerId() + " does not have SELLER or ADMIN role");
        }

        Product product = new Product();
        product.setProductName(request.getProductName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategoryId(category.getCategoryId());
        product.setSellerId(seller.getUserId());

        Product savedProduct = productDao.save(product);

        int initialStock = request.getInitialStock() != null ? request.getInitialStock() : 0;
        Inventory inventory = new Inventory();
        inventory.setProductId(savedProduct.getProductId());
        inventory.setStockQuantity(initialStock);
        inventoryDao.save(inventory);

        savedProduct.setCategoryName(category.getCategoryName());
        savedProduct.setSellerName(seller.getName());
        savedProduct.setStockQuantity(initialStock);

        return ProductResponse.fromProduct(savedProduct);
    }

    public List<ProductResponse> getAllProducts(String keyword, Long categoryId) {
        List<Product> products;
        if (keyword != null && !keyword.isBlank()) {
            products = productDao.search(keyword);
        } else if (categoryId != null) {
            products = productDao.findByCategoryId(categoryId);
        } else {
            products = productDao.findAll();
        }
        return products.stream().map(ProductResponse::fromProduct).collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long productId) {
        return productDao.findById(productId)
                .map(ProductResponse::fromProduct)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productDao.findBySellerId(sellerId).stream()
                .map(ProductResponse::fromProduct)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        Product existing = productDao.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Category category = categoryDao.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        existing.setProductName(request.getProductName().trim());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        existing.setCategoryId(category.getCategoryId());

        productDao.update(existing);

        if (request.getInitialStock() != null) {
            inventoryDao.updateStock(productId, request.getInitialStock());
            existing.setStockQuantity(request.getInitialStock());
        }

        existing.setCategoryName(category.getCategoryName());
        return ProductResponse.fromProduct(existing);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        productDao.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        inventoryDao.deleteByProductId(productId);
        productDao.deleteById(productId);
    }
}
