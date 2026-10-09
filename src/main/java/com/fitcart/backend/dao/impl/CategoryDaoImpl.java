package com.fitcart.backend.dao.impl;

import com.fitcart.backend.dao.CategoryDao;
import com.fitcart.backend.model.Category;
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
public class CategoryDaoImpl implements CategoryDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Category> categoryRowMapper = (rs, rowNum) -> {
        Category category = new Category();
        category.setCategoryId(rs.getLong("category_id"));
        category.setCategoryName(rs.getString("category_name"));
        return category;
    };

    public CategoryDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Category save(Category category) {
        String sql = "INSERT INTO categories (category_name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, category.getCategoryName());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            category.setCategoryId(key.longValue());
        }
        return category;
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT category_id, category_name FROM categories ORDER BY category_name ASC";
        return jdbcTemplate.query(sql, categoryRowMapper);
    }

    @Override
    public Optional<Category> findById(Long categoryId) {
        String sql = "SELECT category_id, category_name FROM categories WHERE category_id = ?";
        return jdbcTemplate.query(sql, categoryRowMapper, categoryId).stream().findFirst();
    }

    @Override
    public Optional<Category> findByName(String categoryName) {
        String sql = "SELECT category_id, category_name FROM categories WHERE LOWER(category_name) = LOWER(?)";
        return jdbcTemplate.query(sql, categoryRowMapper, categoryName).stream().findFirst();
    }

    @Override
    public boolean existsByName(String categoryName) {
        String sql = "SELECT COUNT(*) FROM categories WHERE LOWER(category_name) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, categoryName);
        return count != null && count > 0;
    }

    @Override
    public int update(Category category) {
        String sql = "UPDATE categories SET category_name = ? WHERE category_id = ?";
        return jdbcTemplate.update(sql, category.getCategoryName(), category.getCategoryId());
    }

    @Override
    public int deleteById(Long categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        return jdbcTemplate.update(sql, categoryId);
    }
}
