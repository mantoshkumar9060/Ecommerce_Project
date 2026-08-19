package com.ecommerce.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_addresses", indexes = @Index(name = "idx_address_user", columnList = "user_id"))
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 120)
    private String recipientName;
    @Column(nullable = false, length = 20)
    private String mobileNumber;
    @Column(nullable = false, length = 160)
    private String line1;
    @Column(length = 160)
    private String line2;
    @Column(length = 120)
    private String landmark;
    @Column(nullable = false, length = 80)
    private String city;
    @Column(nullable = false, length = 80)
    private String state;
    @Column(nullable = false, length = 20)
    private String postalCode;
    @Column(nullable = false, length = 80)
    private String country = "India";
    @Column(nullable = false, length = 12)
    private String addressType = "HOME";
    @Column(nullable = false)
    private boolean defaultAddress;

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; }
    public String getLine2() { return line2; }
    public void setLine2(String line2) { this.line2 = line2; }
    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getAddressType() { return addressType; }
    public void setAddressType(String addressType) { this.addressType = addressType; }
    public boolean isDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean defaultAddress) { this.defaultAddress = defaultAddress; }
}
