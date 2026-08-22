package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.AuthDtos;
import com.ecommerce.auth.entity.Address;
import com.ecommerce.auth.entity.AppUser;
import com.ecommerce.auth.repository.AddressRepository;
import com.ecommerce.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {
    private final AddressRepository addresses;
    private final UserRepository users;

    public AddressService(AddressRepository addresses, UserRepository users) {
        this.addresses = addresses;
        this.users = users;
    }

    @Transactional
    public List<AuthDtos.Address> list(Long userId) {
        return addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId).stream().map(this::dto).toList();
    }

    @Transactional
    public AuthDtos.Address get(Long userId, Long addressId) {
        return dto(address(userId, addressId));
    }

    @Transactional
    public AuthDtos.Address create(Long userId, AuthDtos.AddressRequest request) {
        List<Address> existing = addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId);
        boolean makeDefault = request.defaultAddress() || existing.isEmpty();
        if (makeDefault) clearDefault(existing);
        Address address = new Address();
        address.setUser(user(userId));
        copy(request, address);
        address.setDefaultAddress(makeDefault);
        return dto(addresses.save(address));
    }

    @Transactional
    public AuthDtos.Address update(Long userId, Long addressId, AuthDtos.AddressRequest request) {
        Address address = address(userId, addressId);
        if (request.defaultAddress()) {
            clearDefault(addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId));
            address.setDefaultAddress(true);
        }
        copy(request, address);
        return dto(address);
    }

    @Transactional
    public AuthDtos.Address setDefault(Long userId, Long addressId) {
        Address address = address(userId, addressId);
        clearDefault(addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId));
        address.setDefaultAddress(true);
        return dto(address);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = address(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addresses.delete(address);
        if (wasDefault) {
            addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId).stream().findFirst().ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    private void clearDefault(List<Address> addresses) {
        addresses.forEach(address -> address.setDefaultAddress(false));
    }

    private AppUser user(Long userId) {
        return users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Address address(Long userId, Long addressId) {
        return addresses.findByIdAndUserId(addressId, userId).orElseThrow(() -> new IllegalArgumentException("Address not found"));
    }

    private void copy(AuthDtos.AddressRequest request, Address address) {
        address.setRecipientName(request.recipientName().trim());
        address.setMobileNumber(request.mobileNumber().trim());
        address.setLine1(request.line1().trim());
        address.setLine2(blankToNull(request.line2()));
        address.setLandmark(blankToNull(request.landmark()));
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setCountry(request.country().trim());
        address.setAddressType(request.addressType().trim().toUpperCase());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AuthDtos.Address dto(Address address) {
        return new AuthDtos.Address(address.getId(), address.getRecipientName(), address.getMobileNumber(), address.getLine1(), address.getLine2(), address.getLandmark(), address.getCity(), address.getState(), address.getPostalCode(), address.getCountry(), address.getAddressType(), address.isDefaultAddress());
    }
}
