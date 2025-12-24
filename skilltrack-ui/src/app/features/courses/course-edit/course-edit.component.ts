import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';

import { CourseService } from '../../../core/services/course.service';
import { CourseDetail } from '../../../core/models/course.model';
import { CanComponentDeactivate } from '../../../core/guards/pending-changes.guard';

@Component({
  selector: 'app-course-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="card">
      <h2>Edit Course</h2>

      <div *ngIf="loading">Loading...</div>
      <div class="error" *ngIf="error">{{ error }}</div>

      <form *ngIf="course" [formGroup]="form" (ngSubmit)="save()" class="row" style="flex-direction:column;max-width:640px;">
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
          <!-- Edit vs create: we pre-populate the form with existing course data -->
          <textarea rows="5" formControlName="description"></textarea>
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

        <div class="row">
          <button type="submit" [disabled]="saving || form.invalid">{{ saving ? 'Saving…' : 'Save' }}</button>
          <button type="button" (click)="publish()" [disabled]="publishing">{{ publishing ? 'Publishing…' : 'Publish' }}</button>
          <button type="button" (click)="cancel()" [disabled]="saving || publishing">Cancel</button>
        </div>
      </form>

      <div *ngIf="course" style="margin-top:12px;">
        <small>Status: {{ course.status }}</small>
      </div>

      <div *ngIf="course" style="margin-top:16px;">
        <h3>Module management</h3>
        <div class="row" style="align-items:end;gap:10px;flex-wrap:wrap;">
          <label style="flex:1;min-width:240px;">
            New module title
            <input [value]="newModuleTitle" (input)="newModuleTitle = $any($event.target).value" />
          </label>
          <button type="button" (click)="addModule()" [disabled]="addingModule || !newModuleTitle.trim()">
            {{ addingModule ? 'Adding…' : 'Add module' }}
          </button>
        </div>

        <div *ngIf="course.modules?.length === 0" style="opacity:0.85;">No modules yet.</div>
        <div *ngFor="let m of course.modules" class="card">
          <div style="display:flex;justify-content:space-between;gap:10px;align-items:center;flex-wrap:wrap;">
            <div>
              <strong>{{ m.title }}</strong>
              <div style="opacity:0.85;">Lessons: {{ m.lessons?.length ?? 0 }}</div>
            </div>
            <button type="button" (click)="deleteModule(m.id)" [disabled]="deletingModuleIds.has(m.id)">
              {{ deletingModuleIds.has(m.id) ? 'Deleting…' : 'Delete' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class CourseEditComponent implements CanComponentDeactivate {
  loading = false;
  saving = false;
  publishing = false;
  error: string | null = null;

  addingModule = false;
  newModuleTitle = '';
  deletingModuleIds = new Set<number>();

  private saved = false;

  course: CourseDetail | null = null;

  form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    category: ['PROGRAMMING', [Validators.required]],
    level: ['BEGINNER', [Validators.required]],
    price: [0, [Validators.min(0)]]
  });

  private readonly id: number;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly courses: CourseService,
    private readonly router: Router
  ) {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = null;

    this.courses.getCourse(this.id).subscribe({
      next: (res) => {
        this.loading = false;
        const r: any = res as any;
        const c = r?.course ? { ...r.course, modules: r.modules ?? [] } : r;
        this.course = c;
        this.form.patchValue({
          title: c.title,
          description: c.description,
          category: c.category,
          level: c.level,
          price: c.price ?? 0
        });
        // After loading data from the server, treat the form as pristine.
        this.form.markAsPristine();
        this.saved = true;
      },
      error: (err: any) => {
        this.loading = false;
        this.error = err?.message ?? err?.error?.message ?? 'Failed to load course';
      }
    });
  }

  save(): void {
    if (this.form.invalid || this.saving) return;

    this.saving = true;
    this.error = null;

    this.courses.updateCourse(this.id, this.form.getRawValue()).subscribe({
      next: () => {
        this.saving = false;
        this.saved = true;
        this.load();
      },
      error: (err: any) => {
        this.saving = false;
        this.error = err?.message ?? err?.error?.message ?? 'Failed to save course';
      }
    });
  }

  publish(): void {
    if (this.publishing) return;

    this.publishing = true;
    this.error = null;

    this.courses.publishCourse(this.id).subscribe({
      next: () => {
        this.publishing = false;
        this.saved = true;
        this.load();
      },
      error: (err: any) => {
        this.publishing = false;
        this.error = err?.message ?? err?.error?.message ?? 'Failed to publish course';
      }
    });
  }

  addModule(): void {
    const title = this.newModuleTitle.trim();
    if (!title || this.addingModule) return;

    this.addingModule = true;
    this.error = null;

    this.courses.createModule(this.id, title).subscribe({
      next: () => {
        this.addingModule = false;
        this.newModuleTitle = '';
        this.load();
      },
      error: (err: any) => {
        this.addingModule = false;
        this.error = err?.message ?? err?.error?.message ?? 'Failed to add module';
      }
    });
  }

  deleteModule(moduleId: number): void {
    if (this.deletingModuleIds.has(moduleId)) return;

    this.deletingModuleIds.add(moduleId);
    this.error = null;

    this.courses.deleteModule(moduleId).subscribe({
      next: () => {
        this.deletingModuleIds.delete(moduleId);
        this.load();
      },
      error: (err: any) => {
        this.deletingModuleIds.delete(moduleId);
        this.error = err?.message ?? err?.error?.message ?? 'Failed to delete module';
      }
    });
  }

  cancel(): void {
    void this.router.navigateByUrl('/courses');
  }

  hasUnsavedChanges(): boolean {
    // Edit vs create: the form is pre-populated; we mark pristine after loading.
    // If user changes anything and hasn't saved, warn on navigation.
    return this.form.dirty && !this.saved;
  }
}
