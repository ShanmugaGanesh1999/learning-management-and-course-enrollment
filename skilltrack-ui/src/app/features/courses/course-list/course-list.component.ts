import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { CourseService } from '../../../core/services/course.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { AuthService } from '../../../core/services/auth.service';
import { CourseSummary } from '../../../core/models/course.model';
import { PageResponse } from '../../../core/models/page.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-course-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PaginationComponent, LoadingSpinnerComponent],
  template: `
    <div class="card">
      <h2>Courses</h2>

      <form [formGroup]="filters" class="row" style="align-items:end;gap:12px;flex-wrap:wrap;">
        <label style="display:flex;flex-direction:column;">
          <span style="display:flex;align-items:center;gap:6px;">
            <svg aria-hidden="true" width="14" height="14" viewBox="0 0 24 24" fill="none" style="opacity:0.85;">
              <path d="M10 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16Z" stroke="currentColor" stroke-width="2"/>
              <path d="m21 21-4.35-4.35" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            Search
          </span>
          <input
            formControlName="keyword"
            placeholder="Search courses"
            (input)="onSearch($any($event.target).value)"
          />
        </label>

        <label>
          Category
          <select formControlName="category">
            <option value="">All</option>
            <option value="PROGRAMMING">PROGRAMMING</option>
            <option value="DATA_SCIENCE">DATA_SCIENCE</option>
            <option value="DESIGN">DESIGN</option>
            <option value="BUSINESS">BUSINESS</option>
            <option value="MARKETING">MARKETING</option>
            <option value="DEVOPS">DEVOPS</option>
            <option value="OTHER">OTHER</option>
          </select>
        </label>

        <label>
          Level
          <select formControlName="level">
            <option value="">All</option>
            <option value="BEGINNER">BEGINNER</option>
            <option value="INTERMEDIATE">INTERMEDIATE</option>
            <option value="ADVANCED">ADVANCED</option>
          </select>
        </label>

        <label>
          Sort
          <select formControlName="sort">
            <option value="title,asc">Title (A-Z)</option>
            <option value="title,desc">Title (Z-A)</option>
            <option value="createdAt,desc">Date (newest)</option>
            <option value="createdAt,asc">Date (oldest)</option>
            <option value="level,asc">Level (A-Z)</option>
            <option value="level,desc">Level (Z-A)</option>
          </select>
        </label>

        <div style="display:flex;gap:8px;align-items:center;">
          <button type="button" (click)="gridView = true" [disabled]="gridView">Grid</button>
          <button type="button" (click)="gridView = false" [disabled]="!gridView">List</button>
        </div>
      </form>

      <app-loading-spinner [show]="loading"></app-loading-spinner>
      <div class="error" *ngIf="error">{{ error }}</div>

      <div *ngIf="page && courses.length === 0">No courses found.</div>

      <div *ngIf="page">
        <div
          class="row"
          [style.flexDirection]="gridView ? 'row' : 'column'"
          style="gap:12px;flex-wrap:wrap;align-items:stretch;"
        >
          <div
            *ngFor="let c of courses"
            class="card"
            [style.width]="gridView ? 'min(320px, 100%)' : '100%'"
          >
            <div style="display:flex;gap:12px;align-items:flex-start;">
              <div
                aria-label="Course thumbnail"
                style="width:72px;height:72px;border:1px solid #ddd;border-radius:6px;display:flex;align-items:center;justify-content:center;opacity:0.7;"
              >
                Thumbnail
              </div>

              <div style="flex:1;">
                <div><strong>{{ c.title }}</strong></div>
                <small>{{ c.category }} · {{ c.level }}</small>
                <div style="margin-top:6px;opacity:0.85;">
                  Instructor: {{ c.instructorId ? ('#' + c.instructorId) : '—' }}
                </div>
                <div style="opacity:0.85;">Price: {{ (c as any).price ?? '—' }}</div>
              </div>
            </div>

            <div class="row" style="justify-content:space-between;align-items:center;margin-top:10px;">
              <a [routerLink]="['/courses', c.id]">View</a>

              <button
                *ngIf="showEnrollButton(c.id)"
                type="button"
                (click)="enroll(c.id)"
                [disabled]="enrollingCourseIds.has(c.id)"
              >
                {{ enrollingCourseIds.has(c.id) ? 'Enrolling…' : 'Enroll' }}
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
export class CourseListComponent implements OnInit, OnDestroy {
  loading = false;
  error: string | null = null;

  page: PageResponse<CourseSummary> | null = null;

  // Prompt-required component state
  courses: CourseSummary[] = [];
  totalItems = 0;
  pageSize = 10;
  currentPage = 0;
  sortBy = 'createdAt,desc';

  gridView = true;

  enrolledCourseIds = new Set<number>();
  enrollingCourseIds = new Set<number>();

  private readonly destroyed$ = new Subject<void>();
  private readonly search$ = new Subject<string>();

  filters = this.fb.group({
    keyword: [''],
    category: [''],
    level: [''],
    sort: ['createdAt,desc']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly coursesApi: CourseService,
    private readonly enrollments: EnrollmentService,
    private readonly auth: AuthService
  ) {}

  ngOnInit(): void {
    // Debounce search input so we don’t call the API on every keystroke.
    this.search$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroyed$))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadCourses();
      });

    // Filter/sort changes reset pagination immediately (no debounce needed).
    this.filters.controls.category.valueChanges.pipe(takeUntil(this.destroyed$)).subscribe(() => this.onFilterChange());
    this.filters.controls.level.valueChanges.pipe(takeUntil(this.destroyed$)).subscribe(() => this.onFilterChange());
    this.filters.controls.sort.valueChanges.pipe(takeUntil(this.destroyed$)).subscribe(() => this.onFilterChange());

    this.loadCourses();
    this.loadEnrolledCoursesBestEffort();
  }

  setPage(page: number): void {
    this.onPageChange(page);
  }

  setSize(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadCourses();
  }

  /**
   * Pagination handling:
   * - API uses 0-based `page`
   * - when filters/search change we reset to page 0 to avoid empty pages
   */
  loadCourses(): void {
    const v = this.filters.getRawValue();

    this.sortBy = v.sort ?? 'createdAt,desc';

    this.loading = true;
    this.error = null;

    this.coursesApi
      .getCourses(this.currentPage, this.pageSize, this.sortBy, {
        keyword: v.keyword?.trim() ? v.keyword.trim() : undefined,
        category: v.category ? v.category : undefined,
        level: v.level ? v.level : undefined
      })
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: (res: PageResponse<CourseSummary>) => {
          this.loading = false;
          this.page = res;
          this.courses = res.content;
          this.totalItems = res.totalElements;
        },
        error: (err: any) => {
          this.loading = false;
          this.error = err?.message ?? err?.error?.message ?? 'Failed to load courses';
        }
      });
  }

  onSearch(keyword: string): void {
    // Debounced via search$ stream.
    this.search$.next(keyword);
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadCourses();
  }

  onPageChange(newPage: number): void {
    this.currentPage = newPage;
    this.loadCourses();
  }

  showEnrollButton(courseId: number): boolean {
    // Enroll is meaningful only for authenticated students.
    if (!this.auth.isLoggedIn() || !this.auth.hasRole('STUDENT')) return false;
    return !this.enrolledCourseIds.has(courseId);
  }

  enroll(courseId: number): void {
    if (this.enrollingCourseIds.has(courseId)) return;

    this.enrollingCourseIds.add(courseId);
    this.enrollments.enrollCourse(courseId).pipe(takeUntil(this.destroyed$)).subscribe({
      next: () => {
        this.enrollingCourseIds.delete(courseId);
        this.enrolledCourseIds.add(courseId);
      },
      error: (err: any) => {
        this.enrollingCourseIds.delete(courseId);
        this.error = err?.message ?? err?.error?.message ?? 'Enroll failed';
      }
    });
  }

  private loadEnrolledCoursesBestEffort(): void {
    if (!this.auth.isLoggedIn() || !this.auth.hasRole('STUDENT')) return;

    // Best-effort: fetch first page of enrollments and build a quick lookup set.
    this.enrollments
      .getMyEnrollments(0, 100)
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: (p: any) => {
          const ids = new Set<number>();
          for (const item of p.content ?? []) {
            if (typeof item.courseId === 'number') ids.add(item.courseId);
          }
          this.enrolledCourseIds = ids;
        },
        error: () => {
          // ignore
        }
      });
  }

  ngOnDestroy(): void {
    this.destroyed$.next();
    this.destroyed$.complete();
  }
}
