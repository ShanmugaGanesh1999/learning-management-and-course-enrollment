import { Routes } from '@angular/router';

import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';
import { pendingChangesGuard } from './core/guards/pending-changes.guard';

import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ProfileComponent } from './features/auth/profile/profile.component';

import { CourseListComponent } from './features/courses/course-list/course-list.component';
import { CourseDetailComponent } from './features/courses/course-detail/course-detail.component';
import { CourseCreateComponent } from './features/courses/course-create/course-create.component';
import { CourseEditComponent } from './features/courses/course-edit/course-edit.component';
import { InstructorDashboardComponent } from './features/courses/instructor-dashboard/instructor-dashboard.component';

import { MyCoursesComponent } from './features/enrollments/my-courses/my-courses.component';
import { CourseProgressComponent } from './features/enrollments/course-progress/course-progress.component';
import { EnrollmentsListComponent } from './features/enrollments/enrollments-list/enrollments-list.component';

import { UserManagementComponent } from './features/admin/user-management/user-management.component';
import { CourseManagementComponent } from './features/admin/course-management/course-management.component';
import { AnalyticsComponent } from './features/admin/analytics/analytics.component';
import { UnauthorizedComponent } from './features/unauthorized/unauthorized.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'courses' },

  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  { path: 'courses', component: CourseListComponent },
  { path: 'courses/:id', component: CourseDetailComponent },

  { path: 'profile', canActivate: [AuthGuard], component: ProfileComponent },

  { path: 'my-courses', canActivate: [AuthGuard], component: MyCoursesComponent },
  { path: 'my-courses/:enrollmentId/progress', canActivate: [AuthGuard], component: CourseProgressComponent },

  { path: 'unauthorized', component: UnauthorizedComponent },

  // Instructor (lightweight; relies on backend auth/ownership)
  { path: 'instructor/courses', canActivate: [AuthGuard, RoleGuard], data: { roles: ['INSTRUCTOR', 'ADMIN'] }, component: InstructorDashboardComponent },
  { path: 'instructor/courses/new', canActivate: [AuthGuard, RoleGuard], data: { roles: ['INSTRUCTOR', 'ADMIN'] }, canDeactivate: [pendingChangesGuard], component: CourseCreateComponent },
  { path: 'instructor/courses/:id/edit', canActivate: [AuthGuard, RoleGuard], data: { roles: ['INSTRUCTOR', 'ADMIN'] }, canDeactivate: [pendingChangesGuard], component: CourseEditComponent },
  { path: 'instructor/courses/:courseId/enrollments', canActivate: [AuthGuard, RoleGuard], data: { roles: ['INSTRUCTOR', 'ADMIN'] }, component: EnrollmentsListComponent },

  // Admin
  { path: 'admin/users', canActivate: [AuthGuard, RoleGuard], data: { role: 'ADMIN' }, component: UserManagementComponent },
  { path: 'admin/courses', canActivate: [AuthGuard, RoleGuard], data: { role: 'ADMIN' }, component: CourseManagementComponent },
  { path: 'admin/analytics', canActivate: [AuthGuard, RoleGuard], data: { role: 'ADMIN' }, component: AnalyticsComponent },

  { path: '**', redirectTo: 'courses' }
];
