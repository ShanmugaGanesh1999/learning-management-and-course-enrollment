import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="card">
      <h2>Unauthorized</h2>
      <p>You do not have permission to view this page.</p>
      <a routerLink="/courses">Go to courses</a>
    </div>
  `
})
export class UnauthorizedComponent {}
