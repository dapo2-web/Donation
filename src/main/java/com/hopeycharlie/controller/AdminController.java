package com.hopeycharlie.controller;

import com.hopeycharlie.dto.DonationDtos.*;
import com.hopeycharlie.service.DonationService;
import com.hopeycharlie.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AdminController — all admin panel endpoints.
 * Protected by Spring Security (requires "ADMIN" role — see SecurityConfig).
 *
 * Routes:
 *   POST /api/admin/login          → admin login, returns token
 *   GET  /api/admin/stats          → dashboard stats
 *   GET  /api/admin/donations      → all donations list
 *   POST /api/admin/donations/{id}/confirm  → confirm Bitcoin donation
 *   POST /api/admin/donations/{id}/reject   → reject donation
 *   GET  /api/admin/users          → all registered users
 *   POST /api/admin/users/{id}/disable      → disable user
 *   POST /api/admin/users/{id}/enable       → enable user
 *   DELETE /api/admin/users/{id}            → delete user
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final DonationService donationService;
    private final UserService userService;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * POST /api/admin/login
     * Simple password-based admin login — returns a token if correct.
     */
    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginRequest request) {
        if (!adminPassword.equals(request.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Incorrect password"));
        }
        // Return a simple token — admin panel stores this and sends it in x-admin-token header
        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", adminPassword  // In production, use a proper JWT here
        ));
    }

    /**
     * GET /api/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(donationService.getStats(userService.getUserCount()));
    }

    /**
     * GET /api/admin/donations
     */
    @GetMapping("/donations")
    public ResponseEntity<DonationListResponse> getDonations() {
        return ResponseEntity.ok(new DonationListResponse(donationService.getAllDonations()));
    }

    /**
     * POST /api/admin/donations/{id}/confirm
     * Confirms a Bitcoin donation and emails the donor.
     *
     * @PathVariable String id — the donation ID from the URL
     */
    @PostMapping("/donations/{id}/confirm")
    public ResponseEntity<?> confirmDonation(@PathVariable String id) {
        donationService.confirmDonation(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Donation confirmed. Donor notified."));
    }

    /**
     * POST /api/admin/donations/{id}/reject
     * Rejects a donation and emails the donor.
     *
     * Request body (optional): { "reason": "Transaction not found on blockchain" }
     */
    @PostMapping("/donations/{id}/reject")
    public ResponseEntity<?> rejectDonation(@PathVariable String id,
                                            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        donationService.rejectDonation(id, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Donation rejected. Donor notified."));
    }

    /**
     * GET /api/admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<UserListResponse> getUsers() {
        return ResponseEntity.ok(new UserListResponse(userService.getAllUsers()));
    }

    /**
     * POST /api/admin/users/{id}/disable
     */
    @PostMapping("/users/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable String id) {
        userService.disableUser(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * POST /api/admin/users/{id}/enable
     */
    @PostMapping("/users/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable String id) {
        userService.enableUser(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
