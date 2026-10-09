
package com.fitcart.backend.dao.impl;

import com.fitcart.backend.dao.PaymentDao;
import com.fitcart.backend.model.Payment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class PaymentDaoImpl implements PaymentDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Payment> paymentRowMapper = (rs, rowNum) -> {
        Payment payment = new Payment();

        payment.setPaymentId(rs.getLong("payment_id"));
        payment.setOrderId(rs.getLong("order_id"));
        payment.setPaymentMethod(rs.getString("payment_method"));
        payment.setPaymentStatus(rs.getString("payment_status"));
        payment.setTransactionId(rs.getString("transaction_id"));

        // Amount comes from the linked order table.
        payment.setAmount(rs.getBigDecimal("amount"));

        // The current payments table has no payment_date column.
        payment.setPaymentDate(null);

        return payment;
    };

    public PaymentDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Payment save(Payment payment) {

        // Match the columns that actually exist in the payments table.
        String sql =
                "INSERT INTO payments " +
                "(order_id, payment_method, payment_status, transaction_id) " +
                "VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);

            ps.setLong(1, payment.getOrderId());
            ps.setString(2, payment.getPaymentMethod());
            ps.setString(3, payment.getPaymentStatus());
            ps.setString(4, payment.getTransactionId());

            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key != null) {
            payment.setPaymentId(key.longValue());
        }

        return payment;
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {

        // Obtain the amount from orders rather than payments.
        String sql =
                "SELECT p.payment_id, p.order_id, p.payment_method, " +
                "p.payment_status, p.transaction_id, " +
                "o.total_amount AS amount " +
                "FROM payments p " +
                "JOIN orders o ON o.order_id = p.order_id " +
                "WHERE p.order_id = ?";

        return jdbcTemplate.query(
                sql, paymentRowMapper, orderId
        ).stream().findFirst();
    }
}
