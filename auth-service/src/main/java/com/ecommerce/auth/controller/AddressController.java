package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.AuthDtos;
import com.ecommerce.auth.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {
    private final AddressService addresses;

    public AddressController(AddressService addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    public List<AuthDtos.Address> list(@RequestHeader("X-User-Id") Long userId) {
        return addresses.list(userId);
    }

    @GetMapping("/{addressId}")
    public AuthDtos.Address get(@RequestHeader("X-User-Id") Long userId, @PathVariable Long addressId) {
        return addresses.get(userId, addressId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.Address create(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody AuthDtos.AddressRequest request) {
        return addresses.create(userId, request);
    }

    @PutMapping("/{addressId}")
    public AuthDtos.Address update(@RequestHeader("X-User-Id") Long userId, @PathVariable Long addressId, @Valid @RequestBody AuthDtos.AddressRequest request) {
        return addresses.update(userId, addressId, request);
    }

    @PatchMapping("/{addressId}/default")
    public AuthDtos.Address setDefault(@RequestHeader("X-User-Id") Long userId, @PathVariable Long addressId) {
        return addresses.setDefault(userId, addressId);
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-User-Id") Long userId, @PathVariable Long addressId) {
        addresses.delete(userId, addressId);
    }
}
