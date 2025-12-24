package com.skilltrack.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for refresh token exchange.
 *
 * Notes:
 * - This API returns a new access token and (optionally) a new refresh token.
 * - In a production-grade implementation, refresh token rotation should be backed
 *   by server-side storage (e.g., DB/Redis) to invalidate previously used tokens.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshResponse {

    private String token;
    private String refreshToken;
    private long expiresIn;
    private String tokenType;
}
