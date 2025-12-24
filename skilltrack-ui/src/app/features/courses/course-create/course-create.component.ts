import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { CourseService } from '../../../core/services/course.service';
import { CanComponentDeactivate } from '../../../core/guards/pending-changes.guard';

@Component({
  selector: 'app-course-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="card">
      <h2>Create Course</h2>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="row" style="flex-direction:column;max-width:640px;">
        <label>
          Title
          <input formControlName="title" />
        </label>

        <div class="error" *ngIf="form.controls.title.touched && form.controls.title.invalid">
          <div *ngIf="form.controls.title.errors?.['required']">Title is required</div>
          <div *ngIf="form.controls.title.errors?.['maxlength']">Max 120 characters</div>
        </div>

        <label>
          Description
          <!-- Rich text editor placeholder: use a textarea for now -->
          <textarea rows="6" formControlName="description"></textarea>
        </label>

        <label>
          Category
          <select formControlName="category">
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
            <option value="BEGINNER">BEGINNER</option>
            <option value="INTERMEDIATE">INTERMEDIATE</option>
            <option value="ADVANCED">ADVANCED</option>
          </select>
        </label>

        <label>
          Price
          <input type="number" step="0.01" formControlName="price" />
        </label>

        <div class="error" *ngIf="form.controls.price.touched && form.controls.price.invalid">
          <div *ngIf="form.controls.price.errors?.['min']">Price cannot be negative</div>
        </div>

        <div class="error" *ngIf="error">{{ error }}</div>

        <div class="row" style="gap:10px;">
          <button type="submit" [disabled]="loading || form.invalid">{{ loading ? 'Creating…' : 'Submit' }}</button>
          <button type="button" (click)="cancel()" [disabled]="loading">Cancel</button>
        </div>
      </form>
    </div>
  `
})
export class CourseCreateComponent implements CanComponentDeactivate {
  loading = false;
  error: string | null = null;
  private saved = false;

  form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    category: ['PROGRAMMING', [Validators.required]],
    level: ['BEGINNER', [Validators.required]],
    price: [0, [Validators.min(0)]]
  });

  constructor(private readonly fb: FormBuilder, private readonly courses: CourseService, private readonly router: Router) {
    this.form.valueChanges.subscribe(() => {
      // If the user edits after saving, consider the form dirty again.
      if (this.form.dirty) this.saved = false;
    });
  }

  /**
   * Form submission flow:
   * - validate form
   * - call API
   * - navigate back to course list on success
   */
  onSubmit(): void {
    if (this.form.invalid || this.loading) return;

    this.loading = true;
    this.error = null;

    this.courses.createCourse(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading = false;
        this.saved = true;
        void this.router.navigateByUrl('/courses');
      },
        error: (err: any) => {
        this.loading = false;
        this.error = err?.message ?? err?.error?.message ?? 'Failed to create course';
      }
    });
  }

  cancel(): void {
    void this.router.navigateByUrl('/courses');
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty && !this.saved;
  }
}
