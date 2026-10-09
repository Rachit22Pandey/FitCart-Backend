package com.fitcart.backend.dao.impl;

import com.fitcart.backend.dao.InventoryDao;
import com.fitcart.backend.model.Inventory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class InventoryDaoImpl implements InventoryDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Inventory> inventoryRowMapper = (rs, rowNum) -> {
        Inventory inventory = new Inventory();
        inventory.setInventoryId(rs.getLong("inventory_id"));
        inventory.setProductId(rs.getLong("product_id"));
        inventory.setStockQuantity(rs.getInt("stock_quantity"));
        return inventory;
    };

    public InventoryDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Inventory save(Inventory inventory) {
        String sql = "INSERT INTO inventory (product_id, stock_quantity) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, inventory.getProductId());
            ps.setInt(2, inventory.getStockQuantity() != null ? inventory.getStockQuantity() : 0);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            inventory.setInventoryId(key.longValue());
        }
        return inventory;
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        String sql = "SELECT inventory_id, product_id, stock_quantity FROM inventory WHERE product_id = ?";
        return jdbcTemplate.query(sql, inventoryRowMapper, productId).stream().findFirst();
    }

    @Override
    public int updateStock(Long productId, Integer stockQuantity) {
        String sql = "UPDATE inventory SET stock_quantity = ? WHERE product_id = ?";
        int updated = jdbcTemplate.update(sql, stockQuantity, productId);
        if (updated == 0) {
            // Upsert fallback if row didn't exist yet
            Inventory inv = new Inventory(null, productId, stockQuantity);
            save(inv);
            return 1;
        }
        return updated;
    }

    @Override
    public int decreaseStock(Long productId, Integer quantity) {
        // Atomic stock reduction ensuring it doesn't drop below zero
        String sql = "UPDATE inventory SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?";
        return jdbcTemplate.update(sql, quantity, productId, quantity);
    }

    @Override
    public int increaseStock(Long productId, Integer quantity) {
        String sql = "UPDATE inventory SET stock_quantity = stock_quantity + ? WHERE product_id = ?";
        return jdbcTemplate.update(sql, quantity, productId);
    }

    @Override
    public int deleteByProductId(Long productId) {
        String sql = "DELETE FROM inventory WHERE product_id = ?";
        return jdbcTemplate.update(sql, productId);
    }
}
