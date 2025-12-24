import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService, RegisterRequest } from '../../../core/services/auth.service';
import { PasswordMatch, passwordStrengthValidator } from '../../../shared/validators/auth.validators';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="card">
      <h2>Register</h2>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="row" style="flex-direction:column;max-width:520px;">
        <label>
          Full name
          <input formControlName="fullName" />
        </label>

        <div class="error" *ngIf="form.controls.fullName.touched && form.controls.fullName.invalid">
          <div *ngIf="form.controls.fullName.errors?.['required']">Full name is required</div>
        </div>

        <label>
          Username
          <input formControlName="username" />
        </label>

        <div class="error" *ngIf="form.controls.username.touched && form.controls.username.invalid">
          <div *ngIf="form.controls.username.errors?.['required']">Username is required</div>
          <div *ngIf="form.controls.username.errors?.['minlength']">Must be at least 3 characters</div>
        </div>

        <label>
          Email
          <input formControlName="email" autocomplete="email" />
        </label>

        <div class="error" *ngIf="form.controls.email.touched && form.controls.email.invalid">
          <div *ngIf="form.controls.email.errors?.['required']">Email is required</div>
          <div *ngIf="form.controls.email.errors?.['email']">Enter a valid email address</div>
        </div>

        <label>
          Role
          <select formControlName="role">
            <option value="STUDENT">Student</option>
            <option value="INSTRUCTOR">Instructor</option>
          </select>
        </label>

        <div class="error" *ngIf="form.controls.role.touched && form.controls.role.invalid">
          <div *ngIf="form.controls.role.errors?.['required']">Role is required</div>
        </div>

        <label>
          Password
          <input type="password" formControlName="password" autocomplete="new-password" />
        </label>

        <div *ngIf="form.controls.password.value" style="opacity:0.85;">
          <strong>Password strength:</strong> {{ passwordStrengthLabel }}
        </div>

        <div class="error" *ngIf="form.controls.password.touched && form.controls.password.invalid">
          <div *ngIf="form.controls.password.errors?.['required']">Password is required</div>
          <div *ngIf="form.controls.password.errors?.['weakPassword']">
            Password must be at least 8 characters and include upper, lower, and a number.
          </div>
        </div>

        <label>
          Confirm password
          <input type="password" formControlName="confirmPassword" autocomplete="new-password" />
        </label>

        <div class="error" *ngIf="form.controls.confirmPassword.touched && form.controls.confirmPassword.invalid">
          <div *ngIf="form.controls.confirmPassword.errors?.['required']">Confirm your password</div>
        </div>

        <div class="error" *ngIf="form.touched && form.errors?.['passwordMismatch']">
          Passwords do not match
        </div>

        <label style="display:flex;align-items:center;gap:8px;">
          <input type="checkbox" formControlName="acceptTerms" />
          I agree to the terms and conditions
        </label>

        <div class="error" *ngIf="form.controls.acceptTerms.touched && form.controls.acceptTerms.invalid">
          <div *ngIf="form.controls.acceptTerms.errors?.['required']">You must accept the terms to continue</div>
        </div>

        <div class="error" *ngIf="error">{{ error }}</div>
        <div *ngIf="success" style="opacity:0.85;">{{ success }}</div>

        <button type="submit" [disabled]="loading || form.invalid">
          {{ loading ? 'Creating account…' : 'Create account' }}
        </button>

        <p>
          Already have an account? <a routerLink="/login">Login</a>
        </p>
      </form>
    </div>
  `
})
export class RegisterComponent {
  loading = false;
  error: string | null = null;
  success: string | null = null;

  /**
   * Reactive forms pattern:
   * - Field validators live next to the form definition
   * - Cross-field rules (like password match) live at the FormGroup level
   */
  form = this.fb.group({
    fullName: ['', [Validators.required]],
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    role: ['STUDENT' as RegisterRequest['role'], [Validators.required]],
    password: ['', [Validators.required, passwordStrengthValidator()]],
    confirmPassword: ['', [Validators.required]],
    acceptTerms: [false, [Validators.requiredTrue]]
  }, { validators: [PasswordMatch('password', 'confirmPassword')] });

  constructor(
    private readonly fb: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  get passwordStrengthLabel(): string {
    const e = this.form.controls.password.errors?.['weakPassword'] as
      | { lengthOk?: boolean; hasLower?: boolean; hasUpper?: boolean; hasDigit?: boolean }
      | undefined;

    if (!this.form.controls.password.value) return '—';
    if (!e) return 'Strong';

    const score = [e.lengthOk, e.hasLower, e.hasUpper, e.hasDigit].filter(Boolean).length;
    if (score <= 1) return 'Weak';
    if (score <= 3) return 'Medium';
    return 'Medium';
  }

  onSubmit(): void {
    if (this.form.invalid || this.loading) return;

    this.loading = true;
    this.error = null;
    this.success = null;

    const v = this.form.getRawValue();
    const req: RegisterRequest = {
      fullName: v.fullName,
      username: v.username,
      email: v.email,
      role: v.role,
      password: v.password
    };

    this.auth.register(req).subscribe({
      next: () => {
        this.loading = false;
        this.success = 'Account created. Redirecting to login…';
        window.setTimeout(() => void this.router.navigateByUrl('/login'), 800);
      },
      error: (err: any) => {
        this.loading = false;
        this.error = err?.message ?? err?.error?.message ?? 'Registration failed';
      }
    });
  }
}
