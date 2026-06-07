import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NotificationService } from './services/notification.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  constructor(public readonly notificationService: NotificationService) {}

  trackByNotificationId(_: number, notification: { id: number }): number {
    return notification.id;
  }

  dismissNotification(id: number): void {
    this.notificationService.dismiss(id);
  }
}
