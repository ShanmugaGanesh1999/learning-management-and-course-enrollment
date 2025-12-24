import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, forkJoin, takeUntil } from 'rxjs';

import { EnrollmentService } from '../../../core/services/enrollment.service';
import { EnrollmentResponse } from '../../../core/models/enrollment.model';
import { CourseService } from '../../../core/services/course.service';
import { Lesson, Module } from '../../../core/models/course.model';

@Component({
  selector: 'app-course-progress',
  standalone: true,
  imports: [CommonModule, RouterLink],
  styles: [
    `
    .confetti {
      position: relative;
      height: 0;
    }
    .confetti i {
      position: absolute;
      top: -6px;
      width: 8px;
      height: 14px;
      opacity: 0.9;
      background: #ddd;
      animation: fall 700ms ease-out forwards;
    }
    @keyframes fall {
      from { transform: translateY(0) rotate(0deg); opacity: 1; }
      to { transform: translateY(70px) rotate(200deg); opacity: 0; }
    }
    `
  ],
  template: `
    <div class="card">
      <a routerLink="/my-courses">Back</a>

      <h2>Progress</h2>

      <div *ngIf="loading">Loading...</div>
      <div class="error" *ngIf="error">{{ error }}</div>

      <div class="error" *ngIf="message" style="opacity:0.85;">{{ message }}</div>

      <div *ngIf="enrollment">
        <div><strong>{{ courseTitle || ('Course #' + enrollment.courseId) }}</strong></div>

        <div style="margin-top:10px;max-width:520px;">
          <div style="display:flex;justify-content:space-between;">
            <span>Overall progress</span>
            <span>{{ currentProgress }}%</span>
          </div>
          <div style="height:10px;border:1px solid #ddd;border-radius:999px;overflow:hidden;">
            <div [style.width.%]="currentProgress" style="height:10px;background:#ddd;"></div>
          </div>
        </div>

        <div style="margin-top:10px;opacity:0.85;">
          Current progress: {{ currentProgress }}% · Time estimate: {{ timeEstimateLabel }}
        </div>

        <div *ngIf="showConfetti" class="confetti" aria-label="Confetti">
          <i *ngFor="let p of confettiPieces; let idx = index" [style.left.px]="idx * 14"></i>
        </div>

        <div class="row" style="gap:10px;align-items:center;margin-top:12px;flex-wrap:wrap;">
          <button type="button" (click)="updateProgress()" [disabled]="busy">
            {{ busy ? 'Updating…' : 'Update progress' }}
          </button>
          <button
            *ngIf="enrollment.status === 'COMPLETED'"
            type="button"
            (click)="certificate()"
            [disabled]="busy"
          >
            Certificate
          </button>
        </div>

        <h3 style="margin-top:16px;">Modules</h3>
        <div *ngIf="modules.length === 0" style="opacity:0.85;">No modules.</div>

        <details *ngFor="let m of modules" class="card" open>
          <summary><strong>{{ m.title }}</strong></summary>

          <div *ngIf="!m.lessons || m.lessons.length === 0" style="opacity:0.85;">No lessons.</div>

          <div *ngFor="let l of (m.lessons ?? [])" style="display:flex;gap:10px;align-items:center;">
            <input
              type="checkbox"
              [checked]="isLessonCompleted(l.id)"
              (change)="updateLessonProgress(l.id, $any($event.target).checked)"
            />
            <span>{{ l.title }}</span>
          </div>
        </details>
      </div>
    </div>
  `
})
export class CourseProgressComponent implements OnInit, OnDestroy {
  loading = false;
  busy = false;
  error: string | null = null;
  message: string | null = null;

  // Prompt-required properties
  enrollment: EnrollmentResponse | null = null;
  modules: Module[] = [];
  currentProgress = 0;

  courseTitle: string | null = null;

  private readonly enrollmentId: number;
  private readonly completedLessonIds = new Set<number>();
  private readonly destroyed$ = new Subject<void>();

