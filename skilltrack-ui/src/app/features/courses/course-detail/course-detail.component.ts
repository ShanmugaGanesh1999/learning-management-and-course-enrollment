import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { CourseService } from '../../../core/services/course.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { AuthService } from '../../../core/services/auth.service';
import { CourseDetail, Lesson, Module } from '../../../core/models/course.model';

@Component({
  selector: 'app-course-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="card">
      <a routerLink="/courses">Back</a>

      <div *ngIf="loading">Loading...</div>
      <div class="error" *ngIf="error">{{ error }}</div>

      <div *ngIf="course">
        <div>
          <h2>{{ course.title }}</h2>
          <p>{{ course.description }}</p>
          <div style="opacity:0.85;">
            Level: {{ course.level }} · Instructor: {{ course.instructorId ? ('#' + course.instructorId) : '—' }}
          </div>
        </div>

        <div class="row" style="margin-top:12px;align-items:center;gap:10px;flex-wrap:wrap;">
          <button *ngIf="!enrolled && canEnroll" (click)="onEnroll()" [disabled]="enrolling">
            {{ enrolling ? 'Enrolling…' : 'Enroll' }}
          </button>

          <div *ngIf="enrolled" style="min-width:240px;">
            <div style="display:flex;justify-content:space-between;">
              <span>Progress</span>
              <span>{{ progress }}%</span>
            </div>
            <div style="height:10px;border:1px solid #ddd;border-radius:999px;overflow:hidden;">
              <div [style.width.%]="progress" style="height:10px;background:#ddd;"></div>
            </div>
          </div>

          <button *ngIf="enrolled" type="button" (click)="startLearning()">Start Learning</button>

          <span *ngIf="message" style="opacity:0.85;">{{ message }}</span>
        </div>

        <h3 style="margin-top:16px;">Modules</h3>
        <div *ngIf="!modules || modules.length === 0">No modules.</div>

        <div class="row" style="gap:12px;flex-wrap:wrap;align-items:flex-start;">
          <div style="flex:1;min-width:min(360px, 100%);">
            <details *ngFor="let m of modules" class="card" open>
              <summary><strong>{{ m.title }}</strong></summary>
              <div *ngIf="!m.lessons || m.lessons.length === 0">No lessons.</div>
              <ul *ngIf="m.lessons && m.lessons.length > 0">
                <li *ngFor="let l of m.lessons">
                  <a href="#" (click)="$event.preventDefault(); selectLesson(m, l)">{{ l.title }}</a>
                </li>
              </ul>
            </details>
          </div>

          <div style="flex:1;min-width:min(360px, 100%);">
            <div class="card">
              <h3 style="margin-top:0;">Lesson</h3>
              <div *ngIf="!selectedLesson">
                Select a lesson to view its content. (Video placeholder)
              </div>
              <div *ngIf="selectedLesson">
                <div><strong>{{ selectedLesson.title }}</strong></div>
                <div style="margin-top:8px;opacity:0.85;">
                  Video placeholder: {{ selectedLesson.videoUrl ?? '—' }}
                </div>
                <div style="margin-top:8px;white-space:pre-wrap;">
                  {{ selectedLesson.content ?? 'Lesson content placeholder.' }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class CourseDetailComponent implements OnInit, OnDestroy {
  loading = false;
  enrolling = false;
  error: string | null = null;
  message: string | null = null;

  course: CourseDetail | null = null;

  // Prompt-required state
  enrolled = false;
  progress = 0;

  modules: Module[] = [];
  selectedLesson: Lesson | null = null;

  private readonly destroyed$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly courses: CourseService,
    private readonly enrollments: EnrollmentService,
    private readonly auth: AuthService
  ) {}

  get canEnroll(): boolean {
    // Backend enforces STUDENT role; UI just checks auth.
    return this.auth.isAuthenticated() && this.auth.hasRole('STUDENT');
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadCourse(id);
  }

  /**
   * Nested data handling:
   * - Backend returns a wrapper object: { course, modules }
   * - We normalize it into a single CourseDetail + modules array for rendering.
   */
  loadCourse(id: number): void {
    this.loading = true;
    this.error = null;

    this.courses.getCourseById(id).pipe(takeUntil(this.destroyed$)).subscribe({
      next: (res: any) => {
        this.loading = false;
        const r: any = res as any;
        const normalized = r?.course ? ({ ...r.course, modules: r.modules ?? [] } as CourseDetail) : (r as CourseDetail);
        this.course = normalized;
        this.modules = normalized.modules ?? [];
        this.selectedLesson = null;

        this.checkEnrollmentBestEffort(normalized.id);
      },
      error: (err: any) => {
        this.loading = false;
        this.error = err?.message ?? err?.error?.message ?? 'Failed to load course';
      }
    });
  }

  onEnroll(): void {
    if (!this.course || this.enrolling) return;

    this.enrolling = true;
    this.message = null;

    this.enrollments.enrollCourse(this.course.id).pipe(takeUntil(this.destroyed$)).subscribe({
      next: () => {
        this.enrolling = false;
        this.enrolled = true;
        this.progress = 0;
        this.message = 'Enrolled successfully';
      },
      error: (err: any) => {
        this.enrolling = false;
        this.message = err?.message ?? err?.error?.message ?? 'Enroll failed';
      }
    });
  }

  selectLesson(_module: Module, lesson: Lesson): void {
    this.selectedLesson = lesson;
  }

  startLearning(): void {
    // Placeholder action; a future iteration could route to a dedicated lesson player.
    this.message = 'Start Learning (placeholder)';
  }

  private checkEnrollmentBestEffort(courseId: number): void {
    this.enrolled = false;
    this.progress = 0;
    if (!this.auth.isLoggedIn() || !this.auth.hasRole('STUDENT')) return;

    this.enrollments
      .getMyEnrollments(0, 100)
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: (p: any) => {
          const item = (p.content ?? []).find((x) => x.courseId === courseId);
          if (item) {
            this.enrolled = true;
            this.progress = item.progressPercentage ?? 0;
          }
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
