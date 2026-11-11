package com.hopeycharlie.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig — configures Spring Security.
 *
 * This class controls:
 *   1. Which API routes are PUBLIC (no login needed)
 *   2. Which routes are PROTECTED (need auth)
 *   3. Password hashing (BCrypt)
 *   4. CORS (allows your HTML frontend to call the API)
 *
 * @Configuration = Spring reads this class for bean definitions
 * @EnableWebSecurity = Activates Spring Security
 * @Bean = The return value of this method is managed by Spring
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * SecurityFilterChain — defines the security rules.
     *
     * Every HTTP request goes through this filter chain before
     * reaching your controllers.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — not needed for REST APIs with JWT
                .csrf(csrf -> csrf.disable())

                // Configure CORS using our corsConfigurationSource bean below
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless session — we use JWT tokens, not server sessions
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Define which routes are public vs protected
                .authorizeHttpRequests(auth -> auth
                        // Public routes — anyone can call these
                        .requestMatchers("/api/auth/**").permitAll()   // register, login
                        .requestMatchers("/api/donate", "/api/donate/**").permitAll() // donate + history (token checked in controller)
                        .requestMatchers("/api/admin/login").permitAll() // admin login
                        // Static files (your HTML/CSS/JS frontend)
                        .requestMatchers("/", "/index.html", "/admin.html",
                                "/**.html", "/**.css", "/**.js").permitAll()
                        // Admin routes — protected by our AdminTokenFilter (token in header)
                        // We handle admin auth manually in the controller for simplicity
                        .requestMatchers("/api/admin/**").permitAll()  // token checked in controller
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Disable Spring's default login page (we have our own)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /**
     * BCryptPasswordEncoder — used to hash passwords before saving to DB.
     *
     * BCrypt is one-way: you can't reverse it to get the original password.
     * To verify: BCrypt.matches("plaintext", "$2a$10$hashedvalue...") → true/false
     *
     * The strength parameter (10) controls how slow the hash is.
     * Slower = harder to brute-force. 10 is the standard default.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * CORS Configuration — allows your frontend HTML to call your Spring API.
     *
     * Without this, browsers block cross-origin requests for security.
     * Since your HTML and API are on the same Railway domain, this is
     * mostly needed during local development.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
