package com.fitcart.backend.dao.impl;

import com.fitcart.backend.dao.WishlistDao;
import com.fitcart.backend.model.WishlistItem;
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
public class WishlistDaoImpl implements WishlistDao {

    private final JdbcTemplate jdbcTemplate;

    private static final String JOIN_SELECT =
            "SELECT w.wishlist_id, w.user_id, w.product_id, " +
            "p.product_name, p.price, c.category_name " +
            "FROM wishlist w " +
            "JOIN products p ON w.product_id = p.product_id " +
            "LEFT JOIN categories c ON p.category_id = c.category_id ";

    private final RowMapper<WishlistItem> wishlistRowMapper = (rs, rowNum) -> {
        WishlistItem item = new WishlistItem();
        item.setWishlistId(rs.getLong("wishlist_id"));
        item.setUserId(rs.getLong("user_id"));
        item.setProductId(rs.getLong("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setPrice(rs.getBigDecimal("price"));
        item.setCategoryName(rs.getString("category_name"));
        return item;
    };

    public WishlistDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public WishlistItem save(WishlistItem item) {
        String sql = "INSERT INTO wishlist (user_id, product_id) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, item.getUserId());
            ps.setLong(2, item.getProductId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            item.setWishlistId(key.longValue());
        }
        return item;
    }

    @Override
    public Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId) {
        String sql = JOIN_SELECT + "WHERE w.user_id = ? AND w.product_id = ?";
        return jdbcTemplate.query(sql, wishlistRowMapper, userId, productId).stream().findFirst();
    }

    @Override
    public List<WishlistItem> findByUserId(Long userId) {
        String sql = JOIN_SELECT + "WHERE w.user_id = ? ORDER BY w.wishlist_id DESC";
        return jdbcTemplate.query(sql, wishlistRowMapper, userId);
    }

    @Override
    public int deleteById(Long wishlistId) {
        String sql = "DELETE FROM wishlist WHERE wishlist_id = ?";
        return jdbcTemplate.update(sql, wishlistId);
    }

    @Override
    public int deleteByUserIdAndProductId(Long userId, Long productId) {
        String sql = "DELETE FROM wishlist WHERE user_id = ? AND product_id = ?";
        return jdbcTemplate.update(sql, userId, productId);
    }
}
