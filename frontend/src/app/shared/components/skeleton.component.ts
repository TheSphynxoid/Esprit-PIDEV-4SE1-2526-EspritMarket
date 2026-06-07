import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="animate-pulse rounded-md bg-neutral-200 dark:bg-neutral-700" [ngClass]="variantClass()"></div>
  `,
  styles: []
})
export class SkeletonComponent {
  readonly variant = input<'text' | 'heading' | 'avatar' | 'card' | 'table-row'>('text');
  readonly lines = input<number>(1);

  variantClass(): string {
    switch (this.variant()) {
      case 'heading': return 'h-8 w-64';
      case 'avatar': return 'h-10 w-10 rounded-full';
      case 'card': return 'h-40 w-full';
      case 'table-row': return 'h-12 w-full';
      case 'text': return 'h-4 w-48';
    }
  }
}
