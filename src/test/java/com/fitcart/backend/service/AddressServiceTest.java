package com.fitcart.backend.service;

import com.fitcart.backend.dao.AddressDao;
import com.fitcart.backend.dao.UserDao;
import com.fitcart.backend.dto.AddressRequest;
import com.fitcart.backend.exception.ResourceNotFoundException;
import com.fitcart.backend.model.Address;
import com.fitcart.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressDao addressDao;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private AddressService addressService;

    private User user;
    private Address address;

    @BeforeEach
    void setUp() {
        user = new User(10L, "John", "john@fit.com", "pass", "BUYER", "12345", "2026-10-09");
        address = new Address(1L, 10L, "123 Fitness St", "FitCity", "FitState", "110001", "India", "9876543210");
    }

    @Test
    void testCreateAddress_Success() {
        AddressRequest req = new AddressRequest(10L, "123 Fitness St", "FitCity", "FitState", "110001", "India", "9876543210");
        when(userDao.findById(10L)).thenReturn(Optional.of(user));
        when(addressDao.save(any(Address.class))).thenReturn(address);

        Address result = addressService.createAddress(req);

        assertNotNull(result);
        assertEquals("FitCity", result.getCity());
        verify(addressDao).save(any(Address.class));
    }

    @Test
    void testCreateAddress_UserNotFound() {
        AddressRequest req = new AddressRequest(99L, "123 St", "City", "State", "110001", "India", "123");
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.createAddress(req));
    }

    @Test
    void testGetAddressesByUserId() {
        when(addressDao.findByUserId(10L)).thenReturn(Collections.singletonList(address));

        List<Address> list = addressService.getAddressesByUserId(10L);

        assertEquals(1, list.size());
        verify(addressDao).findByUserId(10L);
    }
}
