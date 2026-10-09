package com.fitcart.backend.dao.impl;

import com.fitcart.backend.dao.AddressDao;
import com.fitcart.backend.model.Address;
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
public class AddressDaoImpl implements AddressDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Address> addressRowMapper = (rs, rowNum) -> {
        Address address = new Address();
        address.setAddressId(rs.getLong("address_id"));
        address.setUserId(rs.getLong("user_id"));
        address.setStreet(rs.getString("street"));
        address.setCity(rs.getString("city"));
        address.setState(rs.getString("state"));
        address.setPostalCode(rs.getString("postal_code"));
        address.setCountry(rs.getString("country"));
        address.setPhone(rs.getString("phone"));
        return address;
    };

    public AddressDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Address save(Address address) {
        String sql = "INSERT INTO addresses (user_id, street, city, state, postal_code, country, phone) VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, address.getUserId());
            ps.setString(2, address.getStreet());
            ps.setString(3, address.getCity());
            ps.setString(4, address.getState());
            ps.setString(5, address.getPostalCode());
            ps.setString(6, address.getCountry());
            ps.setString(7, address.getPhone());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            address.setAddressId(key.longValue());
        }
        return address;
    }

    @Override
    public Optional<Address> findById(Long addressId) {
        String sql = "SELECT address_id, user_id, street, city, state, postal_code, country, phone FROM addresses WHERE address_id = ?";
        return jdbcTemplate.query(sql, addressRowMapper, addressId).stream().findFirst();
    }

    @Override
    public List<Address> findByUserId(Long userId) {
        String sql = "SELECT address_id, user_id, street, city, state, postal_code, country, phone FROM addresses WHERE user_id = ? ORDER BY address_id DESC";
        return jdbcTemplate.query(sql, addressRowMapper, userId);
    }

    @Override
    public int update(Address address) {
        String sql = "UPDATE addresses SET street = ?, city = ?, state = ?, postal_code = ?, country = ?, phone = ? WHERE address_id = ?";
        return jdbcTemplate.update(sql,
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getPhone(),
                address.getAddressId());
    }

    @Override
    public int deleteById(Long addressId) {
        String sql = "DELETE FROM addresses WHERE address_id = ?";
        return jdbcTemplate.update(sql, addressId);
    }
}
