import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-course-management',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="card">
      <h2>Course Management</h2>
      <p>
        Use the instructor routes to create/edit/publish courses.
      </p>
      <a routerLink="/instructor/courses/new">Create a course</a>
    </div>
  `
})
export class CourseManagementComponent {}
