import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { emailOrUsernameValidator } from '../../../shared/validators/auth.validators';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LoadingSpinnerComponent],
  template: `
    <div class="card">
      <h2>Login</h2>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="row" style="flex-direction:column;max-width:420px;">
        <label>
          Email or username
          <input formControlName="identifier" autocomplete="username" />
        </label>

        <div class="error" *ngIf="form.controls.identifier.touched && form.controls.identifier.invalid">
          <div *ngIf="form.controls.identifier.errors?.['required']">Identifier is required</div>
          <div *ngIf="form.controls.identifier.errors?.['minlength']">Must be at least 3 characters</div>
          <div *ngIf="form.controls.identifier.errors?.['email']">Enter a valid email address</div>
          <div *ngIf="form.controls.identifier.errors?.['username']">Username can contain letters, numbers, . _ -</div>
        </div>

        <label>
          Password
          <input type="password" formControlName="password" autocomplete="current-password" />
        </label>

        <div class="error" *ngIf="form.controls.password.touched && form.controls.password.invalid">
          <div *ngIf="form.controls.password.errors?.['required']">Password is required</div>
          <div *ngIf="form.controls.password.errors?.['minlength']">Must be at least 6 characters</div>
        </div>

        <label style="display:flex;align-items:center;gap:8px;">
          <input type="checkbox" formControlName="rememberMe" />
          Remember me (placeholder)
        </label>

        <p>
          <a href="#" (click)="$event.preventDefault()">Forgot password?</a>
          <span style="opacity:0.7;">(placeholder)</span>
        </p>

        <div class="error" *ngIf="error">{{ error }}</div>

        <button type="submit" [disabled]="loading || form.invalid">{{ loading ? 'Logging in…' : 'Login' }}</button>
        <app-loading-spinner [show]="loading"></app-loading-spinner>

        <p>
          No account? <a routerLink="/register">Register</a>
        </p>
      </form>
    </div>
  `
})
export class LoginComponent {
  loading = false;
  error: string | null = null;

  /**
   * Reactive forms pattern:
   * - FormGroup holds the form state
   * - Validators run synchronously and keep UI + submission logic consistent
   */
  form = this.fb.group({
    identifier: ['', [Validators.required, Validators.minLength(3), emailOrUsernameValidator()]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    rememberMe: [false]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  onSubmit(): void {
    if (this.form.invalid || this.loading) return;

    this.loading = true;
    this.error = null;

    const v = this.form.getRawValue();
    this.auth.login({ username: v.identifier, password: v.password }).subscribe({
      next: () => {
        this.loading = false;
        // “Dashboard” route (simplest): course catalog.
        void this.router.navigateByUrl('/courses');
      },
      error: (err: any) => {
        this.loading = false;
        this.error = err?.message ?? err?.error?.message ?? 'Login failed';
      }
    });
  }
}
