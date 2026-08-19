package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.AuthDtos;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.auth.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final AddressService addresses;

    public AuthController(AuthService auth, AddressService addresses) {
        this.auth = auth;
        this.addresses = addresses;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.Token register(@Valid @RequestBody AuthDtos.Register r) {
        return auth.register(r);
    }

    @PostMapping("/login")
    public AuthDtos.Token login(@Valid @RequestBody AuthDtos.Login r) {
        return auth.login(r);
    }

    @GetMapping("/me")
    public AuthDtos.Profile profile(@RequestHeader("X-User-Id") Long userId) {
        return auth.profile(userId);
    }

    @PatchMapping("/me")
    public AuthDtos.Profile updateProfile(@RequestHeader("X-User-Id") Long userId,
                                           @Valid @RequestBody AuthDtos.UpdateProfile request) {
        return auth.updateProfile(userId, request);
    }

    @GetMapping("/addresses")
    public List<AuthDtos.Address> addresses(@RequestHeader("X-User-Id") Long userId) {
        return addresses.list(userId);
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.Address addAddress(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody AuthDtos.AddressRequest request) { return addresses.create(userId, request); }

    @PutMapping("/addresses/{addressId}")
    public AuthDtos.Address updateAddress(@RequestHeader("X-User-Id") Long userId, @PathVariable Long addressId,
                                          @Valid @RequestBody AuthDtos.AddressRequest request) { return addresses.update(userId, addressId, request); }

    @PatchMapping("/addresses/{addressId}/default")
    public AuthDtos.Address setDefaultAddress(@RequestHeader("X-User-Id") Long userId, @PathVariable Long addressId) { return addresses.setDefault(userId, addressId); }

    @DeleteMapping("/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@RequestHeader("X-User-Id") Long userId, @PathVariable Long addressId) { addresses.delete(userId, addressId); }
}
