import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { EnrollmentService } from '../../../core/services/enrollment.service';
import { EnrollmentResponse } from '../../../core/models/enrollment.model';
import { PageResponse } from '../../../core/models/page.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-enrollments-list',
  standalone: true,
  imports: [CommonModule, RouterLink, PaginationComponent],
  template: `
    <div class="card">
      <a routerLink="/instructor/courses">Back</a>
      <h2>Course Enrollments</h2>

      <div class="row" style="gap:10px;align-items:center;flex-wrap:wrap;">
        <button type="button" (click)="exportEnrollments()">Export enrollments</button>
        <span style="opacity:0.85;">(placeholder)</span>
      </div>

      <div *ngIf="loading">Loading...</div>
      <div class="error" *ngIf="error">{{ error }}</div>

      <div *ngIf="pagination && enrollments.length === 0" style="opacity:0.85;">No enrollments.</div>

      <div *ngIf="pagination" class="card" style="overflow:auto;">
        <table style="width:100%;border-collapse:collapse;min-width:840px;">
          <thead>
            <tr>
              <th style="text-align:left;padding:8px;border-bottom:1px solid #eee;">Student name</th>
              <th style="text-align:left;padding:8px;border-bottom:1px solid #eee;">Enrollment date</th>
              <th style="text-align:left;padding:8px;border-bottom:1px solid #eee;">Progress</th>
              <th style="text-align:left;padding:8px;border-bottom:1px solid #eee;">Status</th>
              <th style="text-align:left;padding:8px;border-bottom:1px solid #eee;">Last accessed</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let e of enrollments">
              <td style="padding:8px;border-bottom:1px solid #f2f2f2;">
                {{ e.studentName || ('Student #' + e.studentId) }}
              </td>
              <td style="padding:8px;border-bottom:1px solid #f2f2f2;">{{ formatDate(e.enrolledAt) }}</td>
              <td style="padding:8px;border-bottom:1px solid #f2f2f2;">
                <div style="display:flex;align-items:center;gap:10px;">
                  <div style="width:160px;height:10px;border:1px solid #ddd;border-radius:999px;overflow:hidden;">
                    <div [style.width.%]="e.progressPercentage" style="height:10px;background:#ddd;"></div>
                  </div>
                  <span>{{ e.progressPercentage }}%</span>
                </div>
              </td>
              <td style="padding:8px;border-bottom:1px solid #f2f2f2;">
                <span style="padding:2px 8px;border:1px solid #ddd;border-radius:999px;">{{ e.status }}</span>
              </td>
              <td style="padding:8px;border-bottom:1px solid #f2f2f2;">{{ formatDate(e.lastAccessedAt || e.enrolledAt) }}</td>
            </tr>
          </tbody>
        </table>

        <app-pagination
          [page]="pagination"
          (pageChange)="setPage($event)"
          (sizeChange)="setSize($event)"
        ></app-pagination>
      </div>
    </div>
  `
})
export class EnrollmentsListComponent implements OnInit, OnDestroy {
  /**
   * Instructor analytics view:
   * - lists enrollments for a specific course
   * - provides quick progress/status visibility per student
   */

  loading = false;
  error: string | null = null;

  // Prompt-required properties
  enrollments: EnrollmentResponse[] = [];
  courseId = 0;
  pagination: PageResponse<EnrollmentResponse> | null = null;

  private pageIndex = 0;
  private pageSize = 10;
  private readonly destroyed$ = new Subject<void>();

  constructor(private readonly route: ActivatedRoute, private readonly enrollments: EnrollmentService) {
    this.courseId = Number(this.route.snapshot.paramMap.get('courseId'));
  }

  ngOnInit(): void {
    this.loadEnrollments();
  }

  setPage(page: number): void {
    this.pageIndex = page;
    this.loadEnrollments();
  }

  setSize(size: number): void {
    this.pageSize = size;
    this.pageIndex = 0;
    this.loadEnrollments();
  }

  loadEnrollments(): void {
    this.loading = true;
    this.error = null;

    this.enrollments
      .courseEnrollments(this.courseId, { page: this.pageIndex, size: this.pageSize, sort: 'enrolledAt,desc', status: null })
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: (res: PageResponse<EnrollmentResponse>) => {
          this.loading = false;
          this.pagination = res;
          this.enrollments = res.content ?? [];
        },
        error: (err: any) => {
          this.loading = false;
          this.error = err?.message ?? err?.error?.message ?? 'Failed to load enrollments';
        }
      });
  }

  exportEnrollments(): void {
    // Placeholder for future export functionality.
    window.alert('Export is not implemented yet.');
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
