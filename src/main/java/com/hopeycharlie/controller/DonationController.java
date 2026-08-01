package com.hopeycharlie.controller;

import com.hopeycharlie.dto.DonationDtos.*;
import com.hopeycharlie.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * POST /api/donate
     * Submit a new donation.
     */
    @PostMapping
    public ResponseEntity<DonationResponse> donate(@Valid @RequestBody DonationRequest request) {
        log.info("Donation request received: ${} via {}", request.getAmount(), request.getMethod());
        DonationResponse response = donationService.processDonation(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/donate/history?email=donor@example.com
     * Returns donation history for a specific donor email.
     * Used by the "My Donations" page for logged-in users.
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getDonationHistory(@RequestParam String email) {
        List<DonationDetail> donations = donationService.getDonationsByEmail(email);
        return ResponseEntity.ok(Map.of("donations", donations));
    }
}
