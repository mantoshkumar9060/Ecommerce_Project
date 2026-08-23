package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.AuthDtos;
import com.ecommerce.auth.entity.AppUser;
import com.ecommerce.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AuthService service = new AuthService(users, encoder, jwt);

    @Test
    void registerNormalizesEmailHashesPasswordAndReturnsToken() {
        AuthDtos.Register request = new AuthDtos.Register("Asha", " Asha@Example.Com ", "password1");
        when(users.findByEmailIgnoreCase(request.email())).thenReturn(Optional.empty());
        when(encoder.encode("password1")).thenReturn("hash");
        when(users.save(any(AppUser.class))).thenAnswer(call -> {
            AppUser user = call.getArgument(0);
            setId(user, 4L);
            return user;
        });
        when(jwt.create(any(AppUser.class))).thenReturn("jwt");

        AuthDtos.Token token = service.register(request);

        assertEquals("jwt", token.accessToken());
        assertEquals("Bearer", token.tokenType());
        assertEquals(4L, token.userId());
        verify(encoder).encode("password1");
    }

    @Test
    void registerRejectsExistingEmailWithoutSaving() {
        when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(new AppUser()));
        assertThrows(IllegalStateException.class, () -> service.register(new AuthDtos.Register("A", "a@b.com", "password1")));
        verify(users, never()).save(any());
    }

    @Test
    void loginRejectsUnknownAndWrongPassword() {
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.login(new AuthDtos.Login("missing@example.com", "password1")));
        AppUser user = user(3L, "hash");
        when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong", "hash")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.login(new AuthDtos.Login("a@b.com", "wrong")));
        verify(jwt, never()).create(any());
    }

    @Test
    void updateProfileTrimsOptionalFieldsAndKeepsRole() {
        AppUser user = user(7L, "hash");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        AuthDtos.Profile profile = service.updateProfile(7L, new AuthDtos.UpdateProfile("  Asha  ", "  ", null, " female "));
        assertEquals("Asha", profile.name());
        assertNull(profile.mobileNumber());
        assertEquals("female", profile.gender());
        assertEquals("CUSTOMER", profile.role());
    }

    private static AppUser user(long id, String hash) {
        AppUser user = new AppUser();
        setId(user, id);
        user.setName("Asha");
        user.setEmail("a@b.com");
        user.setPasswordHash(hash);
        return user;
    }

    private static void setId(AppUser user, long id) {
        try {
            var field = AppUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
