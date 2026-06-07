import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Toast, ToastService } from '../services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed top-4 right-4 z-50 space-y-3 pointer-events-none">
      <div 
        *ngFor="let toast of toasts"
        [ngClass]="getToastClasses(toast.type)"
        class="pointer-events-auto animate-slide-in"
      >
        <div class="flex items-start gap-3 p-4 rounded-lg shadow-lg">
          <span class="text-lg">{{ toast.icon }}</span>
          <div class="flex-1">
            <p class="text-sm font-medium">{{ toast.message }}</p>
          </div>
          <button 
            (click)="removeToast(toast.id)"
            class="text-lg hover:opacity-70 transition-opacity"
          >
            ✕
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host ::ng-deep {
      @keyframes slideIn {
        from {
          transform: translateX(400px);
          opacity: 0;
        }
        to {
          transform: translateX(0);
          opacity: 1;
        }
      }

      .animate-slide-in {
        animation: slideIn 0.3s ease-out;
      }
    }
  `]
})
export class ToastContainerComponent implements OnInit {
  private toastService = inject(ToastService);
  toasts: Toast[] = [];

  ngOnInit() {
    this.toastService.getToasts().subscribe(toasts => {
      this.toasts = toasts;
    });
  }

  removeToast(id: string) {
    this.toastService.remove(id);
  }

  getToastClasses(type: string): string {
    const baseClasses = 'bg-white dark:bg-neutral-800 border-l-4';
    
    switch (type) {
      case 'success':
        return `${baseClasses} border-green-500 text-green-800 dark:text-green-200`;
      case 'error':
        return `${baseClasses} border-red-500 text-red-800 dark:text-red-200`;
      case 'warning':
        return `${baseClasses} border-amber-500 text-amber-800 dark:text-amber-200`;
      case 'info':
        return `${baseClasses} border-blue-500 text-blue-800 dark:text-blue-200`;
      default:
        return baseClasses;
    }
  }
}
