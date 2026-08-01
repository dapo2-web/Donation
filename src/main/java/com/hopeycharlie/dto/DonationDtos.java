package com.hopeycharlie.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs (Data Transfer Objects) — these are the shapes of data
 * that flow IN and OUT of your REST API.
 *
 * Rule of thumb:
 *   Request DTOs  = what the frontend sends TO Spring
 *   Response DTOs = what Spring sends BACK to the frontend
 *
 * We keep them separate from the database models (Donation, User)
 * so we never accidentally expose sensitive fields like passwords.
 */
public class DonationDtos {

    // ── REQUEST: Frontend sends this when donating ────────────────
    @Data
    public static class DonationRequest {

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Minimum donation is $1")
        private BigDecimal amount;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email")
        private String donorEmail;

        private String donorName;

        @NotBlank(message = "Payment method is required")
        private String method; // "bitcoin" or "apple_card"

        // Only used for Apple Card - base64 encoded image
        private String cardImageBase64;
        private String fileName;
    }

    // ── RESPONSE: Spring sends this back after donation ───────────
    @Data
    public static class DonationResponse {
        private String donationId;
        private String message;
        private boolean success;

        public DonationResponse(String donationId, String message) {
            this.donationId = donationId;
            this.message = message;
            this.success = true;
        }
    }

    // ── RESPONSE: Donation details for admin panel ────────────────
    @Data
    public static class DonationDetail {
        private String id;
        private BigDecimal amount;
        private String donorEmail;
        private String donorName;
        private String method;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime confirmedAt;
        private LocalDateTime rejectedAt;
    }

    // ── RESPONSE: Admin dashboard stats ──────────────────────────
    @Data
    public static class StatsResponse {
        private long totalDonations;
        private BigDecimal totalRaised;
        private long pendingCount;
        private long confirmedCount;
        private long rejectedCount;
        private long bitcoinCount;
        private long appleCardCount;
        private long userCount;
    }

    // ── REQUEST: Admin confirms/rejects ──────────────────────────
    @Data
    public static class ActionRequest {
        @NotBlank
        private String donationId;
        private String reason; // optional rejection reason
    }

    // ── REQUEST/RESPONSE: Auth ────────────────────────────────────
    @Data
    public static class RegisterRequest {
        @NotBlank private String fullName;
        @Email @NotBlank private String email;
        @NotBlank @Size(min = 6) private String password;
    }

    @Data
    public static class LoginRequest {
        @Email @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String name;
        private String email;
        private boolean success;
        private String message;
    }

    // ── RESPONSE: User details for admin ─────────────────────────
    @Data
    public static class UserDetail {
        private String id;
        private String fullName;
        private String email;
        private String role;
        private boolean enabled;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;
        private long donationCount;
        private BigDecimal donationTotal;
    }

    // ── RESPONSE: Admin login ─────────────────────────────────────
    @Data
    public static class AdminLoginRequest {
        @NotBlank private String password;
    }

    // ── RESPONSE: List wrappers ───────────────────────────────────
    @Data
    public static class DonationListResponse {
        private List<DonationDetail> donations;
        private int total;

        public DonationListResponse(List<DonationDetail> donations) {
            this.donations = donations;
            this.total = donations.size();
        }
    }

    @Data
    public static class UserListResponse {
        private List<UserDetail> users;
        private int total;

        public UserListResponse(List<UserDetail> users) {
            this.users = users;
            this.total = users.size();
        }
    }
}
