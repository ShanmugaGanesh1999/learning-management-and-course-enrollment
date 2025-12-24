package com.skilltrack.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:4200}")
    private String corsAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Origins are environment-driven to keep dev/prod safe:
        // - dev: http://localhost:4200
        // - prod: your real domain(s)
        config.setAllowedOrigins(List.of(corsAllowedOrigins.split(",")));
        // Allow only the minimal HTTP methods used by this API.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allow Authorization header for Bearer tokens and Content-Type for JSON.
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        // Allow credentials if the frontend uses cookies in the future (kept true by spec).
        config.setAllowCredentials(true);
        // Cache preflight response for 1 hour.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
