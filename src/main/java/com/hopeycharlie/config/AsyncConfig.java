package com.hopeycharlie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AsyncConfig — enables @Async annotation on methods.
 *
 * When EmailService methods are annotated with @Async,
 * Spring runs them in a separate background thread so the
 * API responds instantly without waiting for emails to send.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring handles the thread pool automatically.
    // No additional configuration needed for basic async support.
}