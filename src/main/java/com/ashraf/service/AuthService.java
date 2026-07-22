package com.ashraf.service;

import com.ashraf.dto.*;
import com.ashraf.entity.*;
import com.ashraf.enums.Status;
import com.ashraf.exception.RoleNotFoundException;
import com.ashraf.exception.SuspendedAccountException;
import com.ashraf.exception.UserAlreadyExistsException;
import com.ashraf.repository.*;
import com.ashraf.security.CustomUserDetails;
import com.ashraf.utils.JWTUtil;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RiderRepository riderRepository;
    private final AddressRepository addressRepository;
    private final RestaurantRepository restaurantRepository;
    private final RolesRepository rolesRepository;
    private final UserRolesRepository userRolesRepository;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, CustomerRepository customerRepository,
                       RiderRepository riderRepository, AddressRepository addressRepository,
                       RestaurantRepository restaurantRepository, RolesRepository rolesRepository,
                       UserRolesRepository userRolesRepository, JWTUtil jwtUtil,
                       AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.riderRepository = riderRepository;
        this.addressRepository = addressRepository;
        this.restaurantRepository = restaurantRepository;
        this.rolesRepository = rolesRepository;
        this.userRolesRepository = userRolesRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    public LoginResult login(LoginRequest req) {
        String email = req.getEmail();
        String rawPassword = req.getPassword();
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(email, rawPassword);
        Authentication authResult = authenticationManager.authenticate(authRequest);

        CustomUserDetails principal = (CustomUserDetails) authResult.getPrincipal();
        User user = principal.getUser();
        if (user.getStatus() == Status.SUSPENDED) {
            throw new SuspendedAccountException("This account has been suspended");
        }
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail());
        String rawRefreshToken = refreshTokenService.issueRefreshToken(user);
        return new LoginResult(accessToken, rawRefreshToken, user);
    }

    @Transactional
    public void registerCustomer(CustomerRegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setStatus(Status.ACTIVE);
        user = userRepository.save(user);

        AddressNormalized address = addressRepository.save(buildAddress(req.getAddress()));

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setName(req.getName());
        customer.setAddress(address);
        customerRepository.save(customer);

        linkRole(user, "CUSTOMER");
    }

    @Transactional
    public void registerRestaurant(RestaurantRegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setStatus(Status.PENDING_VERIFICATION);
        user = userRepository.save(user);

        AddressNormalized address = addressRepository.save(buildAddress(req.getAddress()));

        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUser(user);
        restaurant.setName(req.getName());
        restaurant.setFssaiLicense(req.getFssaiLicense());
        restaurant.setGstin(req.getGstin());
        restaurant.setAddress(address);
        restaurant.setBankDetails(buildBankDetails(req.getBankDetails(), restaurant));
        restaurantRepository.save(restaurant);

        linkRole(user, "RESTAURANT");
    }

    @Transactional
    public void registerRider(RiderRegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setStatus(Status.PENDING_VERIFICATION);
        user = userRepository.save(user);

        AddressNormalized address = addressRepository.save(buildAddress(req.getAddress()));

        Rider rider = new Rider();
        rider.setUser(user);
        rider.setName(req.getName());
        rider.setDrivingLicenseNumber(req.getDrivingLicenseNumber());
        rider.setNationalId(req.getNationalId());
        rider.setVehicleType(req.getVehicleType());
        rider.setVehicleNumber(req.getVehicleNumber());
        rider.setVehicleModel(req.getVehicleModel());
        rider.setVehicleColor(req.getVehicleColor());
        riderRepository.save(rider);

        linkRole(user, "RIDER");
    }

    /**
     * Creates the UserRoles link row for a newly registered user.
     * Every registration path must call this — without it, the user has
     * no entry in the RBAC system at all, regardless of what role-specific
     * entity (Customer/Restaurant/Rider) was created for them.
     */
    private void linkRole(User user, String roleName) {
        Roles role = rolesRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException("Role not seeded: " + roleName));

        UserRoles userRole = new UserRoles();
        userRole.setUser(user);
        userRole.setRole(role);
        userRolesRepository.save(userRole);
    }

    private AddressNormalized buildAddress(AddressRequest req) {
        AddressNormalized address = new AddressNormalized();
        address.setCity(req.getCity());
        address.setState(req.getState());
        address.setPincode(req.getPincode());
        address.setStreet(req.getStreet());
        return address;
    }

    private BankDetails buildBankDetails(BankRequest req, Restaurant restaurant) {
        BankDetails bankDetails = new BankDetails();
        bankDetails.setRestaurant(restaurant);
        bankDetails.setAccountHolderName(req.getAccountHolderName());
        bankDetails.setAccountNumber(req.getAccountNumber()); // TODO: encrypt at rest
        bankDetails.setIfscCode(req.getIfscCode());
        bankDetails.setBankName(req.getBankName());
        return bankDetails;
    }
}