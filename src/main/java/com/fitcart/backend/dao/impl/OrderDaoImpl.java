package com.fitcart.backend.dao.impl;

import com.fitcart.backend.dao.OrderDao;
import com.fitcart.backend.model.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderDaoImpl implements OrderDao {

    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_SELECT =
            "SELECT o.order_id, o.user_id, o.order_date, o.total_amount, o.status, " +
            "u.name AS customer_name, u.email AS customer_email " +
            "FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.user_id ";

    private final RowMapper<Order> orderRowMapper = (rs, rowNum) -> {
        Order order = new Order();
        order.setOrderId(rs.getLong("order_id"));
        order.setUserId(rs.getLong("user_id"));
        order.setOrderDate(rs.getString("order_date"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setStatus(rs.getString("status"));
        order.setCustomerName(rs.getString("customer_name"));
        order.setCustomerEmail(rs.getString("customer_email"));
        return order;
    };

    public OrderDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Order save(Order order) {
        String sql = "INSERT INTO orders (user_id, order_date, total_amount, status) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        String orderDateVal = order.getOrderDate();
        if (orderDateVal == null || orderDateVal.isBlank()) {
            orderDateVal = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            order.setOrderDate(orderDateVal);
        }
        final String finalDate = orderDateVal;

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, order.getUserId());
            ps.setString(2, finalDate);
            ps.setBigDecimal(3, order.getTotalAmount());
            ps.setString(4, order.getStatus() != null ? order.getStatus() : "CONFIRMED");
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            order.setOrderId(key.longValue());
        }
        return order;
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        String sql = BASE_SELECT + "WHERE o.order_id = ?";
        return jdbcTemplate.query(sql, orderRowMapper, orderId).stream().findFirst();
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        String sql = BASE_SELECT + "WHERE o.user_id = ? ORDER BY o.order_id DESC";
        return jdbcTemplate.query(sql, orderRowMapper, userId);
    }

    @Override
    public List<Order> findBySellerId(Long sellerId) {
        String sql =
                "SELECT DISTINCT o.order_id, o.user_id, o.order_date, o.total_amount, o.status, " +
                "u.name AS customer_name, u.email AS customer_email " +
                "FROM orders o " +
                "JOIN order_items oi ON o.order_id = oi.order_id " +
                "JOIN products p ON oi.product_id = p.product_id " +
                "LEFT JOIN users u ON o.user_id = u.user_id " +
                "WHERE p.seller_id = ? " +
                "ORDER BY o.order_id DESC";
        return jdbcTemplate.query(sql, orderRowMapper, sellerId);
    }

    @Override
    public List<Order> findAll() {
        String sql = BASE_SELECT + "ORDER BY o.order_id DESC";
        return jdbcTemplate.query(sql, orderRowMapper);
    }

    @Override
    public int updateStatus(Long orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        return jdbcTemplate.update(sql, status, orderId);
    }
}
