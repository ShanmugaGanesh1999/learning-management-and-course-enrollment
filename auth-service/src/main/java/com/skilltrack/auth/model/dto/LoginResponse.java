package com.skilltrack.auth.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private final String token;
    private final String refreshToken;
    private final UserProfileResponse userDetails;
    private final long expiresIn;
    private final String tokenType;
}
