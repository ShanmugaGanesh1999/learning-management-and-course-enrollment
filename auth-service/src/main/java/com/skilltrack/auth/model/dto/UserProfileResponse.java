package com.skilltrack.auth.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {
    private final Long id;
    private final String username;
    private final String email;
    private final String fullName;
    private final String role;
}
