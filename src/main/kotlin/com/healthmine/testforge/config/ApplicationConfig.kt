package com.healthmine.testforge.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfig {
    // Intentionally empty — TestForge is JWT-verifier-only with no UserDetailsService
    // This prevents Spring Security from auto-creating an in-memory user
}
