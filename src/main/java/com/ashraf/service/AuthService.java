package com.ashraf.service;

import com.ashraf.dto.LoginRequest;
import com.ashraf.dto.LoginResponse;
import com.ashraf.dto.RegisterRequest;
import com.ashraf.entity.*;
import com.ashraf.enums.Role;
import com.ashraf.enums.Status;
import com.ashraf.exception.AccountNotActiveException;
import com.ashraf.exception.InvalidCredentialsException;
import com.ashraf.exception.UserAlreadyExistsException;
import com.ashraf.repository.*;
import com.ashraf.utils.JWTUtil;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RiderRepository riderRepository;
    private final AddressRepository addressRepository;
    private final RestaurantRepository restaurantRepository;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, CustomerRepository customerRepository, RiderRepository riderRepository, AddressRepository addressRepository, RestaurantRepository restaurantRepository, JWTUtil jwtUtil, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.riderRepository = riderRepository;
        this.addressRepository = addressRepository;
        this.restaurantRepository = restaurantRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest req) {
        if(req.getRole() == Role.ADMIN || req.getRole() == Role.SUPER_ADMIN) {
            throw  new IllegalArgumentException("Admins account can't self register");
        }
        if(userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPhoneNumber(req.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        // Customer is activated immediately, but for restaurant and rider need approval
        user.setStatus(req.getRole() == Role.CUSTOMER ? Status.ACTIVE : Status.PENDING_VERIFICATION);
        user = userRepository.save(user);
        switch (req.getRole()) {
            case CUSTOMER -> {
                AddressNormalized address = buildAddress(req);
                address = addressRepository.save(address);

                Customer customer = new Customer();
                customer.setUser(user);
                customer.setName(req.getName());
                customer.setAddress(address);
                customerRepository.save(customer);
            }
            case RESTAURANT -> {
                AddressNormalized address = buildAddress(req);
                address = addressRepository.save(address);

                Restaurant restaurant = new Restaurant();
                restaurant.setOwnerUser(user);
                restaurant.setName(req.getName());
                restaurant.setAddress(address);
                restaurant.setFssaiLicense(req.getFssaiLicense());
                restaurant.setIsOpen(Boolean.FALSE);
                restaurantRepository.save(restaurant);
            }
            case RIDER -> {
                AddressNormalized address = buildAddress(req);
                address = addressRepository.save(address);

                Rider rider = new Rider();
                rider.setUser(user);
                rider.setName(req.getName());
                rider.setVehicleType(req.getVehicleType());
                rider.setLicenseNumber(req.getLicenseNumber());
                rider.setIsAvailable(Boolean.FALSE);
                riderRepository.save(rider);
            }
            default -> throw new IllegalArgumentException("Unsupported role for registration.");
        }
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if(user.getStatus() == Status.SUSPENDED) {
            throw new AccountNotActiveException("Account has been suspended");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(),req.getPassword())
            );
        }catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return new LoginResponse(token, user.getRole().name(), user.getStatus().name(), user.getId());
    }

    private AddressNormalized buildAddress(RegisterRequest req) {
        AddressNormalized address = new AddressNormalized();
        address.setCity(req.getCity());
        address.setState(req.getState());
        address.setPincode(req.getPincode());
        address.setStreet(req.getStreet());
        address.setLatitude(java.math.BigDecimal.valueOf(req.getLatitude()));
        address.setLongitude(java.math.BigDecimal.valueOf(req.getLongitude()));
        return address;
    }
}
