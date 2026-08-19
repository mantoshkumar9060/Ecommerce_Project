package com.ecommerce.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_user_email", columnNames = "email"))
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(length = 20)
    private String mobileNumber;
    private LocalDate dateOfBirth;
    @Column(length = 20)
    private String gender;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        name = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        email = v;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String v) {
        passwordHash = v;
    }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String v) { mobileNumber = v; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate v) { dateOfBirth = v; }
    public String getGender() { return gender; }
    public void setGender(String v) { gender = v; }

    public Role getRole() {
        return role;
    }

    public void setRole(Role v) {
        role = v;
    }
}
