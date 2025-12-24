package com.skilltrack.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating a user's profile information.
 *
 * Security considerations:
 * - This request intentionally does not include password or role changes.
 * - Role changes should be handled via a separate, admin-only workflow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 120)
    private String fullName;
}
