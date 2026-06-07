import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
  id: string;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
  duration?: number;
  icon?: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private toasts$ = new BehaviorSubject<Toast[]>([]);

  getToasts() {
    return this.toasts$.asObservable();
  }

  show(toast: Omit<Toast, 'id'>) {
    const id = Date.now().toString();
    const newToast: Toast = {
      id,
      ...toast,
      duration: toast.duration || 5000
    };

    const current = this.toasts$.value;
    this.toasts$.next([...current, newToast]);

    if (newToast.duration && newToast.duration > 0) {
      setTimeout(() => this.remove(id), newToast.duration);
    }

    return id;
  }

  remove(id: string) {
    const current = this.toasts$.value;
    this.toasts$.next(current.filter(t => t.id !== id));
  }

  success(message: string, duration?: number) {
    return this.show({ message, type: 'success', duration, icon: '✅' });
  }

  error(message: string, duration?: number) {
    return this.show({ message, type: 'error', duration, icon: '❌' });
  }

  warning(message: string, duration?: number) {
    return this.show({ message, type: 'warning', duration, icon: '⚠️' });
  }

  info(message: string, duration?: number) {
    return this.show({ message, type: 'info', duration, icon: 'ℹ️' });
  }
}
