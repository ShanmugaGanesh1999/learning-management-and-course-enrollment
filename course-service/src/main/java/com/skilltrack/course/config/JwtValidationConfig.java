package com.skilltrack.course.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for validating JWTs issued by Auth-Service.
 *
 * IMPORTANT:
 * - The secret must be identical across services (auth, course, enrollment, gateway).
 * - Secrets must be provided via environment variables (never hard-coded).
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtValidationConfig {

    private String secret;
}
