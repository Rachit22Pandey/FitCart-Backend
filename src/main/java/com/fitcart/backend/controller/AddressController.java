
package com.fitcart.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fitcart.backend.dto.AddressRequest;
import com.fitcart.backend.model.Address;
import com.fitcart.backend.service.AddressService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    @PreAuthorize("@resourceAuthorizationService.canAccessUser(#p0.userId, authentication)")
    public ResponseEntity<Address> createAddress(
            @Valid @RequestBody AddressRequest request) {
        Address created = addressService.createAddress(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@resourceAuthorizationService.canAccessUser(#p0, authentication)")
    public ResponseEntity<List<Address>> getAddressesByUserId(
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(
                addressService.getAddressesByUserId(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@resourceAuthorizationService.canManageAddress(#p0, authentication)")
    public ResponseEntity<Address> getAddressById(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(addressService.getAddressById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@resourceAuthorizationService.canManageAddress(#p0, authentication)")
    public ResponseEntity<Address> updateAddress(
            @PathVariable("id") Long id,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(
                addressService.updateAddress(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@resourceAuthorizationService.canManageAddress(#p0, authentication)")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable("id") Long id) {
        addressService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }
}
