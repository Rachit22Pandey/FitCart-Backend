
package com.fitcart.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fitcart.backend.dto.ProductRequest;
import com.fitcart.backend.dto.ProductResponse;
import com.fitcart.backend.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {

        return ResponseEntity.ok(
                productService.getAllProducts(keyword, categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ProductResponse>> getProductsBySeller(
            @PathVariable("sellerId") Long sellerId) {

        return ResponseEntity.ok(
                productService.getProductsBySeller(sellerId));
    }

    @PostMapping
    @PreAuthorize(
        "@productAuthorizationService.canCreateForSeller(#request.sellerId, authentication)"
    )
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response =
                productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
        "@productAuthorizationService.canManageProduct(#id, authentication)"
    )
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response =
                productService.updateProduct(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(
        "@productAuthorizationService.canManageProduct(#id, authentication)"
    )
    public ResponseEntity<Void> deleteProduct(
            @PathVariable("id") Long id) {

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }
}
