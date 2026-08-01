package com.hopeycharlie.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * User entity - stored in the "users" table in PostgreSQL.
 * Spring Security will use this for login/register.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Stored as a BCrypt hash - NEVER store plain text passwords.
     * Spring Security's BCryptPasswordEncoder handles this automatically.
     */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private String role = "USER"; // "USER" or "ADMIN"

    @Column
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login")
    private LocalDateTime lastLogin;
}
