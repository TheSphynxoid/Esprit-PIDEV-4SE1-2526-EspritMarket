import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FooterComponent } from '../../../shared/layout';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { MarketplaceApiService, SrvApiService } from '../../../services';

@Component({
  selector: 'app-companies-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FooterComponent],
  templateUrl: './companies-admin-dashboard.component.html',
  styleUrls: ['./companies-admin-dashboard.component.css']
})
export class CompaniesAdminDashboardComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private marketplaceApi = inject(MarketplaceApiService);
  private srvApi = inject(SrvApiService);

  activeTab = 'overview';

  companies: Array<{ name: string; initials: string; sector: string; contact: string; offers: number; status: string }> = [];

  partnerships: Array<{ company: string; initials: string; type: string; since: string; opportunities: number }> = [];

  applications: Array<{ company: string; description: string; sector: string; email: string; date: string }> = [];

  constructor() {
    this.refreshData();
  }

  private refreshData(): void {
    this.marketplaceApi.stores.list().subscribe({
      next: (stores) => {
        this.companies = stores.map((store) => ({
          name: store.name,
          initials: store.name
            .split(' ')
            .map((part) => part[0])
            .join('')
            .slice(0, 2)
            .toUpperCase(),
          sector: 'Retail',
          contact: store.phone,
          offers: 0,
          status: 'active'
        }));
      },
      error: () => {
        this.companies = [];
      }
    });

    this.srvApi.getPartners(0, 100).subscribe({
      next: (page) => {
        this.partnerships = page.content.map((partner) => ({
          company: partner.name ?? '',
          initials: (partner.name ?? '')
            .split(' ')
            .map((part) => part[0])
            .join('')
            .slice(0, 2)
            .toUpperCase(),
          type: 'Standard Partnership',
          since: new Date().getFullYear().toString(),
          opportunities: 0
        }));
      },
      error: () => {
        this.partnerships = [];
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
