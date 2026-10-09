package com.fitcart.backend.service;

import com.fitcart.backend.dao.CartDao;
import com.fitcart.backend.dao.InventoryDao;
import com.fitcart.backend.dao.ProductDao;
import com.fitcart.backend.dto.CartItemRequest;
import com.fitcart.backend.exception.BadRequestException;
import com.fitcart.backend.exception.ResourceNotFoundException;
import com.fitcart.backend.model.CartItem;
import com.fitcart.backend.model.Inventory;
import com.fitcart.backend.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartDao cartDao;
    private final ProductDao productDao;
    private final InventoryDao inventoryDao;

    public CartService(CartDao cartDao, ProductDao productDao, InventoryDao inventoryDao) {
        this.cartDao = cartDao;
        this.productDao = productDao;
        this.inventoryDao = inventoryDao;
    }

    public CartItem addToCart(CartItemRequest request) {
        Product product = productDao.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        int availableStock = inventoryDao.findByProductId(request.getProductId())
                .map(Inventory::getStockQuantity)
                .orElse(0);

        if (availableStock <= 0) {
            throw new BadRequestException("Product '" + product.getProductName() + "' is out of stock");
        }

        Optional<CartItem> existingItemOpt = cartDao.findByUserIdAndProductId(request.getUserId(), request.getProductId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQty = existingItem.getQuantity() + request.getQuantity();
            if (newQty > availableStock) {
                throw new BadRequestException("Cannot add " + request.getQuantity() + " more items. Available stock: " + availableStock);
            }
            cartDao.updateQuantity(existingItem.getCartId(), newQty);
            existingItem.setQuantity(newQty);
            return existingItem;
        } else {
            if (request.getQuantity() > availableStock) {
                throw new BadRequestException("Requested quantity (" + request.getQuantity() + ") exceeds available stock (" + availableStock + ")");
            }
            CartItem newItem = new CartItem(null, request.getUserId(), request.getProductId(), request.getQuantity());
            newItem.setProductName(product.getProductName());
            newItem.setPrice(product.getPrice());
            newItem.setStockQuantity(availableStock);
            return cartDao.save(newItem);
        }
    }

    public List<CartItem> getCartByUserId(Long userId) {
        return cartDao.findByUserId(userId);
    }

    public CartItem updateQuantity(Long cartId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        CartItem item = cartDao.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartId));

        int availableStock = inventoryDao.findByProductId(item.getProductId())
                .map(Inventory::getStockQuantity)
                .orElse(0);

        if (quantity > availableStock) {
            throw new BadRequestException("Requested quantity (" + quantity + ") exceeds available stock (" + availableStock + ")");
        }

        cartDao.updateQuantity(cartId, quantity);
        item.setQuantity(quantity);
        return item;
    }

    public void removeFromCart(Long cartId) {
        cartDao.deleteById(cartId);
    }

    public void clearCart(Long userId) {
        cartDao.deleteByUserId(userId);
    }
}
