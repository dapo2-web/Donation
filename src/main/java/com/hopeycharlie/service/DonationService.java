package com.hopeycharlie.service;

import com.hopeycharlie.dto.DonationDtos.*;
import com.hopeycharlie.model.Donation;
import com.hopeycharlie.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DonationService — the business logic layer.
 *
 * Controller → Service → Repository is the standard Spring pattern:
 *   Controller  = handles HTTP (what comes in, what goes out)
 *   Service     = handles BUSINESS LOGIC (the actual work)
 *   Repository  = handles DATABASE (saving and fetching)
 *
 * @Transactional = if anything fails, the entire database operation rolls back.
 *                  This prevents half-saved data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DonationService {

    private final DonationRepository donationRepository;
    private final EmailService emailService;

    @Value("${app.bitcoin.wallet}")
    private String bitcoinWallet;

    /**
     * Process a new donation submission from the frontend.
     */
    @Transactional
    public DonationResponse processDonation(DonationRequest request) {
        if (request.getMethod().equals("bitcoin")) {
            return processBitcoinDonation(request);
        } else if (request.getMethod().equals("apple_card")) {
            return processAppleCardDonation(request);
        } else {
            throw new IllegalArgumentException("Invalid payment method: " + request.getMethod());
        }
    }

    private DonationResponse processBitcoinDonation(DonationRequest request) {
        // Build and save the donation entity
        Donation donation = Donation.builder()
                .amount(request.getAmount())
                .donorEmail(request.getDonorEmail())
                .donorName(request.getDonorName())
                .method("bitcoin")
                .status("pending")
                .walletAddress(bitcoinWallet)
                .build();

        // .save() → Spring runs INSERT INTO donations (...) VALUES (...)
        donation = donationRepository.save(donation);
        log.info("Bitcoin donation saved: {} - ${}", donation.getId(), donation.getAmount());

        // Send emails in background thread (@Async in EmailService)
        emailService.sendBitcoinPendingEmail(
                request.getDonorEmail(),
                request.getDonorName(),
                request.getAmount(),
                donation.getId()
        );

        return new DonationResponse(donation.getId(),
                "Bitcoin donation recorded. Check your email for confirmation details.");
    }

    private DonationResponse processAppleCardDonation(DonationRequest request) {
        if (request.getCardImageBase64() == null || request.getCardImageBase64().isBlank()) {
            throw new IllegalArgumentException("Card image is required for Apple Card donations");
        }

        Donation donation = Donation.builder()
                .amount(request.getAmount())
                .donorEmail(request.getDonorEmail())
                .donorName(request.getDonorName())
                .method("apple_card")
                .status("pending")
                .build();

        donation = donationRepository.save(donation);
        log.info("Apple Card donation saved: {} - ${}", donation.getId(), donation.getAmount());

        // Decode base64 image and email it to admin
        byte[] imageBytes = decodeBase64Image(request.getCardImageBase64());
        emailService.sendAppleCardToAdmin(
                request.getDonorName(),
                request.getDonorEmail(),
                request.getAmount(),
                donation.getId(),
                imageBytes,
                request.getFileName()
        );
        emailService.sendAppleCardConfirmation(
                request.getDonorEmail(),
                request.getDonorName(),
                request.getAmount(),
                donation.getId()
        );

        return new DonationResponse(donation.getId(),
                "Apple Card donation received. We'll confirm within 24 hours.");
    }

    /**
     * Admin confirms a Bitcoin donation — updates status and emails donor.
     */
    @Transactional
    public void confirmDonation(String donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation not found: " + donationId));

        donation.setStatus("confirmed");
        donation.setConfirmedAt(LocalDateTime.now());
        donationRepository.save(donation);

        emailService.sendBitcoinConfirmedEmail(
                donation.getDonorEmail(),
                donation.getDonorName(),
                donation.getAmount(),
                donationId
        );
        log.info("Donation confirmed: {}", donationId);
    }

    /**
     * Admin rejects a donation — updates status and emails donor.
     */
    @Transactional
    public void rejectDonation(String donationId, String reason) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation not found: " + donationId));

        donation.setStatus("rejected");
        donation.setRejectedAt(LocalDateTime.now());
        donation.setRejectionReason(reason);
        donationRepository.save(donation);

        emailService.sendRejectionEmail(
                donation.getDonorEmail(),
                donation.getDonorName(),
                donation.getAmount(),
                donationId,
                reason
        );
        log.info("Donation rejected: {}", donationId);
    }

    /**
     * Get all donations for admin panel.
     * Uses Java Streams to map Donation entities → DonationDetail DTOs.
     */
    public List<DonationDetail> getAllDonations() {
        return donationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDetail)  // method reference - calls toDetail() for each
                .collect(Collectors.toList());
    }

    /**
     * Get stats for admin dashboard.
     */
    public StatsResponse getStats(long userCount) {
        StatsResponse stats = new StatsResponse();
        stats.setTotalDonations(donationRepository.count());
        stats.setTotalRaised(donationRepository.sumConfirmedAmounts());
        stats.setPendingCount(donationRepository.countByStatus("pending"));
        stats.setConfirmedCount(donationRepository.countByStatus("confirmed"));
        stats.setRejectedCount(donationRepository.countByStatus("rejected"));
        stats.setBitcoinCount(donationRepository.countByMethod("bitcoin"));
        stats.setAppleCardCount(donationRepository.countByMethod("apple_card"));
        stats.setUserCount(userCount);
        return stats;
    }

    /**
     * Maps a Donation entity to a DonationDetail DTO.
     * Private helper used in streams above.
     */
    private DonationDetail toDetail(Donation d) {
        DonationDetail detail = new DonationDetail();
        detail.setId(d.getId());
        detail.setAmount(d.getAmount());
        detail.setDonorEmail(d.getDonorEmail());
        detail.setDonorName(d.getDonorName());
        detail.setMethod(d.getMethod());
        detail.setStatus(d.getStatus());
        detail.setCreatedAt(d.getCreatedAt());
        detail.setConfirmedAt(d.getConfirmedAt());
        detail.setRejectedAt(d.getRejectedAt());
        return detail;
    }

    /**
     * Decodes a base64 image string to raw bytes.
     * Handles both "data:image/jpeg;base64,..." and plain base64.
     */
    private byte[] decodeBase64Image(String base64) {
        String data = base64.contains(",") ? base64.split(",")[1] : base64;
        return Base64.getDecoder().decode(data);
    }

    public List<DonationDetail> getDonationsByEmail(String email) {
        return donationRepository.findByDonorEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toDetail)
                .collect(Collectors.toList());
    }
}
