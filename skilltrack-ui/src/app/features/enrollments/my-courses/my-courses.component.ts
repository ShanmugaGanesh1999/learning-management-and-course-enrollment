import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { EnrollmentService } from '../../../core/services/enrollment.service';
import { PageResponse } from '../../../core/models/page.model';
import { EnrollmentStatus, MyCourseItem } from '../../../core/models/enrollment.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-my-courses',
  standalone: true,
  imports: [CommonModule, RouterLink, PaginationComponent],
  template: `
    <div class="card">
      <h2>My Courses</h2>

      <div class="row" style="gap:8px;flex-wrap:wrap;">
        <button type="button" (click)="onStatusChange('ALL')" [disabled]="selectedStatus === 'ALL'">All</button>
        <button type="button" (click)="onStatusChange('IN_PROGRESS')" [disabled]="selectedStatus === 'IN_PROGRESS'">In Progress</button>
        <button type="button" (click)="onStatusChange('COMPLETED')" [disabled]="selectedStatus === 'COMPLETED'">Completed</button>
        <button type="button" (click)="onStatusChange('CANCELLED')" [disabled]="selectedStatus === 'CANCELLED'">Cancelled</button>
      </div>

      <div *ngIf="loading">Loading...</div>
      <div class="error" *ngIf="error">{{ error }}</div>
      <div *ngIf="message" style="opacity:0.85;">{{ message }}</div>

      <div *ngIf="page && enrollments.length === 0">No enrollments.</div>

      <div *ngIf="page" class="row" style="flex-direction:column;">
        <div *ngFor="let e of enrollments" class="card">
          <div style="display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;align-items:flex-start;">
            <div>
              <div><strong>{{ e.courseTitle || ('Course #' + e.courseId) }}</strong></div>

              <div style="margin-top:6px;display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
                <span style="padding:2px 8px;border:1px solid #ddd;border-radius:999px;">
                  {{ e.status }}
                </span>
                <span style="opacity:0.85;">Last accessed: {{ formatDate(e.enrolledAt) }}</span>
              </div>

              <div style="margin-top:10px;max-width:420px;">
                <div style="display:flex;justify-content:space-between;">
                  <span>Progress</span>
                  <span>{{ e.progressPercentage }}%</span>
                </div>
                <div style="height:10px;border:1px solid #ddd;border-radius:999px;overflow:hidden;">
                  <div [style.width.%]="e.progressPercentage" style="height:10px;background:#ddd;"></div>
                </div>
              </div>
            </div>

            <div class="row" style="gap:8px;align-items:center;flex-wrap:wrap;">
              <button type="button" (click)="continueLearning(e.enrollmentId)" [disabled]="e.status === 'CANCELLED'">
                Continue Learning
              </button>
              <button type="button" (click)="viewProgress(e.enrollmentId)">View progress</button>
              <button
                *ngIf="e.status === 'COMPLETED'"
                type="button"
                (click)="requestCertificate(e.enrollmentId)"
                [disabled]="busyId === e.enrollmentId"
              >
                {{ busyId === e.enrollmentId ? 'Requesting…' : 'Certificate' }}
              </button>
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
export class MyCoursesComponent implements OnInit, OnDestroy {
  loading = false;
  error: string | null = null;
  message: string | null = null;

  page: PageResponse<MyCourseItem> | null = null;
  busyId: number | null = null;

  // Prompt-required properties
  enrollments: MyCourseItem[] = [];
  selectedStatus: 'ALL' | EnrollmentStatus = 'ALL';
  currentPage = 0;

  private pageSize = 10;
  private allEnrollments: MyCourseItem[] = [];
  private readonly destroyed$ = new Subject<void>();

  constructor(
    private readonly enrollmentsApi: EnrollmentService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadMyEnrollments();
  }

  setPage(page: number): void {
    this.currentPage = page;
    this.updatePagedView();
  }

  setSize(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.updatePagedView();
  }

  /**
   * Status filtering:
   * - We keep a single list of enrollments from the API.
   * - Tabs apply a client-side filter (simple + predictable UI behavior).
   */
  loadMyEnrollments(): void {
    this.loading = true;
    this.error = null;
    this.message = null;

    // Fetch a larger slice once; we paginate client-side for the tab filters.
    this.enrollmentsApi.getMyEnrollments(0, 200).pipe(takeUntil(this.destroyed$)).subscribe({
      next: (res: PageResponse<MyCourseItem>) => {
        this.loading = false;
        this.allEnrollments = res.content ?? [];
        this.currentPage = 0;
        this.updatePagedView();
      },
      error: (err: any) => {
        this.loading = false;
        this.error = err?.message ?? err?.error?.message ?? 'Failed to load enrollments';
      }
    });
  }

  onStatusChange(status: 'ALL' | EnrollmentStatus): void {
    this.selectedStatus = status;
    this.currentPage = 0;
    this.updatePagedView();
  }

  private updatePagedView(): void {
    const filtered = this.selectedStatus === 'ALL'
      ? this.allEnrollments
      : this.allEnrollments.filter((e) => e.status === this.selectedStatus);

    const total = filtered.length;
    const totalPages = Math.max(1, Math.ceil(total / this.pageSize));
    const pageNumber = Math.min(this.currentPage, totalPages - 1);
    const start = pageNumber * this.pageSize;
    const end = start + this.pageSize;

    this.enrollments = filtered.slice(start, end);
    this.page = {
      content: this.enrollments,
      pageNumber,
      pageSize: this.pageSize,
      totalElements: total,
      totalPages,
      first: pageNumber === 0,
      last: pageNumber >= totalPages - 1
    };
  }

  continueLearning(enrollmentId: number): void {
    void this.router.navigateByUrl(`/my-courses/${enrollmentId}/progress`);
  }

  viewProgress(enrollmentId: number): void {
    void this.router.navigateByUrl(`/my-courses/${enrollmentId}/progress`);
  }

  requestCertificate(enrollmentId: number): void {
    this.busyId = enrollmentId;
    this.error = null;
    this.message = null;

    this.enrollmentsApi.issueCertificate(enrollmentId).pipe(takeUntil(this.destroyed$)).subscribe({
      next: () => {
        this.busyId = null;
        this.message = 'Certificate requested.';
        this.loadMyEnrollments();
      },
      error: (err: any) => {
        this.busyId = null;
        this.error = err?.message ?? err?.error?.message ?? 'Failed to request certificate';
      }
    });
  }

  formatDate(raw?: string): string {
    if (!raw) return '—';
    try {
      return new Date(raw).toLocaleDateString();
    } catch {
      return raw;
    }
  }

  ngOnDestroy(): void {
    this.destroyed$.next();
    this.destroyed$.complete();
  }
}
