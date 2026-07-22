package com.ashraf.entity;

import com.ashraf.enums.AddressType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "address_normalized")
@Getter
@Setter
public class AddressNormalized {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    private String state;

    private String pincode;

    private String street;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    private String landmark;

    private String floorOrApartment;

    private String country = "India";

    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    private Boolean isDefault = false;

    // In AddressNormalized:
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
