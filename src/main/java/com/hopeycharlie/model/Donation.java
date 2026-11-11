package com.hopeycharlie.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Donation entity - JPA maps this Java class to a "donations" table in PostgreSQL.
 *
 * @Entity   = This class is a database table
 * @Table    = The actual table name in Postgres
 * @Id       = Primary key column
 * @Column   = A column in the table
 * @Data     = Lombok generates getters, setters, toString, equals, hashCode
 * @Builder  = Allows: Donation.builder().amount(50).build()
 */
@Entity
@Table(name = "donations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "donor_email", nullable = false)
    private String donorEmail;

    @Column(name = "donor_name")
    private String donorName;

    /**
     * Email of the signed-in account that made this donation (null for guest donations).
     * Lets "My Donations" work even if the donor typed a different email on the form.
     */
    @Column(name = "user_email")
    private String userEmail;

    /**
     * Payment method: "bitcoin" (legacy records may retain older method values)
     */
    @Column(nullable = false)
    private String method;

    /**
     * Status lifecycle: pending → confirmed OR rejected
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "pending";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Bitcoin wallet address stored for reference
    @Column(name = "wallet_address")
    private String walletAddress;

    @Column(name = "transaction_id", length = 128)
    private String transactionId;
}
