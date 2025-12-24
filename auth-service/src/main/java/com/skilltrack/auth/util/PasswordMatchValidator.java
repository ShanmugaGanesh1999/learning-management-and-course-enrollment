package com.skilltrack.auth.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Generic validator used by request DTOs that carry a (password, confirmPassword) pair.
 *
 * Security note: this only ensures consistency between fields; it does not enforce
 * password strength rules beyond @Size / other Bean Validation constraints.
 */
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            Object passwordObj = value.getClass().getMethod("getPassword").invoke(value);
            Object confirmPasswordObj = value.getClass().getMethod("getConfirmPassword").invoke(value);

            String password = passwordObj == null ? null : String.valueOf(passwordObj);
            String confirmPassword = confirmPasswordObj == null ? null : String.valueOf(confirmPasswordObj);

            if (password == null || confirmPassword == null) {
                return false;
            }
            return password.equals(confirmPassword);
        } catch (Exception ex) {
            // If the DTO does not expose the expected getters, treat it as invalid.
            return false;
        }
    }
}
