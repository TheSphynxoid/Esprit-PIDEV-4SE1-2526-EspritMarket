import { App } from './app';
import { NotificationService } from './services/notification.service';
import { of } from 'rxjs';

describe('App', () => {
  it('should create the app', () => {
    const notificationService = {
      notifications: of([]),
      unreadCount: of(0),
      dismiss: () => {},
      loadNotifications: () => {}
    } as unknown as NotificationService;
    const app = new App(notificationService);
    expect(app).toBeTruthy();
  });
});
