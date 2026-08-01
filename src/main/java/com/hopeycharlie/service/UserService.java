package com.hopeycharlie.service;

import com.hopeycharlie.dto.DonationDtos.*;
import com.hopeycharlie.model.User;
import com.hopeycharlie.repository.DonationRepository;
import com.hopeycharlie.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService — handles user registration, login, and admin user management.
 *
 * JWT (JSON Web Token) is how we handle authentication:
 *   1. User logs in with email + password
 *   2. We verify password with BCrypt
 *   3. We generate a JWT token and send it back
 *   4. Frontend stores the token and sends it with future requests
 *   5. Spring Security validates the token on protected routes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Register a new user account.
     * Passwords are hashed with BCrypt before saving — NEVER stored as plain text.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already taken
        if (userRepository.existsByEmail(request.getEmail())) {
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage("This email is already registered.");
            return response;
        }

        // Build user - passwordEncoder.encode() runs BCrypt hash
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash
                .role("USER")
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", request.getEmail());

        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage("Account created successfully!");
        response.setToken(generateToken(user));
        response.setName(user.getFullName());
        response.setEmail(user.getEmail());
        return response;
    }

    /**
     * Login - verify password and return a JWT token.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // passwordEncoder.matches() compares plain password to BCrypt hash
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage("Incorrect email or password.");
            return response;
        }

        if (!user.isEnabled()) {
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage("This account has been disabled. Contact support.");
            return response;
        }

        // Update last login timestamp
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage("Login successful!");
        response.setToken(generateToken(user));
        response.setName(user.getFullName());
        response.setEmail(user.getEmail());
        return response;
    }

    /**
     * Get all users for admin panel with their donation stats.
     */
    public List<UserDetail> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toUserDetail)
                .collect(Collectors.toList());
    }

    /**
     * Disable a user account (they can't log in but data is kept).
     */
    @Transactional
    public void disableUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(false);
        userRepository.save(user);
        log.info("User disabled: {}", user.getEmail());
    }

    /**
     * Re-enable a disabled user.
     */
    @Transactional
    public void enableUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        log.info("User enabled: {}", user.getEmail());
    }

    /**
     * Permanently delete a user.
     */
    @Transactional
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
        log.info("User deleted: {}", userId);
    }

    /**
     * Total user count — used in stats.
     */
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * Generate a JWT token for a user.
     * The token encodes the user's email and expires in 7 days.
     */
    private String generateToken(User user) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is long enough for HS256
        byte[] paddedKey = new byte[32];
        System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("name", user.getFullName())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)) // 7 days
                .signWith(Keys.hmacShaKeyFor(paddedKey), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Map User entity → UserDetail DTO with donation stats.
     */
    private UserDetail toUserDetail(User user) {
        UserDetail detail = new UserDetail();
        detail.setId(user.getId());
        detail.setFullName(user.getFullName());
        detail.setEmail(user.getEmail());
        detail.setRole(user.getRole());
        detail.setEnabled(user.isEnabled());
        detail.setCreatedAt(user.getCreatedAt());
        detail.setLastLogin(user.getLastLogin());

        // Count and sum this user's donations
        detail.setDonationCount(donationRepository.countByDonorEmail(user.getEmail()));
        BigDecimal total = donationRepository.findByDonorEmailOrderByCreatedAtDesc(user.getEmail())
                .stream()
                .filter(d -> "confirmed".equals(d.getStatus()))
                .map(d -> d.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        detail.setDonationTotal(total);

        return detail;
    }
}
