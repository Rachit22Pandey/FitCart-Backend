
package com.fitcart.backend.service;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.fitcart.backend.dao.ProductDao;
import com.fitcart.backend.dao.UserDao;
import com.fitcart.backend.model.Product;
import com.fitcart.backend.model.User;

@Service("productAuthorizationService")
public class ProductAuthorizationService {

    private final ProductDao productDao;
    private final UserDao userDao;

    public ProductAuthorizationService(
            ProductDao productDao,
            UserDao userDao) {
        this.productDao = productDao;
        this.userDao = userDao;
    }

    public boolean canCreateForSeller(
            Long sellerId,
            Authentication authentication) {

        if (!isAuthenticated(authentication) || sellerId == null) {
            return false;
        }

        Optional<User> seller = userDao.findById(sellerId);

        if (seller.isEmpty()) {
            return false;
        }

        if (hasRole(authentication, "ADMIN")) {
            return true;
        }

        Optional<User> loggedInUser =
                userDao.findByEmail(authentication.getName());

        return loggedInUser.isPresent()
                && "SELLER".equalsIgnoreCase(loggedInUser.get().getRole())
                && loggedInUser.get().getUserId().equals(sellerId)
                && "SELLER".equalsIgnoreCase(seller.get().getRole());
    }

    public boolean canManageProduct(
            Long productId,
            Authentication authentication) {

        if (!isAuthenticated(authentication) || productId == null) {
            return false;
        }

        if (hasRole(authentication, "ADMIN")) {
            return true;
        }

        Optional<User> loggedInUser =
                userDao.findByEmail(authentication.getName());

        Optional<Product> product =
                productDao.findById(productId);

        return loggedInUser.isPresent()
                && product.isPresent()
                && "SELLER".equalsIgnoreCase(loggedInUser.get().getRole())
                && loggedInUser.get().getUserId()
                        .equals(product.get().getSellerId());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    private boolean hasRole(
            Authentication authentication,
            String role) {

        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_" + role));
    }
}
