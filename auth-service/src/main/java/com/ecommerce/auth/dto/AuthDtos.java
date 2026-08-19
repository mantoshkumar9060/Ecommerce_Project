package com.ecommerce.auth.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record Register(@NotBlank @Size(max = 120) String name, @Email @NotBlank @Size(max = 254) String email,
                           @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record Login(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record Token(String accessToken, String tokenType, Long userId, String role) {
    }

    public record Profile(Long id, String name, String email, String mobileNumber, LocalDate dateOfBirth, String gender, String role) { }

    public record UpdateProfile(@NotBlank @Size(max = 120) String name,
                                @Pattern(regexp = "^$|^[0-9+() -]{7,20}$", message = "Invalid mobile number") String mobileNumber,
                                @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
                                @Pattern(regexp = "^$|(?i)male|female|non-binary|prefer_not_to_say$", message = "Invalid gender") String gender) { }

    public record Address(Long id, String recipientName, String mobileNumber, String line1, String line2, String landmark,
                          String city, String state, String postalCode, String country, String addressType, boolean defaultAddress) { }

    public record AddressRequest(@NotBlank @Size(max = 120) String recipientName,
                                 @NotBlank @Pattern(regexp = "^(?:\\+91[- ]?)?[6-9]\\d{9}$", message = "Enter a valid Indian mobile number") String mobileNumber,
                                 @NotBlank @Size(max = 160) String line1, @Size(max = 160) String line2,
                                 @Size(max = 120) String landmark,
                                 @NotBlank @Size(max = 80) String city, @NotBlank @Size(max = 80) String state,
                                 @Pattern(regexp = "^\\d{6}$", message = "Pincode must be 6 digits") String postalCode, @NotBlank @Size(max = 80) String country,
                                 @Pattern(regexp = "(?i)HOME|WORK|OTHER", message = "Address type must be HOME, WORK, or OTHER") String addressType, boolean defaultAddress) { }
}
