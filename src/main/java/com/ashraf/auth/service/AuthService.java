package com.ashraf.auth.service;

import com.ashraf.core.entity.*;
import com.ashraf.core.enums.Status;
import com.ashraf.core.repository.RolesRepository;
import com.ashraf.core.repository.TokenRepository;
import com.ashraf.core.repository.UserRepository;
import com.ashraf.core.repository.UserRolesRepository;
import com.ashraf.customer.dto.CustomerRegisterRequest;
import com.ashraf.customer.entity.Customer;
import com.ashraf.customer.repository.CustomerRepository;
import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.commerce.dto.AddressRequest;
import com.ashraf.auth.dto.LoginRequest;
import com.ashraf.customer.entity.CustomerAddress;
import com.ashraf.commerce.repository.CustomerAddressRepository;
import com.ashraf.rider.dto.RiderRegisterRequest;
import com.ashraf.rider.entity.Rider;
import com.ashraf.rider.repository.RiderRepository;
import com.ashraf.shared.exception.RoleNotFoundException;
import com.ashraf.shared.exception.SuspendedAccountException;
import com.ashraf.shared.exception.UserAlreadyExistsException;
import com.ashraf.shared.utils.JWTUtil;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RiderRepository riderRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final RolesRepository rolesRepository;
    private final UserRolesRepository userRolesRepository;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, CustomerRepository customerRepository,
                       RiderRepository riderRepository, CustomerAddressRepository customerAddressRepository,
                       RolesRepository rolesRepository,
                       UserRolesRepository userRolesRepository, JWTUtil jwtUtil,
                       AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService, TokenRepository tokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.riderRepository = riderRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.rolesRepository = rolesRepository;
        this.userRolesRepository = userRolesRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    public LoginResult login(LoginRequest req) {
        String email = req.getEmail();
        String rawPassword = req.getPassword();
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(email, rawPassword);
        Authentication authResult = authenticationManager.authenticate(authRequest);

        CustomUserDetails principal = (CustomUserDetails) authResult.getPrincipal();
        User user = principal.getUser();
        //Block authentication
        if (user.getStatus() == Status.SUSPENDED) {
            throw new SuspendedAccountException("This account has been suspended");
        }

        if (user.getStatus() == Status.PENDING_VERIFICATION) {
            throw new RuntimeException("Please verify your email address before logging in.");
        }
        List<String> roleNames = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();
        String accessToken = issueAccessToken(user, roleNames);
        String rawRefreshToken = refreshTokenService.issueRefreshToken(user);
        return new LoginResult(accessToken, rawRefreshToken, user);
    }

    /**
     * Mints a fresh access token for an already-authenticated user. Shared by
     * login() and the /refresh endpoint, which re-issues an access token off
     * a valid refresh token without re-checking credentials.
     */
    public String issueAccessToken(User user, List<String> roleNames) {
        return jwtUtil.generateToken(user.getId(), user.getEmail(), roleNames, user.getStatus().name());
    }

    @Transactional
    public void registerCustomer(CustomerRegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setStatus(Status.PENDING_VERIFICATION);
        user = userRepository.save(user);

        CustomerAddress address = buildAddress(req.getAddress());

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setName(req.getName());

        address.setCustomer(customer);  // add this — links the reverse collection
        address = customerAddressRepository.save(address);
        customer.setAddress(address);
        Customer savedCustomer = customerRepository.save(customer);

        String tokenStr = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(tokenStr, savedCustomer.getUser());
        tokenRepository.save(verificationToken);

        // 3. Dispatch the verification link
        emailService.sendVerificationEmail(savedCustomer.getUser().getEmail(), tokenStr);

        linkRole(user, "CUSTOMER");
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

        CustomerAddress address = customerAddressRepository.save(buildAddress(req.getAddress()));

        Rider rider = new Rider();
        rider.setUser(user);
        rider.setName(req.getName());
        rider.setDrivingLicenseNumber(req.getDrivingLicenseNumber());
        rider.setNationalId(req.getNationalId());
        rider.setVehicleType(req.getVehicleType());
        rider.setVehicleNumber(req.getVehicleNumber());
        rider.setVehicleModel(req.getVehicleModel());
        rider.setVehicleColor(req.getVehicleColor());
        Rider savedRider = riderRepository.save(rider);

        String tokenStr = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(tokenStr, savedRider.getUser());
        tokenRepository.save(verificationToken);

        // 3. Dispatch the verification link
        emailService.sendVerificationEmail(savedRider.getUser().getEmail(), tokenStr);

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

    private CustomerAddress buildAddress(AddressRequest req) {
        CustomerAddress address = new CustomerAddress();
        address.setCity(req.getCity());
        address.setState(req.getState());
        address.setPincode(req.getPincode());
        address.setStreet(req.getStreet());
        return address;
    }

    @Transactional
    public void verifyEmailToken(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Error: Invalid verification link."));

        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            throw new RuntimeException("Error: Verification link has expired.");
        }

        User user = verificationToken.getUser();
        user.setStatus(Status.ACTIVE); // Activate user account status
        userRepository.save(user);

        tokenRepository.delete(verificationToken); // Clean up verification token record
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Already verified — nothing to resend. Caller gets the same
            // generic response regardless, so this branch is invisible externally.
            if (user.getStatus() != Status.PENDING_VERIFICATION) {
                return;
            }

            // Invalidate any existing token(s) for this user before issuing a new one,
            // so only one valid verification link exists at a time.
            tokenRepository.deleteByUser(user);

            String tokenStr = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken(tokenStr, user);
            tokenRepository.save(verificationToken);

            emailService.sendVerificationEmail(user.getEmail(), tokenStr);
        });
        // No else branch: unknown email -> silently no-op. Controller always
        // returns the same generic message either way.
    }
}