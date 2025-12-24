import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card">
      <h2>Analytics</h2>
      <p>
        Course-level analytics are available via the enrollments stats endpoint.
      </p>
      <p>
        Navigate to a course's enrollments view to see live data.
      </p>
    </div>
  `
})
export class AnalyticsComponent {}
