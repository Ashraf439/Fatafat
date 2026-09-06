package com.ashraf.restaurant.staff.service;

import com.ashraf.auth.service.EmailService;
import com.ashraf.core.entity.*;
import com.ashraf.core.enums.PermissionEffect;
import com.ashraf.core.enums.Status;
import com.ashraf.core.repository.*;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.restaurant.core.service.RestaurantAccessService;
import com.ashraf.restaurant.staff.dto.StaffAddRequest;
import com.ashraf.restaurant.staff.dto.StaffPermissionRequest;
import com.ashraf.restaurant.staff.dto.StaffResponse;
import com.ashraf.restaurant.staff.entity.RestaurantStaff;
import com.ashraf.restaurant.staff.enums.StaffStatus;
import com.ashraf.restaurant.staff.repository.RestaurantStaffRepository;
import com.ashraf.shared.exception.*;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RestaurantStaffService {

    private static final Set<String> ASSIGNABLE_ROLES =
            Set.of("RESTAURANT_MANAGER", "RESTAURANT_STAFF", "RESTAURANT_CASHIER");

    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final UserRolesRepository userRolesRepository;
    private final UserPermissionsRepository userPermissionsRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantAccessService restaurantAccessService;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final PermissionRepository permissionRepository;

    public RestaurantStaffService(UserRepository userRepository,
                                  RolesRepository rolesRepository,
                                  UserRolesRepository userRolesRepository,
                                  UserPermissionsRepository userPermissionsRepository,
                                  RestaurantStaffRepository restaurantStaffRepository,
                                  RestaurantRepository restaurantRepository,
                                  RestaurantAccessService restaurantAccessService,
                                  PasswordEncoder passwordEncoder,
                                  TokenRepository tokenRepository,
                                  EmailService emailService,
                                  PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.rolesRepository = rolesRepository;
        this.userRolesRepository = userRolesRepository;
        this.userPermissionsRepository = userPermissionsRepository;
        this.restaurantStaffRepository = restaurantStaffRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantAccessService = restaurantAccessService;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public void addStaff(User owner, StaffAddRequest request) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(owner);

        if (!ASSIGNABLE_ROLES.contains(request.getRole())) {
            throw new IllegalArgumentException("Not an assignable staff role: " + request.getRole());
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new UserDoesNotExistException("Restaurant does not exist."));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(Status.PENDING_VERIFICATION);
        userRepository.save(user);

        Roles role = rolesRepository.findByName(request.getRole())
                .orElseThrow(() -> new RoleNotFoundException("Role not seeded: " + request.getRole()));
        UserRoles userRole = new UserRoles();
        userRole.setUser(user);
        userRole.setRole(role);
        userRolesRepository.save(userRole);

        RestaurantStaff restaurantStaff = new RestaurantStaff();
        restaurantStaff.setRestaurant(restaurant);
        restaurantStaff.setUser(user);
        restaurantStaff.setStatus(StaffStatus.ACTIVE);
        restaurantStaffRepository.save(restaurantStaff);

        String token = UUID.randomUUID().toString();
        tokenRepository.save(new VerificationToken(token, user));
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public List<StaffResponse> listStaff(User requester) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(requester);
        return restaurantStaffRepository.findByRestaurant_Id(restaurantId).stream()
                .map(staff -> new StaffResponse(
                        staff.getUser().getId(),
                        staff.getUser().getEmail(),
                        resolveRoleName(staff.getUser()),
                        staff.getStatus()))
                .toList();
    }

    @Transactional
    public void removeStaff(User owner, Long staffUserId) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(owner);
        RestaurantStaff staffRow = restaurantStaffRepository
                .findByRestaurant_IdAndUser_Id(restaurantId, staffUserId)
                .orElseThrow(() -> new UserDoesNotExistException("Staff not found"));

        staffRow.setStatus(StaffStatus.REMOVED);
        restaurantStaffRepository.save(staffRow);
        userRolesRepository.deleteByUser_Id(staffUserId);
    }

    @Transactional
    public void updateStaffPermission(User owner, Long staffUserId, StaffPermissionRequest request) {
        Long restaurantId = restaurantAccessService.resolveRestaurantId(owner);

        User targetUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new UserDoesNotExistException("Staff not found"));
        restaurantAccessService.assertBelongs(targetUser, restaurantId);

        Permissions permission = permissionRepository.findByName(request.getPermissionName())
                .orElseThrow(() -> new PermissionNotFoundException(
                        "Permission not found: " + request.getPermissionName()));

        UserPermissionId id = new UserPermissionId(staffUserId, permission.getId());
        UserPermissions userPermission = userPermissionsRepository.findById(id)
                .orElseGet(UserPermissions::new);
        userPermission.setId(id);
        userPermission.setUser(targetUser);
        userPermission.setPermission(permission);
        userPermission.setEffect(request.getEffect() == null
                ? PermissionEffect.GRANT
                : request.getEffect());

        userPermissionsRepository.save(userPermission);
    }

    private String resolveRoleName(User user) {
        return user.getUserRoles().stream()
                .findFirst()
                .map(ur -> ur.getRole().getName())
                .orElse(null);
    }
}