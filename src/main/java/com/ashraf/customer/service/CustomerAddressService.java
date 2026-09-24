package com.ashraf.customer.service;

import com.ashraf.commerce.enums.AddressType;
import com.ashraf.commerce.repository.CustomerAddressRepository;
import com.ashraf.core.entity.User;
import com.ashraf.customer.dto.AddressResponse;
import com.ashraf.customer.dto.AddressUpsertRequest;
import com.ashraf.customer.entity.Customer;
import com.ashraf.customer.entity.CustomerAddress;
import com.ashraf.customer.repository.CustomerRepository;
import com.ashraf.shared.exception.BusinessRuleException;
import com.ashraf.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Saved delivery addresses. Invariants kept here:
 * exactly one active default per customer, at least one active address, and
 * Customer.address (legacy "primary" FK) always points at the default.
 * Deleting archives the row because past orders reference it.
 */
@Service
public class CustomerAddressService {

    static final int MAX_ADDRESSES_PER_CUSTOMER = 20;

    private final CustomerAddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final CustomerLookupService customerLookup;

    public CustomerAddressService(CustomerAddressRepository addressRepository,
                                  CustomerRepository customerRepository,
                                  CustomerLookupService customerLookup) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.customerLookup = customerLookup;
    }

    @Transactional
    public List<AddressResponse> list(User user) {
        Customer customer = customerLookup.requireCustomer(user);
        return loadNormalized(customer).stream().map(AddressResponse::new).toList();
    }

    @Transactional
    public AddressResponse create(User user, AddressUpsertRequest req) {
        Customer customer = customerLookup.requireCustomer(user);
        List<CustomerAddress> active = loadNormalized(customer);
        if (active.size() >= MAX_ADDRESSES_PER_CUSTOMER) {
            throw new BusinessRuleException(
                    "You can save up to " + MAX_ADDRESSES_PER_CUSTOMER + " addresses. Remove one first.");
        }

        CustomerAddress address = new CustomerAddress();
        apply(address, req);
        address.setCustomer(customer);
        address.setArchived(false);
        address.setIsDefault(false);
        address = addressRepository.save(address);

        if (active.isEmpty() || Boolean.TRUE.equals(req.makeDefault())) {
            active.add(address);
            makeDefault(customer, address, active);
        }
        return new AddressResponse(address);
    }

    @Transactional
    public AddressResponse update(User user, Long addressId, AddressUpsertRequest req) {
        Customer customer = customerLookup.requireCustomer(user);
        List<CustomerAddress> active = loadNormalized(customer);
        CustomerAddress address = findIn(active, addressId);

        apply(address, req);
        if (Boolean.TRUE.equals(req.makeDefault())) {
            makeDefault(customer, address, active);
        }
        return new AddressResponse(address);
    }

    @Transactional
    public AddressResponse setDefault(User user, Long addressId) {
        Customer customer = customerLookup.requireCustomer(user);
        List<CustomerAddress> active = loadNormalized(customer);
        CustomerAddress address = findIn(active, addressId);
        makeDefault(customer, address, active);
        return new AddressResponse(address);
    }

    @Transactional
    public void archive(User user, Long addressId) {
        Customer customer = customerLookup.requireCustomer(user);
        List<CustomerAddress> active = loadNormalized(customer);
        CustomerAddress address = findIn(active, addressId);

        if (active.size() <= 1) {
            throw new BusinessRuleException("You need at least one saved address.");
        }

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        address.setArchived(true);
        address.setIsDefault(false);
        active.remove(address);

        if (wasDefault) {
            // Most recently added remaining address becomes the default (list is ordered).
            makeDefault(customer, active.getFirst(), active);
        }
    }

    /** Address that belongs to this customer and is still active, else 404. */
    @Transactional(readOnly = true)
    public CustomerAddress requireOwnedAddress(Customer customer, Long addressId) {
        return addressRepository.findActiveByIdAndCustomerId(addressId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery address not found"));
    }

    // ---- internals -----------------------------------------------------------------------

    /**
     * Loads active addresses and repairs legacy data: the address created at registration may
     * not be linked back to its customer or flagged as default. Idempotent and cheap.
     */
    private List<CustomerAddress> loadNormalized(Customer customer) {
        CustomerAddress primary = customer.getAddress();
        if (primary != null && primary.getCustomer() == null) {
            primary.setCustomer(customer);
            addressRepository.saveAndFlush(primary);
        }

        List<CustomerAddress> active = new java.util.ArrayList<>(
                addressRepository.findActiveByCustomerId(customer.getId()));

        if (!active.isEmpty() && active.stream().noneMatch(a -> Boolean.TRUE.equals(a.getIsDefault()))) {
            CustomerAddress chosen = active.stream()
                    .filter(a -> primary != null && a.getId().equals(primary.getId()))
                    .findFirst()
                    .orElse(active.getFirst());
            makeDefault(customer, chosen, active);
            active.sort((a, b) -> Boolean.compare(
                    Boolean.TRUE.equals(b.getIsDefault()), Boolean.TRUE.equals(a.getIsDefault())));
        }
        return active;
    }

    private CustomerAddress findIn(List<CustomerAddress> active, Long addressId) {
        return active.stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    private void makeDefault(Customer customer, CustomerAddress chosen, List<CustomerAddress> active) {
        for (CustomerAddress a : active) {
            a.setIsDefault(a.getId().equals(chosen.getId()));
        }
        customer.setAddress(chosen);
        customerRepository.save(customer);
    }

    private void apply(CustomerAddress a, AddressUpsertRequest req) {
        a.setStreet(req.street().trim());
        a.setLandmark(blankToNull(req.landmark()));
        a.setFloorOrApartment(blankToNull(req.floorOrApartment()));
        a.setCity(req.city().trim());
        a.setState(req.state().trim());
        a.setPincode(req.pincode().trim());
        a.setAddressType(req.addressType() != null ? req.addressType() : AddressType.HOME);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
