import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, UserRole } from '../../../services/auth.service';
import { Router } from '@angular/router';
import { BackofficeLayoutComponent, BackofficeNavItem } from '../../../shared/backoffice-layout';

import { VendorDashboardComponent } from '../vendor-dashboard';
import { RecruiterDashboardComponent } from '../recruiter-dashboard';
import { DeliveryDashboardComponent } from '../deliverer-dashboard';
import { ServiceProviderDashboardComponent } from '../service-provider-dashboard';
import { EventsAdminDashboardComponent } from '../events-admin-dashboard/events-admin-dashboard.component';

interface DashboardTab {
  id: string;
  label: string;
  icon: string;
  component: any;
  roles: UserRole[];
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    BackofficeLayoutComponent,
    VendorDashboardComponent,
    RecruiterDashboardComponent,
    DeliveryDashboardComponent,
    ServiceProviderDashboardComponent,
    EventsAdminDashboardComponent
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly activeTab = signal('vendor');

  readonly dashboardTabs: DashboardTab[] = [
    { id: 'vendor', label: 'Vendor', icon: 'vendor', component: VendorDashboardComponent, roles: ['seller'] },
    { id: 'service-provider', label: 'Services', icon: 'service-provider', component: ServiceProviderDashboardComponent, roles: ['service_provider'] },
    { id: 'recruiter', label: 'Recruiter', icon: 'recruiter', component: RecruiterDashboardComponent, roles: ['recruiter'] },
    { id: 'deliverer', label: 'Delivery', icon: 'deliverer', component: DeliveryDashboardComponent, roles: ['deliverer'] },
    { id: 'events', label: 'Events', icon: 'events', component: EventsAdminDashboardComponent, roles: ['event'] }
  ];

  readonly availableTabs = signal<DashboardTab[]>([]);
  readonly navItems = computed<BackofficeNavItem[]>(() => 
    this.availableTabs().map(tab => ({ id: tab.id, label: tab.label, icon: tab.icon }))
  );

  ngOnInit(): void {
    this.loadAvailableTabs();
  }

  onTabChange(tabId: string): void {
    this.activeTab.set(tabId);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private loadAvailableTabs(): void {
    const user = this.authService.currentUser();
    const userRole = user?.role;
    
    if (!userRole) {
      this.router.navigate(['/login']);
      return;
    }

    const tabs = this.dashboardTabs.filter(tab => tab.roles.includes(userRole));
    this.availableTabs.set(tabs);

    if (tabs.length > 0 && !tabs.find(t => t.id === this.activeTab())) {
      this.activeTab.set(tabs[0].id);
    }
  }
}
