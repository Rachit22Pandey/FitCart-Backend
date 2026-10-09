
package com.fitcart.backend.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.fitcart.backend.dao.AddressDao;
import com.fitcart.backend.dao.CartDao;
import com.fitcart.backend.dao.OrderDao;
import com.fitcart.backend.dao.OrderItemDao;
import com.fitcart.backend.dao.ProductDao;
import com.fitcart.backend.dao.UserDao;
import com.fitcart.backend.dao.WishlistDao;
import com.fitcart.backend.model.Address;
import com.fitcart.backend.model.CartItem;
import com.fitcart.backend.model.Order;
import com.fitcart.backend.model.Product;
import com.fitcart.backend.model.User;
import com.fitcart.backend.model.WishlistItem;

@Service("resourceAuthorizationService")
public class ResourceAuthorizationService {

    private final UserDao userDao;
    private final CartDao cartDao;
    private final WishlistDao wishlistDao;
    private final AddressDao addressDao;
    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final ProductDao productDao;

    public ResourceAuthorizationService(
            UserDao userDao,
            CartDao cartDao,
            WishlistDao wishlistDao,
            AddressDao addressDao,
            OrderDao orderDao,
            OrderItemDao orderItemDao,
            ProductDao productDao) {
        this.userDao = userDao;
        this.cartDao = cartDao;
        this.wishlistDao = wishlistDao;
        this.addressDao = addressDao;
        this.orderDao = orderDao;
        this.orderItemDao = orderItemDao;
        this.productDao = productDao;
    }

    public boolean canAccessUser(Long userId, Authentication auth) {
        if (!isAuthenticated(auth) || userId == null) return false;
        if (hasRole(auth, "ADMIN")) return true;

        Optional<User> current = currentUser(auth);
        return current.isPresent()
                && "BUYER".equalsIgnoreCase(current.get().getRole())
                && Objects.equals(current.get().getUserId(), userId);
    }

    public boolean canManageCartItem(Long cartId, Authentication auth) {
        if (!isAuthenticated(auth) || cartId == null) return false;
        if (hasRole(auth, "ADMIN")) return true;

        Optional<User> current = currentUser(auth);
        Optional<CartItem> item = cartDao.findById(cartId);

        return current.isPresent()
                && item.isPresent()
                && "BUYER".equalsIgnoreCase(current.get().getRole())
                && Objects.equals(
                        current.get().getUserId(),
                        item.get().getUserId());
    }

    public boolean canManageWishlistItem(
            Long wishlistId, Authentication auth) {
        if (!isAuthenticated(auth) || wishlistId == null) return false;
        if (hasRole(auth, "ADMIN")) return true;

        Optional<User> current = currentUser(auth);
        if (current.isEmpty()
                || !"BUYER".equalsIgnoreCase(current.get().getRole())) {
            return false;
        }

        return wishlistDao.findByUserId(current.get().getUserId())
                .stream()
                .anyMatch(item ->
                        Objects.equals(item.getWishlistId(), wishlistId));
    }

    public boolean canManageAddress(Long addressId, Authentication auth) {
        if (!isAuthenticated(auth) || addressId == null) return false;
        if (hasRole(auth, "ADMIN")) return true;

        Optional<User> current = currentUser(auth);
        Optional<Address> address = addressDao.findById(addressId);

        return current.isPresent()
                && address.isPresent()
                && "BUYER".equalsIgnoreCase(current.get().getRole())
                && Objects.equals(
                        current.get().getUserId(),
                        address.get().getUserId());
    }

    public boolean canAccessSellerOrders(
            Long sellerId, Authentication auth) {
        if (!isAuthenticated(auth) || sellerId == null) return false;
        if (hasRole(auth, "ADMIN")) return true;

        Optional<User> current = currentUser(auth);
        return current.isPresent()
                && "SELLER".equalsIgnoreCase(current.get().getRole())
                && Objects.equals(current.get().getUserId(), sellerId);
    }

    public boolean canReadOrder(Long orderId, Authentication auth) {
        if (!isAuthenticated(auth) || orderId == null) return false;
        if (hasRole(auth, "ADMIN")) return true;

        Optional<User> current = currentUser(auth);
        Optional<Order> order = orderDao.findById(orderId);

        if (current.isEmpty() || order.isEmpty()) return false;

        if ("BUYER".equalsIgnoreCase(current.get().getRole())
                && Objects.equals(
                        current.get().getUserId(), order.get().getUserId())) {
            return true;
        }

        return "SELLER".equalsIgnoreCase(current.get().getRole())
                && sellerOwnsOrderProduct(
                        orderId, current.get().getUserId());
    }

    public boolean canManageOrderStatus(
            Long orderId, Authentication auth) {
        if (!isAuthenticated(auth) || orderId == null) return false;
        if (hasRole(auth, "ADMIN")) return true;

        Optional<User> current = currentUser(auth);
        if (current.isEmpty()
                || !"SELLER".equalsIgnoreCase(current.get().getRole())) {
            return false;
        }

        return sellerOwnsOrderProduct(
                orderId, current.get().getUserId());
    }

    private boolean sellerOwnsOrderProduct(Long orderId, Long sellerId) {
        Optional<Order> order = orderDao.findById(orderId);
        if (order.isEmpty()) return false;

        return orderItemDao.findByOrderId(orderId).stream()
                .map(item -> productDao.findById(item.getProductId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Product::getSellerId)
                .anyMatch(id -> Objects.equals(id, sellerId));
    }

    private Optional<User> currentUser(Authentication auth) {
        if (!isAuthenticated(auth)) return Optional.empty();
        return userDao.findByEmail(auth.getName());
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())
                && auth.getAuthorities().stream()
                    .noneMatch(a ->
                        "ROLE_ANONYMOUS".equals(a.getAuthority()));
    }

    private boolean hasRole(Authentication auth, String role) {
        return isAuthenticated(auth)
                && auth.getAuthorities().stream()
                    .anyMatch(a ->
                        ("ROLE_" + role).equals(a.getAuthority()));
    }
}
