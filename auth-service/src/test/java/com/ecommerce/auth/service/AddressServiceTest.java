package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.AuthDtos;
import com.ecommerce.auth.entity.Address;
import com.ecommerce.auth.entity.AppUser;
import com.ecommerce.auth.repository.AddressRepository;
import com.ecommerce.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddressServiceTest {
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AddressService service = new AddressService(addresses, users);

    @Test
    void firstAddressBecomesDefaultAndNormalizesFields() {
        AppUser user = new AppUser();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(addresses.findByUserIdOrderByDefaultAddressDescIdDesc(1L)).thenReturn(List.of());
        when(addresses.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));
        var result = service.create(1L, request(false));
        assertTrue(result.defaultAddress());
        assertEquals("HOME", result.addressType());
        assertNull(result.line2());
    }

    @Test
    void explicitDefaultClearsPreviousDefaults() {
        Address old = address(2L, true);
        AppUser user = new AppUser();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(addresses.findByUserIdOrderByDefaultAddressDescIdDesc(1L)).thenReturn(List.of(old));
        when(addresses.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));
        var result = service.create(1L, request(true));
        assertFalse(old.isDefaultAddress());
        assertTrue(result.defaultAddress());
    }

    @Test
    void deletingDefaultPromotesNextAddress() {
        Address defaultAddress = address(1L, true);
        Address next = address(2L, false);
        when(addresses.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(defaultAddress));
        when(addresses.findByUserIdOrderByDefaultAddressDescIdDesc(1L)).thenReturn(List.of(next));
        service.delete(1L, 9L);
        verify(addresses).delete(defaultAddress);
        assertTrue(next.isDefaultAddress());
    }

    private static AuthDtos.AddressRequest request(boolean defaultAddress) {
        return new AuthDtos.AddressRequest(" Asha ", "9876543210", " Main road ", " ", "", " Delhi ", " Delhi ", "110001", " India ", "home", defaultAddress);
    }

    private static Address address(long id, boolean defaultAddress) {
        Address address = new Address();
        try {
            var f = Address.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(address, id);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        address.setDefaultAddress(defaultAddress);
        return address;
    }
}
