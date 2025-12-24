import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { ApiService } from '../../../core/services/api.service';
import { PageResponse } from '../../../core/models/page.model';
import { UserProfile } from '../../../core/models/user.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent],
  template: `
    <div class="card">
      <h2>Users</h2>

      <form [formGroup]="filters" class="row" style="align-items:end;">
        <label>
          Search
          <input formControlName="search" placeholder="username or email" />
        </label>
      </form>

      <div *ngIf="loading">Loading...</div>
      <div class="error" *ngIf="error">{{ error }}</div>

      <div *ngIf="page && page.content.length === 0">No users.</div>

      <div *ngIf="page" class="row" style="flex-direction:column;">
        <div *ngFor="let u of page.content" class="card">
          <div><strong>{{ u.username }}</strong> <small>{{ u.role }}</small></div>
          <div><small>{{ u.email }}</small></div>
          <div><small>{{ u.fullName }}</small></div>
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
export class UserManagementComponent {
  loading = false;
  error: string | null = null;

  page: PageResponse<UserProfile> | null = null;

  private pageIndex = 0;
  private pageSize = 20;

  filters = this.fb.group({
    search: ['']
  });

  constructor(private readonly fb: FormBuilder, private readonly api: ApiService) {
    this.filters.valueChanges.subscribe(() => {
      this.pageIndex = 0;
      this.load();
    });

    this.load();
  }

  setPage(page: number): void {
    this.pageIndex = page;
    this.load();
  }

  setSize(size: number): void {
    this.pageSize = size;
    this.pageIndex = 0;
    this.load();
  }

  private load(): void {
    const v = this.filters.getRawValue();

    this.loading = true;
    this.error = null;

    this.api
      .get<PageResponse<UserProfile>>('/auth/users', {
        params: {
          page: this.pageIndex,
          size: this.pageSize,
          sort: 'username,asc',
          search: v.search ? v.search : undefined
        }
      })
      .subscribe({
        next: (res) => {
          this.loading = false;
          this.page = res;
        },
        error: (err) => {
          this.loading = false;
          this.error = err?.message ?? err?.error?.message ?? 'Failed to load users';
        }
      });
  }
}
