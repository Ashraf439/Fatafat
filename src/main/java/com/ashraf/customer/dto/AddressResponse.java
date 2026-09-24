package com.ashraf.customer.dto;

import com.ashraf.commerce.enums.AddressType;
import com.ashraf.customer.entity.CustomerAddress;
import lombok.Getter;

@Getter
public class AddressResponse {
    private final Long id;
    private final AddressType addressType;
    private final String street;
    private final String landmark;
    private final String floorOrApartment;
    private final String city;
    private final String state;
    private final String pincode;
    private final Boolean isDefault;

    public AddressResponse(CustomerAddress a) {
        this.id = a.getId();
        this.addressType = a.getAddressType();
        this.street = a.getStreet();
        this.landmark = a.getLandmark();
        this.floorOrApartment = a.getFloorOrApartment();
        this.city = a.getCity();
        this.state = a.getState();
        this.pincode = a.getPincode();
        this.isDefault = Boolean.TRUE.equals(a.getIsDefault());
    }
}
