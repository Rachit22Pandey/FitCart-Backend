package com.fitcart.backend.dao.impl;

import com.fitcart.backend.dao.OrderItemDao;
import com.fitcart.backend.model.OrderItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class OrderItemDaoImpl implements OrderItemDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<OrderItem> orderItemRowMapper = (rs, rowNum) -> {
        OrderItem item = new OrderItem();
        try {
            item.setOrderItemId(rs.getLong("order_item_id"));
        } catch (Exception ignored) {
            // In case column is named item_id or has different alias
            try {
                item.setOrderItemId(rs.getLong("item_id"));
            } catch (Exception e) {
                item.setOrderItemId(null);
            }
        }
        item.setOrderId(rs.getLong("order_id"));
        item.setProductId(rs.getLong("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setPrice(rs.getBigDecimal("price"));
        item.setProductName(rs.getString("product_name"));
        item.setSellerId(rs.getLong("seller_id"));
        item.setSellerName(rs.getString("seller_name"));
        return item;
    };

    public OrderItemDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OrderItem save(OrderItem item) {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, item.getOrderId());
            ps.setLong(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getPrice());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            item.setOrderItemId(key.longValue());
        }
        return item;
    }

    @Override
    public List<OrderItem> findByOrderId(Long orderId) {
        String sql =
                "SELECT oi.*, p.product_name, p.seller_id, u.name AS seller_name " +
                "FROM order_items oi " +
                "JOIN products p ON oi.product_id = p.product_id " +
                "LEFT JOIN users u ON p.seller_id = u.user_id " +
                "WHERE oi.order_id = ?";
        return jdbcTemplate.query(sql, orderItemRowMapper, orderId);
    }
}
