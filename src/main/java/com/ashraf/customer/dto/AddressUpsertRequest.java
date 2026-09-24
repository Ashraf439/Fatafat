package com.ashraf.customer.dto;

import com.ashraf.commerce.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Create/update payload for a saved delivery address. */
public record AddressUpsertRequest(
        @NotBlank @Size(max = 255) String street,
        @Size(max = 255) String landmark,
        @Size(max = 255) String floorOrApartment,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Pincode must be 6 digits") String pincode,
        AddressType addressType,
        Boolean makeDefault
) {}
