package com.skilltrack.auth.model.dto;

import com.skilltrack.auth.model.Role;
import com.skilltrack.auth.util.PasswordMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Registration request DTO.
 * Note: Client-side validation should also validate password confirmation.
 */
@Getter
@Setter
@PasswordMatch
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(min = 8, max = 50)
    private String password;

    @NotBlank
    @Size(min = 8, max = 50)
    private String confirmPassword;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 2, max = 120)
    private String fullName;

    @NotNull
    private Role role;
}
