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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donate")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class DonationController {

    private final DonationService donationService;
    private final UserService userService;

    @Value("${app.bitcoin.wallet}")
    private String bitcoinWallet;

    /**
     * POST /api/donate
     * Submit a new donation. Guests can donate; if a valid sign-in token is sent,
     * the donation is linked to that account so it shows up in "My Donations".
     */
    @GetMapping("/wallet")
    public ResponseEntity<?> getBitcoinWallet() {
        return ResponseEntity.ok(Map.of("walletAddress", bitcoinWallet));
    }

    @PostMapping
    public ResponseEntity<DonationResponse> donate(
            @Valid @RequestBody DonationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userEmail = userService.emailFromToken(authHeader).orElse(null);
        log.info("Donation request received: ${} via {} (signed in: {})",
                request.getAmount(), request.getMethod(), userEmail != null);
        return ResponseEntity.ok(donationService.processDonation(request, userEmail));
    }

    /**
     * GET /api/donate/history
     * Returns the signed-in user's own donations. The user is identified from the
     * Authorization token, NOT from a query parameter, so nobody can look up
     * another person's donations by guessing their email.
     */
    @GetMapping("/history")
    public ResponseEntity<?> getDonationHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return userService.emailFromToken(authHeader)
                .<ResponseEntity<?>>map(email -> {
                    List<DonationDetail> donations = donationService.getDonationsByEmail(email);
                    return ResponseEntity.ok(Map.of("donations", donations));
                })
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Please sign in again.")));
    }
}
