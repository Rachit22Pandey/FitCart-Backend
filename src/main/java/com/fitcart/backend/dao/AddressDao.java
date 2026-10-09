package com.fitcart.backend.dao;

import com.fitcart.backend.model.Address;

import java.util.List;
import java.util.Optional;

public interface AddressDao {

    Address save(Address address);

    Optional<Address> findById(Long addressId);

    List<Address> findByUserId(Long userId);

    int update(Address address);

    int deleteById(Long addressId);
}
