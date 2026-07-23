package com.ashraf.config;

import com.ashraf.entity.*;
import com.ashraf.enums.Status;
import com.ashraf.exception.PermissionNotFoundException;
import com.ashraf.exception.RoleNotFoundException;
import com.ashraf.repository.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final RolesRepository rolesRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionsRepository rolePermissionsRepository;
    private final UserRolesRepository userRolesRepository;

    @Value("${admin.seed.email}")
    private  String email;

    @Value("${admin.seed.password}")
    private String password;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder encoder, RolesRepository rolesRepository, PermissionRepository permissionRepository, RolePermissionsRepository rolePermissionsRepository, UserRolesRepository userRolesRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.rolesRepository = rolesRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionsRepository = rolePermissionsRepository;
        this.userRolesRepository = userRolesRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
        seedPermissions();
        seedRolePermissions();
        seedSuperAdmin();
    }

    private void seedSuperAdmin() {
        if (userRepository.findByEmail(email).isPresent()) {
            return; // seed email already exists — don't attempt to insert again
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
        Roles roles = rolesRepository.findByName("SUPER_ADMIN").orElseThrow();
        UserRoles userRoles = new UserRoles();
        userRoles.setUser(user);
        userRoles.setRole(roles);
        userRolesRepository.save(userRoles);
    }

    private void seedRolePermissions() throws RoleNotFoundException {
        Map<String,List<String>> mapRolePermissions = new HashMap<>();
        mapRolePermissions.put("CUSTOMER", List.of("RATING_ADD", "COMPLAINT_ADD"));
        mapRolePermissions.put("RESTAURANT",List.of("MENU_ITEM_ADD", "MENU_ITEM_REMOVE", "ORDER_ACCEPT", "ORDER_CANCEL"));
        mapRolePermissions.put("RIDER",List.of( "DELIVERY_ACCEPT", "DELIVERY_CANCEL"));
        mapRolePermissions.put("ADMIN",List.of("RESTAURANT_APPROVE", "RIDER_APPROVE","RESTAURANT_REMOVE","RIDER_REMOVE"));
        mapRolePermissions.put("SUPER_ADMIN",List.of("ROLE_ASSIGN", "ADMIN_ADD","ADMIN_REMOVE"));
        for(Map.Entry<String, List<String>> entry: mapRolePermissions.entrySet()) {
            String role = entry.getKey();
            List<String> permissions = entry.getValue();
            Roles r = rolesRepository.findByName(role).orElseThrow(()-> new RoleNotFoundException(String.format("Role not found: %s",role)));
            for(String permission:permissions) {
                Permissions p = permissionRepository.findByName(permission).orElseThrow(()->new PermissionNotFoundException(String.format("Permission not found: %s",permission)));
                RolePermissionId rolePermissionId = new RolePermissionId(r.getId(),p.getId());
                if(!rolePermissionsRepository.existsById(rolePermissionId)) {
                    RolePermissions rolePermissions = new RolePermissions();
                    rolePermissions.setRole(r);
                    rolePermissions.setPermission(p);
                    rolePermissionsRepository.save(rolePermissions);
                }
            }
        }

    }

    private void seedPermissions() {
        List<String> permissions = List.of("RATING_ADD", "COMPLAINT_ADD", "DELIVERY_ACCEPT", "DELIVERY_CANCEL",
                "MENU_ITEM_ADD", "MENU_ITEM_REMOVE", "ORDER_ACCEPT", "ORDER_CANCEL",
                "RESTAURANT_APPROVE", "RIDER_APPROVE","RESTAURANT_REMOVE","RIDER_REMOVE", "ROLE_ASSIGN", "ADMIN_ADD","ADMIN_REMOVE");
        for(String permission:permissions) {
            if(!permissionRepository.existsByName(permission)) {
                Permissions p = new Permissions();
                p.setName(permission);
                permissionRepository.save(p);
            }
        }
    }


    private void seedRoles() {
        List<String> roles =  List.of("CUSTOMER", "RESTAURANT", "RIDER", "ADMIN", "SUPER_ADMIN");
        for(String role:roles) {
            if(!rolesRepository.existsByName(role)) {
                Roles r = new Roles();
                r.setName(role);
                rolesRepository.save(r);
            }
        }
    }
}
