package com.skilltrack.auth.service;

import com.skilltrack.auth.exception.BadRequestException;
import com.skilltrack.auth.exception.UnauthorizedException;
import com.skilltrack.auth.model.User;
import com.skilltrack.auth.model.dto.LoginRequest;
import com.skilltrack.auth.model.dto.LoginResponse;
import com.skilltrack.auth.model.dto.RefreshTokenRequest;
import com.skilltrack.auth.model.dto.RegisterRequest;
import com.skilltrack.auth.model.dto.TokenRefreshResponse;
import com.skilltrack.auth.model.dto.UserProfileResponse;
import com.skilltrack.auth.repository.UserRepository;
import com.skilltrack.auth.security.UserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user.
     *
     * Validation steps:
     * - Ensures password and confirmPassword match (defense-in-depth; also validated at DTO level).
     * - Ensures username and email are unique (prevents account confusion and takeover scenarios).
     *
     * Security checks:
     * - Password is hashed using BCrypt (via PasswordEncoder) and never stored in plaintext.
     * - Logs are kept non-sensitive (no password / token data).
     */
    @Transactional
    public UserProfileResponse registerUser(RegisterRequest request) {
        log.info("Registration attempt: username={} email={}", request.getUsername(), request.getEmail());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirmPassword must match");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        User saved = userRepository.save(user);
        log.info("Registration success: userId={} username={}", saved.getId(), saved.getUsername());
        return userService.toProfile(saved);
    }

    /**
     * Authenticates a user via username/password.
     *
     * Token generation flow:
     * 1) Authenticate credentials via AuthenticationManager.
     * 2) Store Authentication in SecurityContext for the current request thread.
     * 3) Generate access token + refresh token.
     * 4) Update lastLoginAt for auditing.
     */
    @Transactional
    public LoginResponse authenticateUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT tokens are returned as strings; clients should send them as:
        // Authorization: Bearer <token>
        String token = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        User user = userService.requireByUsername(request.getUsername());

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Login success: userId={} username={}", user.getId(), user.getUsername());

        return LoginResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .userDetails(userService.toProfile(user))
            .expiresIn(jwtTokenProvider.getAccessTokenTtlMillis())
            .tokenType("Bearer")
            .build();
    }

    /**
     * Loads the current user based on a JWT token string.
     *
     * Notes on SecurityContext usage:
     * - Controllers typically rely on Spring Security to populate SecurityContextHolder.
     * - This method exists for cases where you only have a raw token string (e.g., service-to-service).
     */
    public UserProfileResponse getCurrentUser(String tokenOrAuthorizationHeader) {
        if (tokenOrAuthorizationHeader == null || tokenOrAuthorizationHeader.isBlank()) {
            throw new UnauthorizedException("Missing token");
        }

        String token = tokenOrAuthorizationHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring("Bearer ".length()).trim();
        }

        if (!jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Invalid token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        User user = userService.requireByUsername(username);
        return userService.toProfile(user);
    }

    /**
     * Exchanges a refresh token for a new access token.
     *
     * Refresh token rotation strategy (recommended):
     * - Issue a new refresh token and invalidate the old one server-side (DB/Redis).
     * - This scaffold returns a new refresh token but does not persist token state.
     */
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String type = jwtTokenProvider.getTokenTypeFromToken(refreshToken);
        if (!"refresh".equalsIgnoreCase(type)) {
            throw new UnauthorizedException("Invalid token type");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userService.requireByUsername(username);

        UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.getPassword(), user.getRole());
        String newAccessToken = jwtTokenProvider.generateToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(principal);

        log.info("Refresh token exchange success: userId={} username={}", user.getId(), user.getUsername());

        return TokenRefreshResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenTtlMillis())
                .tokenType("Bearer")
                .build();
    }

    // Backward-compatible methods (older controller/service calls).

    public UserProfileResponse register(RegisterRequest request) {
        return registerUser(request);
    }

    public LoginResponse login(LoginRequest request) {
        return authenticateUser(request);
    }

    public UserProfileResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        Object principal = authentication.getPrincipal();

        // Principal type can vary; we support both Spring's default UserDetails and our UserPrincipal.
        String username;
        if (principal instanceof org.springframework.security.core.userdetails.User u) {
            username = u.getUsername();
        } else if (principal instanceof UserPrincipal up) {
            username = up.getUsername();
        } else {
            username = String.valueOf(principal);
        }
        User user = userService.requireByUsername(username);
        return userService.toProfile(user);
    }
}
