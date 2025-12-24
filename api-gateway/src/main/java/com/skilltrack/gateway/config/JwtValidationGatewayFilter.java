package com.skilltrack.gateway.config;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.time.OffsetDateTime;

/**
 * Route filter that validates JWTs at the gateway.
 *
 * Responsibilities:
 * - Extract token from Authorization header (Bearer ...)
 * - Validate signature + expiration
 * - Propagate identity headers to downstream services for convenience:
 *   - X-User-Id
 *   - X-User-Role
 *   - X-Username
 *
 * Note:
 * - Downstream services in this project still validate JWTs themselves.
 * - Header propagation is helpful for debugging and future optimizations.
 */
@Slf4j
class JwtValidationGatewayFilter extends AbstractGatewayFilterFactory<JwtValidationGatewayFilter.Config> {

    @Value("${jwt.secret:${JWT_SECRET:your-very-secure-secret-key-minimum-256-bits-long-change-in-production}}")
    private String secret;

    public JwtValidationGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (auth == null || !auth.startsWith("Bearer ")) {
                return writeError(exchange, HttpStatus.UNAUTHORIZED, "Missing Bearer token");
            }

            String token = auth.substring("Bearer ".length()).trim();
            try {
                var claims = Jwts.parserBuilder()
                        .setSigningKey(signingKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                Long userId = claims.get("userId", Long.class);

                ServerWebExchange mutated = exchange.mutate()
                        .request(r -> r.headers(h -> {
                            if (userId != null) {
                                h.set("X-User-Id", String.valueOf(userId));
                            }
                            if (role != null) {
                                h.set("X-User-Role", role);
                            }
                            if (username != null) {
                                h.set("X-Username", username);
                            }
                        }))
                        .build();

                return chain.filter(mutated);
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("JWT validation failed: path={} message={}", exchange.getRequest().getURI().getPath(), ex.getMessage());
                return writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            } catch (Exception ex) {
                log.warn("JWT processing error: path={} message={}", exchange.getRequest().getURI().getPath(), ex.getMessage());
                return writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }
        };
    }

    private Key signingKey() {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = sha512(secretBytes);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static byte[] sha512(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(input);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize SHA-512", ex);
        }
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String body = "{\"timestamp\":\"" + OffsetDateTime.now() + "\"," +
                "\"status\":" + status.value() + "," +
                "\"message\":\"" + escapeJson(message) + "\"," +
                "\"path\":\"" + escapeJson(path) + "\"," +
                "\"errors\":[]}";

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Data
    public static class Config {
        // Reserved for future options (e.g., allowAnonymousPaths).
    }
}
