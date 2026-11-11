package com.hopeycharlie.controller;

import com.hopeycharlie.dto.DonationDtos.*;
import com.hopeycharlie.service.DonationService;
import com.hopeycharlie.service.EmailService;
import com.hopeycharlie.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final DonationService donationService;
    private final UserService userService;
    private final EmailService emailService;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginRequest request) {
        if (adminPassword == null || adminPassword.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("success", false, "error", "Admin password is not configured. Set ADMIN_PASSWORD before using the admin panel."));
        }
        if (request == null || request.getPassword() == null ||
                !java.security.MessageDigest.isEqual(
                        adminPassword.getBytes(StandardCharsets.UTF_8),
                        request.getPassword().getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Incorrect password"));
        }

        String token = Jwts.builder()
                .subject("admin")
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 8L * 60 * 60 * 1000))
                .signWith(signingKey())
                .compact();

        return ResponseEntity.ok(Map.of("success", true, "token", token));
    }

    @PostMapping("/email/test")
    public ResponseEntity<?> sendTestEmail(@RequestHeader(value = "x-admin-token", required = false) String token) {
        if (!authorized(token)) return unauthorized();
        boolean sent = emailService.sendConfigurationTestEmail();
        if (!sent) {
            return ResponseEntity.status(503).body(Map.of("success", false,
                    "message", "Email test failed. Check MAIL_USERNAME, MAIL_PASSWORD, ADMIN_EMAIL and the server logs."));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Test email sent to the configured admin address."));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader(value = "x-admin-token", required = false) String token) {
        if (!authorized(token)) return unauthorized();
        return ResponseEntity.ok(donationService.getStats(userService.getUserCount()));
    }

    @GetMapping("/donations")
    public ResponseEntity<?> getDonations(@RequestHeader(value = "x-admin-token", required = false) String token) {
        if (!authorized(token)) return unauthorized();
        return ResponseEntity.ok(new DonationListResponse(donationService.getAllDonations()));
    }

    @PostMapping("/donations/{id}/confirm")
    public ResponseEntity<?> confirmDonation(@PathVariable String id,
            @RequestHeader(value = "x-admin-token", required = false) String token) {
        if (!authorized(token)) return unauthorized();
        donationService.confirmDonation(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Donation confirmed. Donor notified."));
    }

    @PostMapping("/donations/{id}/reject")
    public ResponseEntity<?> rejectDonation(@PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "x-admin-token", required = false) String token) {
        if (!authorized(token)) return unauthorized();
        String reason = body != null ? body.get("reason") : null;
        donationService.rejectDonation(id, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Donation rejected. Donor notified."));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestHeader(value = "x-admin-token", required = false) String token) {
        if (!authorized(token)) return unauthorized();
        return ResponseEntity.ok(new UserListResponse(userService.getAllUsers()));
    }

    @PostMapping("/users/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable String id,
            @RequestHeader(value = "x-admin-token", required = false) String token) {
        if (!authorized(token)) return unauthorized();
        userService.disableUser(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/users/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable String id,
            @RequestHeader(value = "x-admin-token", required = false) String token) {
        if (!authorized(token)) return unauthorized();
        userService.enableUser(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id,
            @RequestHeader(value = "x-admin-token", required = false) String token) {
        if (!authorized(token)) return unauthorized();
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private boolean authorized(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey()).build()
                    .parseSignedClaims(token).getPayload();
            return "admin".equals(claims.getSubject()) && "ADMIN".equals(claims.get("role", String.class));
        } catch (Exception ex) {
            return false;
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("success", false, "message", "Administrator session is invalid or expired."));
    }
}
