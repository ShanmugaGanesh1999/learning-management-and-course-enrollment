import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Allows either:
 * - an email address, OR
 * - a username (letters, numbers, dot, underscore, dash; 3-50 chars)
 */
export function emailOrUsernameValidator(): ValidatorFn {
  const usernameRegex = /^[a-zA-Z0-9._-]{3,50}$/;
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  return (control: AbstractControl): ValidationErrors | null => {
    const raw = String(control.value ?? '').trim();
    if (!raw) return null;

    const isEmail = raw.includes('@');
    if (isEmail) {
      return emailRegex.test(raw) ? null : { email: true };
    }

    return usernameRegex.test(raw) ? null : { username: true };
  };
}

/**
 * Password strength validator.
 *
 * Rule of thumb (simple and explainable):
 * - >= 8 chars
 * - includes upper + lower + number
 */
export function passwordStrengthValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = String(control.value ?? '');
    if (!v) return null;

    const lengthOk = v.length >= 8;
    const hasLower = /[a-z]/.test(v);
    const hasUpper = /[A-Z]/.test(v);
    const hasDigit = /\d/.test(v);

    const ok = lengthOk && hasLower && hasUpper && hasDigit;
    return ok
      ? null
      : {
          weakPassword: {
            lengthOk,
            hasLower,
            hasUpper,
            hasDigit
          }
        };
  };
}

/**
 * FormGroup-level validator to ensure password and confirm password match.
 */
export function passwordMatchValidator(passwordKey: string, confirmKey: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const password = control.get(passwordKey)?.value;
    const confirm = control.get(confirmKey)?.value;

    if (!password || !confirm) return null;
    return password === confirm ? null : { passwordMismatch: true };
  };
}

/**
 * Alias to match the “@PasswordMatch” naming used in many Angular examples.
 * (Angular doesn't use decorators for validators; we attach this ValidatorFn to a FormGroup.)
 */
export function PasswordMatch(passwordKey: string, confirmKey: string): ValidatorFn {
  return passwordMatchValidator(passwordKey, confirmKey);
}
