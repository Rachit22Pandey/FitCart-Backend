package com.fitcart.backend.dao;

import com.fitcart.backend.model.Payment;

import java.util.Optional;

public interface PaymentDao {

    Payment save(Payment payment);

    Optional<Payment> findByOrderId(Long orderId);
}
