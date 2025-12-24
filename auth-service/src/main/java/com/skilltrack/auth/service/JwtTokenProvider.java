package com.skilltrack.auth.service;

import com.skilltrack.auth.config.JwtConfig;
import com.skilltrack.auth.exception.UnauthorizedException;
import com.skilltrack.auth.model.Role;
import com.skilltrack.auth.security.UserPrincipal;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    /**
     * Generates an access token.
     * Claims: userId, username, role.
     */
    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtConfig.getExpiration(), "access");
    }

    /**
     * Generates a refresh token with longer expiry.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtConfig.getRefreshExpiration(), "refresh");
    }

    // Convenience overload for existing authentication flows.
    public String generateToken(Authentication authentication) {
        return generateToken((UserDetails) authentication.getPrincipal());
    }

    public String generateRefreshToken(Authentication authentication) {
        return generateRefreshToken((UserDetails) authentication.getPrincipal());
    }

    public long getAccessTokenTtlMillis() {
        return jwtConfig.getExpiration();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Object value = Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId");
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    public String getRoleFromToken(String token) {
        Object value = Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Returns the token type claim: typically "access" or "refresh".
     */
    public String getTokenTypeFromToken(String token) {
        Object value = Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("typ");
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Validates JWT signature and expiration.
     * Throws a specific exception for expired tokens.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(token);
            log.debug("JWT validated successfully");
            return true;
        } catch (ExpiredJwtException ex) {
            log.info("JWT expired for subject={}", ex.getClaims() != null ? ex.getClaims().getSubject() : "unknown");
            throw new UnauthorizedException("Token expired");
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    private String buildToken(UserDetails userDetails, long ttlMillis, String type) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttlMillis);

        Long userId = null;
        Role role = null;
        if (userDetails instanceof UserPrincipal principal) {
            userId = principal.getId();
            role = principal.getRole();
        }

        String username = userDetails.getUsername();
        String roleValue = role != null ? role.name() : null;

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", roleValue)
                .claim("typ", type)
                // HS512 is selected for stronger HMAC security properties.
                .signWith(signingKey(), SignatureAlgorithm.HS512)
                .compact();

        log.info("JWT generated: subject={} type={} exp={}", username, type, expiryDate.getTime());
        return token;
    }

    private Key signingKey() {
        // Security considerations:
        // - JWT secret must be strong and environment-provided (never hardcoded).
        // - HS512 requires a 512-bit key; we derive a fixed 512-bit key from the configured secret.
        byte[] secretBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
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
}
