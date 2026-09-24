package com.ashraf.customer.controller;

import com.ashraf.customer.dto.AddressResponse;
import com.ashraf.customer.dto.AddressUpsertRequest;
import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.customer.service.CustomerAddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/customer/addresses")
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    public CustomerAddressController(CustomerAddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(addressService.list(principal.getUser()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @Valid @RequestBody AddressUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(principal.getUser(), request));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> update(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @PathVariable("addressId") Long addressId,
                                                  @Valid @RequestBody AddressUpsertRequest request) {
        return ResponseEntity.ok(addressService.update(principal.getUser(), addressId, request));
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<AddressResponse> setDefault(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @PathVariable("addressId") Long addressId) {
        return ResponseEntity.ok(addressService.setDefault(principal.getUser(), addressId));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal,
                                       @PathVariable("addressId") Long addressId) {
        addressService.archive(principal.getUser(), addressId);
        return ResponseEntity.noContent().build();
    }
}
