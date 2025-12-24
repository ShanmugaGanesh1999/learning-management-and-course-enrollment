package com.skilltrack.auth.model.dto;

import com.skilltrack.auth.util.PasswordMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for changing a user's password.
 *
 * Security considerations:
 * - Requires the current password to mitigate account-takeover via a stolen JWT.
 * - New password is never logged and is only accepted over HTTPS in production.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@PasswordMatch
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(min = 8, max = 100)
    private String confirmPassword;
}
