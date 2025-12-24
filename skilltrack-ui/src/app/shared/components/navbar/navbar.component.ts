import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AsyncPipe, NgIf } from '@angular/common';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgIf, AsyncPipe],
  template: `
    <header>
      <div class="container row" style="align-items:center;justify-content:space-between;">
        <a routerLink="/courses" style="text-decoration:none;font-weight:700;">SkillTrack</a>

        <nav class="row" style="align-items:center;">
          <a routerLink="/courses" routerLinkActive="active">Courses</a>
          <a *ngIf="(auth.currentUser() | async)" routerLink="/my-courses">My Courses</a>
          <a *ngIf="(auth.currentUser() | async)" routerLink="/profile">Profile</a>
          <a *ngIf="!(auth.currentUser() | async)" routerLink="/login">Login</a>
          <a *ngIf="!(auth.currentUser() | async)" routerLink="/register">Register</a>
          <button *ngIf="(auth.currentUser() | async)" (click)="logout()">Logout</button>
        </nav>
      </div>
    </header>
  `
})
export class NavbarComponent {
  constructor(public readonly auth: AuthService) {}

  logout(): void {
    this.auth.logout();
  }
}
