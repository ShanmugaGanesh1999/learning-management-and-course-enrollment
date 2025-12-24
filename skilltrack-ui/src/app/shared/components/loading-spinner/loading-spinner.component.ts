import { Component, Input } from '@angular/core';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [NgIf],
  template: `
    <div *ngIf="show" class="card" style="text-align:center;">
      Loading...
    </div>
  `
})
export class LoadingSpinnerComponent {
  @Input() show = false;
}
