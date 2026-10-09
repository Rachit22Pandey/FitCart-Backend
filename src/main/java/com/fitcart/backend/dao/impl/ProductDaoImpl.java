package com.fitcart.backend.dao.impl;

import com.fitcart.backend.dao.ProductDao;
import com.fitcart.backend.model.Product;
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
public class ProductDaoImpl implements ProductDao {

    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_SELECT =
            "SELECT p.product_id, p.product_name, p.description, p.price, p.category_id, p.seller_id, " +
            "c.category_name, u.name AS seller_name, COALESCE(i.stock_quantity, 0) AS stock_quantity " +
            "FROM products p " +
            "LEFT JOIN categories c ON p.category_id = c.category_id " +
            "LEFT JOIN users u ON p.seller_id = u.user_id " +
            "LEFT JOIN inventory i ON p.product_id = i.product_id ";

    private final RowMapper<Product> productRowMapper = (rs, rowNum) -> {
        Product product = new Product();
        product.setProductId(rs.getLong("product_id"));
        product.setProductName(rs.getString("product_name"));
        product.setDescription(rs.getString("description"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setCategoryId(rs.getLong("category_id"));
        product.setSellerId(rs.getLong("seller_id"));
        product.setCategoryName(rs.getString("category_name"));
        product.setSellerName(rs.getString("seller_name"));
        product.setStockQuantity(rs.getInt("stock_quantity"));
        return product;
    };

    public ProductDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Product save(Product product) {
        String sql = "INSERT INTO products (product_name, description, price, category_id, seller_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, product.getProductName());
            ps.setString(2, product.getDescription());
            ps.setBigDecimal(3, product.getPrice());
            ps.setLong(4, product.getCategoryId());
            ps.setLong(5, product.getSellerId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            product.setProductId(key.longValue());
        }
        return product;
    }

    @Override
    public List<Product> findAll() {
        String sql = BASE_SELECT + "ORDER BY p.product_id DESC";
        return jdbcTemplate.query(sql, productRowMapper);
    }

    @Override
    public Optional<Product> findById(Long productId) {
        String sql = BASE_SELECT + "WHERE p.product_id = ?";
        return jdbcTemplate.query(sql, productRowMapper, productId).stream().findFirst();
    }

    @Override
    public List<Product> findByCategoryId(Long categoryId) {
        String sql = BASE_SELECT + "WHERE p.category_id = ? ORDER BY p.product_id DESC";
        return jdbcTemplate.query(sql, productRowMapper, categoryId);
    }

    @Override
    public List<Product> findBySellerId(Long sellerId) {
        String sql = BASE_SELECT + "WHERE p.seller_id = ? ORDER BY p.product_id DESC";
        return jdbcTemplate.query(sql, productRowMapper, sellerId);
    }

    @Override
    public List<Product> search(String keyword) {
        String sql = BASE_SELECT + "WHERE LOWER(p.product_name) LIKE LOWER(?) OR LOWER(p.description) LIKE LOWER(?) ORDER BY p.product_id DESC";
        String pattern = "%" + keyword.trim() + "%";
        return jdbcTemplate.query(sql, productRowMapper, pattern, pattern);
    }

    @Override
    public int update(Product product) {
        String sql = "UPDATE products SET product_name = ?, description = ?, price = ?, category_id = ? WHERE product_id = ?";
        return jdbcTemplate.update(sql,
                product.getProductName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategoryId(),
                product.getProductId());
    }

    @Override
    public int deleteById(Long productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";
        return jdbcTemplate.update(sql, productId);
    }
}
