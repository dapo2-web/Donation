package com.hopeycharlie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for Hopey & Charlie's Hearts Spring Boot application.
 *
 * @SpringBootApplication combines:
 *   - @Configuration       (this class can define beans)
 *   - @EnableAutoConfiguration (Spring Boot auto-wires everything)
 *   - @ComponentScan       (scans this package for controllers/services/etc)
 */
@SpringBootApplication
public class HopeyCharlieApplication {

    public static void main(String[] args) {
        SpringApplication.run(HopeyCharlieApplication.class, args);
    }
}
