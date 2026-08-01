package com.hopeycharlie.controller;

import com.hopeycharlie.dto.DonationDtos.*;
import com.hopeycharlie.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — handles user registration and login.
 *
 * Routes:
 *   POST /api/auth/register  → create new account
 *   POST /api/auth/login     → login, get JWT token
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * POST /api/auth/register
     *
     * Request body:
     * {
     *   "fullName": "Jane Doe",
     *   "email": "jane@example.com",
     *   "password": "securepassword"
     * }
     *
     * Returns: JWT token + user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/login
     *
     * Request body:
     * {
     *   "email": "jane@example.com",
     *   "password": "securepassword"
     * }
     *
     * Returns: JWT token + user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(401).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
