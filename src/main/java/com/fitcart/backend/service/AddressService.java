package com.fitcart.backend.service;

import com.fitcart.backend.dao.AddressDao;
import com.fitcart.backend.dao.UserDao;
import com.fitcart.backend.dto.AddressRequest;
import com.fitcart.backend.exception.ResourceNotFoundException;
import com.fitcart.backend.model.Address;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressDao addressDao;
    private final UserDao userDao;

    public AddressService(AddressDao addressDao, UserDao userDao) {
        this.addressDao = addressDao;
        this.userDao = userDao;
    }

    public Address createAddress(AddressRequest request) {
        userDao.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Address address = new Address();
        address.setUserId(request.getUserId());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setPhone(request.getPhone());

        return addressDao.save(address);
    }

    public List<Address> getAddressesByUserId(Long userId) {
        return addressDao.findByUserId(userId);
    }

    public Address getAddressById(Long addressId) {
        return addressDao.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
    }

    public Address updateAddress(Long addressId, AddressRequest request) {
        Address existing = getAddressById(addressId);
        existing.setStreet(request.getStreet());
        existing.setCity(request.getCity());
        existing.setState(request.getState());
        existing.setPostalCode(request.getPostalCode());
        existing.setCountry(request.getCountry());
        existing.setPhone(request.getPhone());

        addressDao.update(existing);
        return existing;
    }

    public void deleteAddress(Long addressId) {
        getAddressById(addressId);
        addressDao.deleteById(addressId);
    }
}
