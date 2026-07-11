package com.ashraf.controller;

import com.ashraf.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PatchMapping("/restaurants/{id}/approve")
    public ResponseEntity<String> approveRestaurants(@PathVariable("id") Long id) {
        adminService.approveRestaurants(id);
        return  ResponseEntity.ok("Successfully approved.");
    }

    @PatchMapping("/riders/{id}/approve")
    public ResponseEntity<String> approveRiders(@PathVariable("id") Long id) {
        adminService.approveRiders(id);
        return  ResponseEntity.ok("Successfully approved.");
    }
}
