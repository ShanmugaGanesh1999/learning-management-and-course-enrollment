package com.skilltrack.enrollment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT validation configuration.
 */
@Configuration
@Getter
public class JwtValidationConfig {

    @Value("${jwt.secret}")
    private String secret;
}
