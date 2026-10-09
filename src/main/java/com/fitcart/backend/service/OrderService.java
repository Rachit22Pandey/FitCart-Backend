package com.fitcart.backend.service;

import com.fitcart.backend.dao.*;
import com.fitcart.backend.dto.OrderItemDto;
import com.fitcart.backend.dto.PlaceOrderRequest;
import com.fitcart.backend.exception.BadRequestException;
import com.fitcart.backend.exception.ResourceNotFoundException;
import com.fitcart.backend.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {

    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final PaymentDao paymentDao;
    private final ProductDao productDao;
    private final InventoryDao inventoryDao;
    private final CartDao cartDao;
    private final UserDao userDao;

    public OrderService(OrderDao orderDao,
                        OrderItemDao orderItemDao,
                        PaymentDao paymentDao,
                        ProductDao productDao,
                        InventoryDao inventoryDao,
                        CartDao cartDao,
                        UserDao userDao) {
        this.orderDao = orderDao;
        this.orderItemDao = orderItemDao;
        this.paymentDao = paymentDao;
        this.productDao = productDao;
        this.inventoryDao = inventoryDao;
        this.cartDao = cartDao;
        this.userDao = userDao;
    }

    @Transactional
    public Order placeOrder(PlaceOrderRequest request) {
        User user = userDao.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        List<OrderItemDto> itemsToOrder = new ArrayList<>();
        boolean fromCart = request.getUseCart();

        if (fromCart) {
            List<CartItem> cartItems = cartDao.findByUserId(user.getUserId());
            if (cartItems.isEmpty()) {
                throw new BadRequestException("Cannot place order: Cart is empty");
            }
            for (CartItem ci : cartItems) {
                itemsToOrder.add(new OrderItemDto(ci.getProductId(), ci.getQuantity()));
            }
        } else {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new BadRequestException("Order must contain at least one item");
            }
            itemsToOrder = request.getItems();
        }

        // Validate products, calculate subtotal and check stock
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Product, Integer> verifiedItems = new LinkedHashMap<>();

        for (OrderItemDto itemDto : itemsToOrder) {
            Product product = productDao.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId()));

            int availableStock = inventoryDao.findByProductId(product.getProductId())
                    .map(Inventory::getStockQuantity)
                    .orElse(0);

            if (availableStock < itemDto.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product '" + product.getProductName() +
                        "'. Available: " + availableStock + ", requested: " + itemDto.getQuantity());
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
            verifiedItems.put(product, itemDto.getQuantity());
        }

        // Atomically decrease stock in inventory
        for (Map.Entry<Product, Integer> entry : verifiedItems.entrySet()) {
            int updated = inventoryDao.decreaseStock(entry.getKey().getProductId(), entry.getValue());
            if (updated == 0) {
                throw new BadRequestException("Stock changed concurrently for '" + entry.getKey().getProductName() + "'. Order aborted.");
            }
        }

        // Save order
        Order order = new Order();
        order.setUserId(user.getUserId());
        order.setTotalAmount(totalAmount);
        order.setStatus("CONFIRMED");
        Order savedOrder = orderDao.save(order);

        // Save order items
        for (Map.Entry<Product, Integer> entry : verifiedItems.entrySet()) {
            OrderItem orderItem = new OrderItem(
                    null,
                    savedOrder.getOrderId(),
                    entry.getKey().getProductId(),
                    entry.getValue(),
                    entry.getKey().getPrice()
            );
            orderItemDao.save(orderItem);
        }

        // Demonstration payment record
        String paymentMethod = (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod().trim().toUpperCase()
                : "DEMO_CARD";
        String txnId = "DEMO-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = new Payment(
                null,
                savedOrder.getOrderId(),
                paymentMethod,
                "DEMO_PAID",
                txnId,
                totalAmount,
                null
        );
        paymentDao.save(payment);

        // Clear cart if ordered from cart
        if (fromCart) {
            cartDao.deleteByUserId(user.getUserId());
        }

        return getOrderById(savedOrder.getOrderId());
    }

    public Order getOrderById(Long orderId) {
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setItems(orderItemDao.findByOrderId(orderId));
        order.setPayment(paymentDao.findByOrderId(orderId).orElse(null));
        return order;
    }

    public List<Order> getUserOrders(Long userId) {
        List<Order> orders = orderDao.findByUserId(userId);
        for (Order o : orders) {
            o.setItems(orderItemDao.findByOrderId(o.getOrderId()));
            o.setPayment(paymentDao.findByOrderId(o.getOrderId()).orElse(null));
        }
        return orders;
    }

    public List<Order> getSellerOrders(Long sellerId) {
        List<Order> orders = orderDao.findBySellerId(sellerId);
        for (Order o : orders) {
            o.setItems(orderItemDao.findByOrderId(o.getOrderId()));
            o.setPayment(paymentDao.findByOrderId(o.getOrderId()).orElse(null));
        }
        return orders;
    }

    public List<Order> getAllOrders() {
        List<Order> orders = orderDao.findAll();
        for (Order o : orders) {
            o.setItems(orderItemDao.findByOrderId(o.getOrderId()));
            o.setPayment(paymentDao.findByOrderId(o.getOrderId()).orElse(null));
        }
        return orders;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        getOrderById(orderId); // ensure exists
        String status = newStatus.trim().toUpperCase();
        if (!Arrays.asList("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED").contains(status)) {
            throw new BadRequestException("Invalid status: " + newStatus + ". Allowed: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED");
        }
        orderDao.updateStatus(orderId, status);
        return getOrderById(orderId);
    }
}
