package com.fitcart.backend.service;

import com.fitcart.backend.dao.*;
import com.fitcart.backend.dto.OrderItemDto;
import com.fitcart.backend.dto.PlaceOrderRequest;
import com.fitcart.backend.exception.BadRequestException;
import com.fitcart.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private OrderItemDao orderItemDao;

    @Mock
    private PaymentDao paymentDao;

    @Mock
    private ProductDao productDao;

    @Mock
    private InventoryDao inventoryDao;

    @Mock
    private CartDao cartDao;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private OrderService orderService;

    private User buyer;
    private Product product;
    private Inventory inventory;
    private Order order;

    @BeforeEach
    void setUp() {
        buyer = new User(10L, "John", "john@fit.com", "pass", "BUYER", "12345", "2026-10-09");
        product = new Product(1L, "Pre-Workout Explosion", "Energy booster", new BigDecimal("30.00"), 1L, 2L);
        inventory = new Inventory(1L, 1L, 10);
        order = new Order(100L, 10L, "2026-10-09 12:00:00", new BigDecimal("60.00"), "CONFIRMED");
    }

    @Test
    void testPlaceOrder_DirectItems_Success() {
        OrderItemDto itemDto = new OrderItemDto(1L, 2);
        PlaceOrderRequest req = new PlaceOrderRequest(10L, Collections.singletonList(itemDto), false, "CARD", 1L);

        when(userDao.findById(10L)).thenReturn(Optional.of(buyer));
        when(productDao.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryDao.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryDao.decreaseStock(1L, 2)).thenReturn(1);
        when(orderDao.save(any(Order.class))).thenReturn(order);
        when(orderDao.findById(100L)).thenReturn(Optional.of(order));

        Order placed = orderService.placeOrder(req);

        assertNotNull(placed);
        assertEquals(100L, placed.getOrderId());
        verify(inventoryDao).decreaseStock(1L, 2);
        verify(orderItemDao).save(any(OrderItem.class));
        verify(paymentDao).save(any(Payment.class));
    }

    @Test
    void testPlaceOrder_InsufficientStock() {
        OrderItemDto itemDto = new OrderItemDto(1L, 50); // available is 10
        PlaceOrderRequest req = new PlaceOrderRequest(10L, Collections.singletonList(itemDto), false, "UPI", 1L);

        when(userDao.findById(10L)).thenReturn(Optional.of(buyer));
        when(productDao.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryDao.findByProductId(1L)).thenReturn(Optional.of(inventory));

        assertThrows(BadRequestException.class, () -> orderService.placeOrder(req));
    }

    @Test
    void testUpdateOrderStatus() {
        when(orderDao.findById(100L)).thenReturn(Optional.of(order));
        when(orderDao.updateStatus(100L, "SHIPPED")).thenReturn(1);

        Order updated = orderService.updateOrderStatus(100L, "SHIPPED");

        assertNotNull(updated);
        verify(orderDao).updateStatus(100L, "SHIPPED");
    }
}
