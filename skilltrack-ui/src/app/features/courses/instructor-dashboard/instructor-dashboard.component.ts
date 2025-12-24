import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Subject, catchError, forkJoin, of, takeUntil } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { CourseSummary } from '../../../core/models/course.model';
import { EnrollmentStatsResponse } from '../../../core/models/enrollment.model';
import { PageResponse } from '../../../core/models/page.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { UserProfile } from '../../../core/models/user.model';

@Component({
  selector: 'app-instructor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, PaginationComponent],
  template: `
    <div class="card">
      <h2>Instructor Dashboard</h2>

      <div class="row" style="gap:10px;align-items:center;flex-wrap:wrap;">
        <button type="button" (click)="createCourse()">Create Course</button>
        <div *ngIf="message" style="opacity:0.85;">{{ message }}</div>
      </div>

      <div *ngIf="loading">Loading...</div>
      <div class="error" *ngIf="error">{{ error }}</div>

      <div *ngIf="!loading && courses.length === 0" style="opacity:0.85;">
        No courses yet.
      </div>

      <div *ngIf="page" class="row" style="flex-direction:column;">
        <div *ngFor="let c of courses" class="card">
          <div style="display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;align-items:flex-start;">
            <div>
              <div><strong>{{ c.title }}</strong></div>

              <div style="margin-top:6px;display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
                <span style="padding:2px 8px;border:1px solid #ddd;border-radius:999px;">
                  {{ (c.status || 'DRAFT') }}
                </span>

                <span style="opacity:0.85;">
                  Total enrollments: {{ statsByCourseId[c.id]?.totalEnrollments ?? '—' }}
                </span>

                <span style="opacity:0.85;">
                  Average progress: {{ formatPct(statsByCourseId[c.id]?.averageProgress) }}
                </span>
              </div>
            </div>

            <div class="row" style="gap:8px;align-items:center;flex-wrap:wrap;">
              <button type="button" (click)="onEdit(c.id)">Edit</button>
              <button type="button" (click)="onDelete(c.id)">Delete</button>

              <button
                type="button"
                *ngIf="(c.status || 'DRAFT') === 'DRAFT'"
                (click)="onPublish(c.id)"
                [disabled]="busyId === c.id"
              >
                {{ busyId === c.id ? 'Publishing…' : 'Publish' }}
              </button>

              <button type="button" (click)="viewEnrollments(c.id)">View enrollments</button>
            </div>
          </div>
        </div>

        <app-pagination
          [page]="page"
          (pageChange)="setPage($event)"
          (sizeChange)="setSize($event)"
        ></app-pagination>
      </div>
    </div>
  `
})
export class InstructorDashboardComponent implements OnInit, OnDestroy {
  /**
   * Instructor view:
   * - shows only the authenticated instructor's courses (plus admins if allowed)
   * - provides quick management actions (publish/edit/delete) and enrollment analytics
   */

  loading = false;
  error: string | null = null;
  message: string | null = null;

  // Prompt-required properties
  courses: CourseSummary[] = [];
  totalItems = 0;
  pageSize = 10;

  page: PageResponse<CourseSummary> | null = null;
  busyId: number | null = null;

  statsByCourseId: Record<number, EnrollmentStatsResponse | null> = {};

  private instructorId: number | null = null;
  private pageIndex = 0;
  private readonly destroyed$ = new Subject<void>();

  constructor(
    private readonly auth: AuthService,
    private readonly coursesApi: CourseService,
    private readonly enrollmentsApi: EnrollmentService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.auth
      .getCurrentUser()
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: (u: UserProfile | null) => {
          this.instructorId = u?.id ?? null;
          this.loadCourses();
        },
        error: () => {
          this.error = 'Failed to load instructor profile.';
        }
      });
  }

  setPage(page: number): void {
    this.pageIndex = page;
    this.loadCourses();
  }

  setSize(size: number): void {
    this.pageSize = size;
    this.pageIndex = 0;
    this.loadCourses();
  }

  loadCourses(): void {
    if (!this.instructorId) {
      this.courses = [];
      this.page = null;
      this.totalItems = 0;
      return;
    }

    this.loading = true;
    this.error = null;
    this.message = null;

    this.coursesApi
      .getInstructorCourses(this.instructorId, this.pageIndex, this.pageSize)
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: (res: PageResponse<CourseSummary>) => {
          this.loading = false;
          this.page = res;
          this.courses = res.content ?? [];
          this.totalItems = res.totalElements ?? 0;

          // Fetch analytics (best-effort) for the current page of courses.
          if (this.courses.length === 0) return;

          forkJoin(
            this.courses.map((c) =>
              this.enrollmentsApi.courseStats(c.id).pipe(catchError(() => of(null)))
            )
          )
            .pipe(takeUntil(this.destroyed$))
            .subscribe({
              next: (statsList: Array<EnrollmentStatsResponse | null>) => {
                for (let i = 0; i < this.courses.length; i++) {
                  this.statsByCourseId[this.courses[i].id] = statsList[i];
                }
              }
            });
        },
        error: (err: any) => {
          this.loading = false;
          this.error = err?.message ?? err?.error?.message ?? 'Failed to load instructor courses';
        }
      });
  }

  createCourse(): void {
    void this.router.navigateByUrl('/instructor/courses/new');
  }

  onEdit(courseId: number): void {
    void this.router.navigateByUrl(`/instructor/courses/${courseId}/edit`);
  }

  onDelete(courseId: number): void {
    const ok = window.confirm('Delete this course? This cannot be undone.');
    if (!ok) return;

    this.busyId = courseId;
    this.error = null;
    this.message = null;

    this.coursesApi
      .deleteCourse(courseId)
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: () => {
          this.busyId = null;
          this.message = 'Course deleted.';
          this.loadCourses();
        },
        error: (err: any) => {
          this.busyId = null;
          this.error = err?.message ?? err?.error?.message ?? 'Failed to delete course';
        }
      });
  }

  onPublish(courseId: number): void {
    this.busyId = courseId;
    this.error = null;
    this.message = null;

    this.coursesApi
      .publishCourse(courseId)
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: () => {
          this.busyId = null;
          this.message = 'Course published.';
          this.loadCourses();
        },
        error: (err: any) => {
          this.busyId = null;
          this.error = err?.message ?? err?.error?.message ?? 'Failed to publish course';
        }
      });
  }

  viewEnrollments(courseId: number): void {
    void this.router.navigateByUrl(`/instructor/courses/${courseId}/enrollments`);
  }

  formatPct(value: number | undefined | null): string {
    if (typeof value !== 'number' || Number.isNaN(value)) return '—';
    return `${Math.round(value)}%`;
  }

  ngOnDestroy(): void {
    this.destroyed$.next();
    this.destroyed$.complete();
  }
}
