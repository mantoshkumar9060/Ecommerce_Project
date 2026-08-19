package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.AuthDtos;
import com.ecommerce.auth.entity.*;
import com.ecommerce.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthDtos.Token register(AuthDtos.Register r) {
        if (users.findByEmailIgnoreCase(r.email()).isPresent())
            throw new IllegalStateException("Email is already registered");
        AppUser u = new AppUser();
        u.setName(r.name());
        u.setEmail(r.email().trim().toLowerCase());
        u.setPasswordHash(encoder.encode(r.password()));
        u = users.save(u);
        return token(u);
    }

    @Transactional
    public AuthDtos.Token login(AuthDtos.Login r) {
        AppUser u = users.findByEmailIgnoreCase(r.email()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!encoder.matches(r.password(), u.getPasswordHash()))
            throw new IllegalArgumentException("Invalid email or password");
        return token(u);
    }

    private AuthDtos.Token token(AppUser u) {
        return new AuthDtos.Token(jwt.create(u), "Bearer", u.getId(), u.getRole().name());
    }

    @Transactional
    public AuthDtos.Profile profile(Long userId) {
        return profileOf(user(userId));
    }

    @Transactional
    public AuthDtos.Profile updateProfile(Long userId, AuthDtos.UpdateProfile request) {
        AppUser user = user(userId);
        user.setName(request.name().trim());
        user.setMobileNumber(blankToNull(request.mobileNumber()));
        user.setDateOfBirth(request.dateOfBirth());
        user.setGender(blankToNull(request.gender()));
        return profileOf(user);
    }

    private AppUser user(Long userId) {
        return users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private AuthDtos.Profile profileOf(AppUser user) {
        return new AuthDtos.Profile(user.getId(), user.getName(), user.getEmail(), user.getMobileNumber(), user.getDateOfBirth(), user.getGender(), user.getRole().name());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
