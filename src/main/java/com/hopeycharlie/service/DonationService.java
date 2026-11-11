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
    public DonationResponse processDonation(DonationRequest request, String userEmail) {
        if ("bitcoin".equalsIgnoreCase(request.getMethod())) {
            return processBitcoinDonation(request, userEmail);
        }
        throw new IllegalArgumentException("This payment method is not currently available. Card payments will be enabled only through a secure payment processor.");
    }

    private DonationResponse processBitcoinDonation(DonationRequest request, String userEmail) {
        // Build and save the donation entity
        Donation donation = Donation.builder()
                .amount(request.getAmount())
                .donorEmail(request.getDonorEmail())
                .donorName(request.getDonorName())
                .userEmail(userEmail)
                .method("bitcoin")
                .status("pending")
                .walletAddress(bitcoinWallet)
                .transactionId(request.getTransactionId())
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
        detail.setTransactionId(d.getTransactionId());
        detail.setStatus(d.getStatus());
        detail.setCreatedAt(d.getCreatedAt());
        detail.setConfirmedAt(d.getConfirmedAt());
        detail.setRejectedAt(d.getRejectedAt());
        return detail;
    }

    /** Everything a signed-in user has donated: by account link or by matching donor email. */
    public List<DonationDetail> getDonationsByEmail(String email) {
        return donationRepository.findByUserEmailIgnoreCaseOrDonorEmailIgnoreCaseOrderByCreatedAtDesc(email, email)
                .stream()
                .map(this::toDetail)
                .collect(Collectors.toList());
    }
}
