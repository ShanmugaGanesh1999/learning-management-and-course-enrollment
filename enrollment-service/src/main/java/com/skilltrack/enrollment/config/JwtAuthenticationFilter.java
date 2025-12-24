package com.skilltrack.enrollment.config;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.List;

/**
 * Validates Bearer JWT tokens and populates Spring SecurityContext.
 *
 * This service does not authenticate users with a database; it only validates
 * tokens issued by Auth-Service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidationConfig jwtValidationConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring("Bearer ".length()).trim();
        }

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var claims = Jwts.parserBuilder()
                        .setSigningKey(signingKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                Long userId = claims.get("userId", Long.class);

                List<SimpleGrantedAuthority> authorities = role == null
                        ? List.of()
                        : List.of(new SimpleGrantedAuthority("ROLE_" + role));

                JwtUserPrincipal principal = new JwtUserPrincipal(userId, username, role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                log.warn("JWT validation failed: path={} message={}", request.getRequestURI(), ex.getMessage());
            } catch (Exception ex) {
                log.warn("JWT processing error: path={} message={}", request.getRequestURI(), ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private Key signingKey() {
        byte[] secretBytes = jwtValidationConfig.getSecret().getBytes(StandardCharsets.UTF_8);
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

    /**
     * Minimal principal for downstream business logic (ownership checks, auditing).
     */
    public record JwtUserPrincipal(Long userId, String username, String role) {
    }
}
