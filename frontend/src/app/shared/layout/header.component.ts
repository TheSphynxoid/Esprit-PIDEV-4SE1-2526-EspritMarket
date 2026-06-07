import { Component, Input, inject, signal, computed, HostListener, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { ThemeService } from '../../services/theme.service';

const CREATED_STORE_PROFILE_KEY = 'create_store_latest_shop_profile';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
    styleUrls: ['./header.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class HeaderComponent {

  @Input() isAuthPage = false;
  @Input() hideCartAndDashboard = false;

  authService = inject(AuthService);
  cartService = inject(CartService);
  themeService = inject(ThemeService);
  private router = inject(Router);

  mobileMenuOpen = signal(false);
  moreMenuOpen = signal(false);

  hasDashboardAccess = computed(() => {
    const role = this.authService.currentUser()?.role;
    return role === 'seller' ||
         role === 'service_provider' ||
         role === 'deliverer' ||
         role === 'recruiter' ||
         role === 'event' ||
         role === 'admin_market';
  });

  toggleMobileMenu() {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu() {
    this.mobileMenuOpen.set(false);
  }

  toggleMoreMenu() {
    this.moreMenuOpen.update(v => !v);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest('[data-more-menu]')) {
      this.moreMenuOpen.set(false);
    }
  }

  getBrandRoute(): string {
    return '/marketplace';
  }

  onBrandClick(event: Event): void {
    event.preventDefault();
    this.closeMobileMenu();

    if (this.router.url.startsWith('/marketplace')) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    this.router.navigate(['/marketplace']);
  }

  getUserInitials(): string {
    const name = this.authService.currentUser()?.name || '';
    return name.split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2) || '??';
  }

  getRoleLabel(): string {
    const role = this.authService.currentUser()?.role;

    const roleLabels: Record<string, string> = {
      visitor: 'Visitor',
      seller: 'Seller',
      service_provider: 'Service Provider',
      deliverer: 'Deliverer',
      partner: 'Partner',
      recruiter: 'Recruiter',
      event: 'Event Manager',
      admin_market: 'Admin Market'
    };

    return roleLabels[role || ''] || 'User';
  }

  getDashboardRoute(): string {
    const role = this.authService.currentUser()?.role;

    // Store profile shortcut should only apply to sellers.
    if (role === 'seller' && this.hasCurrentUserStoreProfile()) {
      return '/dashboard/vendor';
    }

    switch (role) {
      case 'seller':
        return '/dashboard/vendor';
      case 'service_provider':
        return '/dashboard/service-provider';
      case 'deliverer':
        return '/dashboard/deliverer';
      case 'recruiter':
        return '/dashboard/recruiter';
      case 'event':
        return '/dashboard/events';
      case 'admin_market':
        return '/admin-market';
      case 'partner':
      case 'visitor': return '/marketplace';
      default: return '/home';
    }
  }

  getSellRoute(): string {
    if (!this.authService.isLoggedIn()) {
      return '/login';
    }

    const role = this.authService.currentUser()?.role;
    if (role === 'seller') {
      return '/create-store-step-2';
    }

    return '/create-store';
  }

  getSellQueryParams(): Record<string, string> | null {
    if (!this.authService.isLoggedIn()) {
      return { fromSell: 'true' };
    }

    return null;
  }

  onSellClick(event: Event): void {
    event.preventDefault();
    this.closeMobileMenu();

    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login'], { queryParams: { fromSell: 'true' } });
      return;
    }

    const currentUser = this.authService.currentUser();
    if (!currentUser) {
      this.router.navigate(['/login'], { queryParams: { fromSell: 'true' } });
      return;
    }

    if (currentUser.role === 'seller') {
      this.router.navigate(['/create-store-step-2']);
      return;
    }
    this.router.navigate(['/create-store']);
  }

  private hasCurrentUserStoreProfile(): boolean {
    if (!this.authService.isLoggedIn()) {
      return false;
    }

    try {
      const raw = localStorage.getItem(CREATED_STORE_PROFILE_KEY);
      if (!raw) {
        return false;
      }

      const profile = JSON.parse(raw) as { ownerEmail?: string; name?: string };
      if (!profile || typeof profile !== 'object') {
        return false;
      }

      const currentEmail = String(this.authService.currentUser()?.email || '').toLowerCase().trim();
      const ownerEmail = String(profile.ownerEmail || '').toLowerCase().trim();
      if (currentEmail && ownerEmail && currentEmail === ownerEmail) {
        return true;
      }

      // Backward compatibility for older profiles that may not have ownerEmail
      return !!String(profile.name || '').trim();
    } catch {
      return false;
    }
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
