package com.hopeycharlie.repository;

import com.hopeycharlie.model.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * DonationRepository — Spring Data JPA magic.
 *
 * By extending JpaRepository<Donation, String>, Spring AUTO-GENERATES
 * all the SQL for you. You get these for FREE without writing any SQL:
 *
 *   donationRepository.save(donation)         → INSERT / UPDATE
 *   donationRepository.findById(id)           → SELECT WHERE id = ?
 *   donationRepository.findAll()              → SELECT * FROM donations
 *   donationRepository.delete(donation)       → DELETE
 *   donationRepository.count()               → SELECT COUNT(*)
 *
 * You can also define custom queries by METHOD NAME — Spring reads
 * the method name and writes the SQL automatically:
 *   findByStatus("pending")   → SELECT * FROM donations WHERE status = 'pending'
 *   findByDonorEmail(email)   → SELECT * FROM donations WHERE donor_email = ?
 */
@Repository
public interface DonationRepository extends JpaRepository<Donation, String> {

    // Spring reads "ByStatus" and generates: WHERE status = ?
    List<Donation> findByStatusOrderByCreatedAtDesc(String status);

    // Find all donations by a specific donor email
    List<Donation> findByDonorEmailOrderByCreatedAtDesc(String donorEmail);

    // Find all donations ordered by newest first
    List<Donation> findAllByOrderByCreatedAtDesc();

    // Count pending donations (for admin stats)
    long countByStatus(String status);

    // Custom JPQL query - sum all confirmed donation amounts
    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Donation d WHERE d.status = 'confirmed'")
    BigDecimal sumConfirmedAmounts();

    // Count donations per method
    long countByMethod(String method);

    // Count donations by donor email (for user stats)
    long countByDonorEmail(String donorEmail);
}
