package com.ashraf.restaurant.core.service;

import com.ashraf.auth.service.EmailService;
import com.ashraf.core.entity.Roles;
import com.ashraf.core.entity.User;
import com.ashraf.core.entity.UserRoles;
import com.ashraf.core.entity.VerificationToken;
import com.ashraf.core.enums.Status;
import com.ashraf.core.repository.RolesRepository;
import com.ashraf.core.repository.TokenRepository;
import com.ashraf.core.repository.UserRepository;
import com.ashraf.core.repository.UserRolesRepository;
import com.ashraf.restaurant.core.dto.RestaurantRegisterRequest;
import com.ashraf.restaurant.onboarding.service.RestaurantOnboardingService;
import com.ashraf.shared.exception.RoleNotFoundException;
import com.ashraf.shared.exception.UserAlreadyExistsException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Restaurant-specific registration. Deliberately kept out of the shared
 * AuthService: registering a restaurant owner isn't just "create a User" —
 * it also has to kick off the onboarding application, which is a
 * restaurant-domain concern, not a generic auth one.
 * Registration itself stays a full-detail signup (name/FSSAI/GSTIN/address/
 * bank all captured up front, per RestaurantRegisterRequest), but no
 * Restaurant entity is created here. That's deferred entirely to
 * RestaurantOnboardingService.confirmPayment(), once an admin has approved
 * the application and payment has cleared. Until then, all business-facing
 * state lives on RestaurantOnboardingApplication, not on User.status —
 * User.status only tracks email verification.
 */
@Service
public class RestaurantAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolesRepository rolesRepository;
    private final UserRolesRepository userRolesRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final RestaurantOnboardingService restaurantOnboardingService;

    public RestaurantAuthService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 RolesRepository rolesRepository,
                                 UserRolesRepository userRolesRepository,
                                 TokenRepository tokenRepository,
                                 EmailService emailService,
                                 RestaurantOnboardingService restaurantOnboardingService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolesRepository = rolesRepository;
        this.userRolesRepository = userRolesRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.restaurantOnboardingService = restaurantOnboardingService;
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

        linkRole(user, "RESTAURANT");

        String tokenStr = UUID.randomUUID().toString();
        tokenRepository.save(new VerificationToken(tokenStr, user));
        emailService.sendVerificationEmail(user.getEmail(), tokenStr);
    }

    private void linkRole(User user, String roleName) {
        Roles role = rolesRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException("Role not seeded: " + roleName));

        UserRoles userRole = new UserRoles();
        userRole.setUser(user);
        userRole.setRole(role);
        userRolesRepository.save(userRole);
    }


}