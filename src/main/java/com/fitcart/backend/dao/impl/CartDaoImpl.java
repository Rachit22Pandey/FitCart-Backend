package com.fitcart.backend.dao.impl;

import com.fitcart.backend.dao.CartDao;
import com.fitcart.backend.model.CartItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class CartDaoImpl implements CartDao {

    private final JdbcTemplate jdbcTemplate;

    private static final String JOIN_SELECT =
            "SELECT c.cart_id, c.user_id, c.product_id, c.quantity, " +
            "p.product_name, p.price, COALESCE(i.stock_quantity, 0) AS stock_quantity " +
            "FROM cart c " +
            "JOIN products p ON c.product_id = p.product_id " +
            "LEFT JOIN inventory i ON p.product_id = i.product_id ";

    private final RowMapper<CartItem> cartItemRowMapper = (rs, rowNum) -> {
        CartItem item = new CartItem();
        item.setCartId(rs.getLong("cart_id"));
        item.setUserId(rs.getLong("user_id"));
        item.setProductId(rs.getLong("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setPrice(rs.getBigDecimal("price"));
        item.setQuantity(rs.getInt("quantity"));
        item.setStockQuantity(rs.getInt("stock_quantity"));
        return item;
    };

    public CartDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CartItem save(CartItem item) {
        String sql = "INSERT INTO cart (user_id, product_id, quantity) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, item.getUserId());
            ps.setLong(2, item.getProductId());
            ps.setInt(3, item.getQuantity() != null ? item.getQuantity() : 1);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            item.setCartId(key.longValue());
        }
        return item;
    }

    @Override
    public Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId) {
        String sql = JOIN_SELECT + "WHERE c.user_id = ? AND c.product_id = ?";
        return jdbcTemplate.query(sql, cartItemRowMapper, userId, productId).stream().findFirst();
    }

    @Override
    public Optional<CartItem> findById(Long cartId) {
        String sql = JOIN_SELECT + "WHERE c.cart_id = ?";
        return jdbcTemplate.query(sql, cartItemRowMapper, cartId).stream().findFirst();
    }

    @Override
    public List<CartItem> findByUserId(Long userId) {
        String sql = JOIN_SELECT + "WHERE c.user_id = ? ORDER BY c.cart_id DESC";
        return jdbcTemplate.query(sql, cartItemRowMapper, userId);
    }

    @Override
    public int updateQuantity(Long cartId, Integer quantity) {
        String sql = "UPDATE cart SET quantity = ? WHERE cart_id = ?";
        return jdbcTemplate.update(sql, quantity, cartId);
    }

    @Override
    public int deleteById(Long cartId) {
        String sql = "DELETE FROM cart WHERE cart_id = ?";
        return jdbcTemplate.update(sql, cartId);
    }

    @Override
    public int deleteByUserId(Long userId) {
        String sql = "DELETE FROM cart WHERE user_id = ?";
        return jdbcTemplate.update(sql, userId);
    }
}
