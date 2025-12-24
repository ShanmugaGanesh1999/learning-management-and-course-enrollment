import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card">
      <h2>Profile</h2>

      <div *ngIf="(auth.currentUser() | async) as user; else empty">
        <div><strong>Username:</strong> {{ user.username }}</div>
        <div><strong>Role:</strong> {{ user.role }}</div>
        <div><strong>Full name:</strong> {{ user.fullName }}</div>
        <div><strong>Email:</strong> {{ user.email }}</div>

        <button (click)="refresh()">Refresh</button>
      </div>

      <ng-template #empty>
        <p>No user loaded.</p>
        <button (click)="refresh()">Load profile</button>
      </ng-template>

      <div class="error" *ngIf="error">{{ error }}</div>
    </div>
  `
})
export class ProfileComponent {
  error: string | null = null;

  constructor(public readonly auth: AuthService) {}

  refresh(): void {
    this.error = null;
    this.auth.me().subscribe({
      next: () => {},
      error: (err) => {
        this.error = err?.message ?? err?.error?.message ?? 'Failed to load profile';
      }
    });
  }
}
