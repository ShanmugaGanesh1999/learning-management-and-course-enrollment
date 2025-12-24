package com.skilltrack.auth.controller;

import com.skilltrack.auth.model.dto.LoginRequest;
import com.skilltrack.auth.model.dto.LoginResponse;
import com.skilltrack.auth.model.dto.PageResponse;
import com.skilltrack.auth.model.dto.RefreshTokenRequest;
import com.skilltrack.auth.model.dto.RegisterRequest;
import com.skilltrack.auth.model.dto.TokenRefreshResponse;
import com.skilltrack.auth.model.dto.UserProfileResponse;
import com.skilltrack.auth.service.AuthService;
import com.skilltrack.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and user management endpoints")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account after validating uniqueness of username/email and hashing the password with BCrypt."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(
            examples = @ExampleObject(value = "{\"timestamp\":\"2025-01-01T00:00:00\",\"status\":400,\"message\":\"Username already exists\",\"path\":\"/api/auth/auth/register\",\"errors\":[]}")
        ))
    })
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
    // Request/response are JSON; validation is enforced via @Valid (JSR-303).
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(request));
    }

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user and issue tokens",
        description = "Validates credentials, then returns an access token (Bearer) and a refresh token. Use the access token in the Authorization header." 
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(
            examples = @ExampleObject(value = "{\"timestamp\":\"2025-01-01T00:00:00\",\"status\":401,\"message\":\"Unauthorized\",\"path\":\"/api/auth/auth/login\",\"errors\":[]}")
        ))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    // Token format:
    // Authorization: Bearer <token>
    return ResponseEntity.ok(authService.authenticateUser(request));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get current user profile",
        description = "Secured endpoint. Uses Spring Security's SecurityContextHolder to identify the current user." 
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
    // Authentication requirement:
    // - Request must include a valid Bearer token.
    // - JwtAuthenticationFilter populates SecurityContextHolder on success.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return ResponseEntity.ok(authService.currentUser(authentication));
    }

    @PostMapping("/refresh-token")
    @Operation(
        summary = "Refresh access token",
        description = "Accepts a refresh token and returns a new access token. Client should replace stored tokens (refresh token rotation)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "List users (admin)",
        description = "Admin-only endpoint with pagination/sorting and optional search by username/email."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<PageResponse<UserProfileResponse>> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "username,asc") String sort,
        @RequestParam(required = false) String search
    ) {
    // Pagination implementation:
    // - page is 0-based
    // - size caps the number of items per page
    // - sort format: field,(asc|desc)
    Pageable pageable = PageRequest.of(page, Math.min(size, 100), parseSort(sort));
    return ResponseEntity.ok(userService.getAllUsers(pageable, search));
    }

    private Sort parseSort(String sort) {
    if (sort == null || sort.isBlank()) {
        return Sort.by(Sort.Direction.ASC, "username");
    }

    String[] parts = sort.split(",", 2);
    String property = parts[0].trim();
    String direction = parts.length > 1 ? parts[1].trim() : "asc";

    // Whitelist sortable properties to avoid accidental exposure of internal fields.
    if (!property.equals("username")
        && !property.equals("email")
        && !property.equals("createdAt")
        && !property.equals("updatedAt")
        && !property.equals("lastLoginAt")
        && !property.equals("role")) {
        property = "username";
    }

    Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
    return Sort.by(dir, property);
    }
}
