import { Component, inject, signal, Input, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';

export interface BackofficeNavItem {
  id: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-backoffice-layout',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './backoffice-layout.component.html',
  styleUrls: ['./backoffice-layout.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class BackofficeLayoutComponent {
  private authService = inject(AuthService);
  readonly themeService = inject(ThemeService);

  @Input() title = 'Dashboard';
  @Input() navItems: BackofficeNavItem[] = [];
  @Input() activeTab = '';
  @Output() tabChange = new EventEmitter<string>();
  @Output() logout = new EventEmitter<void>();

  readonly sidebarOpen = signal(false);
  readonly currentUser = this.authService.currentUser;

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  onTabClick(tabId: string): void {
    this.tabChange.emit(tabId);
    this.closeSidebar();
  }

  onLogout(): void {
    this.logout.emit();
    this.closeSidebar();
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }
}
