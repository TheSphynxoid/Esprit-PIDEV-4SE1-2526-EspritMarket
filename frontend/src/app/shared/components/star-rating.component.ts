import { Component, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-center gap-1">
      @for (star of stars; track star) {
        <button
          type="button"
          class="star-btn"
          [class.filled]="star <= value()"
          [class.hovered]="star <= hoverValue()"
          (mouseenter)="hoverValue.set(star)"
          (mouseleave)="hoverValue.set(0)"
          (click)="select(star)"
          [disabled]="disabled()"
        >
          <svg class="star-icon" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
          </svg>
        </button>
      }
      <span class="text-sm text-secondary ml-1" *ngIf="showLabel()">{{ value() }}/5</span>
    </div>
  `,
  styles: [`
    .star-btn {
      background: none;
      border: none;
      cursor: pointer;
      padding: 2px;
      line-height: 1;
      color: #d1d5db;
      transition: color 0.15s, transform 0.15s;
    }
    .star-btn:hover:not(:disabled) { transform: scale(1.15); }
    .star-btn:disabled { cursor: default; }
    .star-btn.filled { color: #f59e0b; }
    .star-btn.hovered { color: #fbbf24; }
    .star-icon { width: 24px; height: 24px; }
    :host-context(.dark) .star-btn:not(.filled):not(.hovered) { color: #525252; }
  `]
})
export class StarRatingComponent {
  readonly value = input<number>(0);
  readonly disabled = input<boolean>(false);
  readonly showLabel = input<boolean>(false);
  readonly valueChange = output<number>();

  readonly stars = [1, 2, 3, 4, 5];
  readonly hoverValue = signal(0);

  select(star: number): void {
    if (!this.disabled()) {
      this.valueChange.emit(star);
    }
  }
}
