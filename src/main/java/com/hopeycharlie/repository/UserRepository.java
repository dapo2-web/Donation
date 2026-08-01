package com.hopeycharlie.repository;

import com.hopeycharlie.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository — Spring Data JPA repository for User entities.
 *
 * Optional<User> means the result might be null (user not found).
 * You check it like: userRepo.findByEmail(email).orElseThrow(...)
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // Used during login to find user by email
    Optional<User> findByEmail(String email);

    // Check if email already registered (during signup)
    boolean existsByEmail(String email);

    // Find all users ordered by newest first (for admin panel)
    List<User> findAllByOrderByCreatedAtDesc();

    // Search users by name or email (admin search)
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email
    );
}