  showConfetti = false;
  confettiPieces = Array.from({ length: 18 });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly enrollments: EnrollmentService,
    private readonly courses: CourseService
  ) {
    this.enrollmentId = Number(this.route.snapshot.paramMap.get('enrollmentId'));
  }

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = null;
    this.message = null;

    forkJoin({
      enrollment: this.enrollments.getEnrollment(this.enrollmentId),
    })
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: ({ enrollment }: { enrollment: EnrollmentResponse }) => {
          this.enrollment = enrollment;
          this.currentProgress = enrollment.progressPercentage ?? 0;

          this.courses
            .getCourseById(enrollment.courseId)
            .pipe(takeUntil(this.destroyed$))
            .subscribe({
              next: (res: any) => {
                const r: any = res as any;
                const normalized = r?.course ? r.course : r;
                this.courseTitle = normalized?.title ?? null;
                this.modules = (r?.modules ?? normalized?.modules ?? []) as Module[];

                // If enrollment is already complete, mark all lessons as complete.
                if ((enrollment.progressPercentage ?? 0) >= 100) {
                  for (const m of this.modules) {
                    for (const l of m.lessons ?? []) {
                      if (typeof l.id === 'number') this.completedLessonIds.add(l.id);
                    }
                  }
                  this.calculateProgress();
                }

                this.loading = false;
              },
              error: () => {
                // Course details are best-effort for progress UI.
                this.loading = false;
              }
            });
        },
        error: (err: any) => {
          this.loading = false;
          this.error = err?.message ?? err?.error?.message ?? 'Failed to load enrollment';
        }
      });
  }

  /**
   * Progress calculation:
   * - count completed lessons vs total lessons
   * - compute percentage
   * This is client-side demo logic; a real app would persist lesson completion.
   */
  calculateProgress(): number {
    const allLessonIds: number[] = [];
    for (const m of this.modules) {
      for (const l of m.lessons ?? []) {
        if (typeof l.id === 'number') allLessonIds.push(l.id);
      }
    }
    const total = allLessonIds.length;
    if (total === 0) return 0;

    const completed = allLessonIds.filter((id) => this.completedLessonIds.has(id)).length;
    return Math.round((completed / total) * 100);
  }

  updateLessonProgress(lessonId: number, completed: boolean): void {
    if (completed) this.completedLessonIds.add(lessonId);
    else this.completedLessonIds.delete(lessonId);

    this.currentProgress = this.calculateProgress();
    this.updateProgress();
  }

  isLessonCompleted(lessonId: number): boolean {
    return this.completedLessonIds.has(lessonId);
  }

  updateProgress(): void {
    if (!this.enrollment || this.busy) return;

    const pct = Math.max(0, Math.min(100, this.currentProgress));
    const before = this.enrollment.progressPercentage ?? 0;

    this.busy = true;
    this.error = null;
    this.message = null;

    this.enrollments.updateProgress(this.enrollment.id, pct).pipe(takeUntil(this.destroyed$)).subscribe({
      next: (e: EnrollmentResponse) => {
        this.busy = false;
        this.enrollment = e;
        this.message = 'Progress updated.';

        // Confetti when reaching 100%.
        if (before < 100 && (e.progressPercentage ?? 0) >= 100) {
          this.showConfetti = true;
          window.setTimeout(() => (this.showConfetti = false), 800);
        }
      },
      error: (err: any) => {
        this.busy = false;
        this.error = err?.message ?? err?.error?.message ?? 'Failed to update progress';
      }
    });
  }

  certificate(): void {
    this.busy = true;
    this.error = null;
    this.message = null;

    this.enrollments.issueCertificate(this.enrollmentId).pipe(takeUntil(this.destroyed$)).subscribe({
      next: (e: EnrollmentResponse) => {
        this.busy = false;
        this.enrollment = e;
        this.message = 'Certificate requested.';
      },
      error: (err: any) => {
        this.busy = false;
        this.error = err?.message ?? err?.error?.message ?? 'Failed to issue certificate';
      }
    });
  }

  get timeEstimateLabel(): string {
    const totalLessons = this.countTotalLessons();
    const completedLessons = this.countCompletedLessons();
    const remaining = Math.max(0, totalLessons - completedLessons);

    // Simple placeholder: 5 minutes per lesson.
    const minutes = remaining * 5;
    return `${minutes} min`;
  }

  private countTotalLessons(): number {
    let total = 0;
    for (const m of this.modules) total += (m.lessons ?? []).length;
    return total;
  }

  private countCompletedLessons(): number {
    let completed = 0;
    for (const m of this.modules) {
      for (const l of m.lessons ?? []) {
        if (typeof l.id === 'number' && this.completedLessonIds.has(l.id)) completed++;
      }
    }
    return completed;
  }

  ngOnDestroy(): void {
    this.destroyed$.next();
    this.destroyed$.complete();
  }
}
