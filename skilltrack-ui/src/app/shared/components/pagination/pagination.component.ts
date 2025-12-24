import { Component, EventEmitter, Input, Output } from '@angular/core';
import { NgIf } from '@angular/common';

import { PageResponse } from '../../../core/models/page.model';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [NgIf],
  template: `
    <div *ngIf="page" class="row" style="align-items:center;justify-content:space-between;">
      <div>
        <button (click)="prev()" [disabled]="page.first">Prev</button>
        <button (click)="next()" [disabled]="page.last">Next</button>
        <span style="margin-left:8px;">Page {{ page.pageNumber + 1 }} / {{ page.totalPages }}</span>
      </div>

      <div>
        <label>
          Size
          <select [value]="page.pageSize" (change)="sizeChange.emit(parseSize($any($event.target).value))">
            <option [value]="10">10</option>
            <option [value]="25">25</option>
            <option [value]="50">50</option>
          </select>
        </label>
      </div>
    </div>
  `
})
export class PaginationComponent {
  @Input() page: PageResponse<any> | null = null;
  @Output() pageChange = new EventEmitter<number>();
  @Output() sizeChange = new EventEmitter<number>();

  prev(): void {
    if (!this.page || this.page.first) return;
    this.pageChange.emit(this.page.pageNumber - 1);
  }

  next(): void {
    if (!this.page || this.page.last) return;
    this.pageChange.emit(this.page.pageNumber + 1);
  }

  parseSize(raw: any): number {
    const n = Number.parseInt(String(raw), 10);
    return Number.isFinite(n) ? n : 10;
  }
}
