import { Component, inject, ViewChild, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BackofficeLayoutComponent, BackofficeNavItem } from '../../../shared/backoffice-layout';
import { ModalComponent } from '../../../shared/components/modal.component';
import { ServiceFormComponent } from '../service-form';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { CommonApiService, SrvApiService } from '../../../services';
import { User, ServiceUpsertRequest as ServiceWritePayload } from '@esprit-market/api-types';
import { formatStatusLabel } from '../../../shared/utils';

@Component({
  selector: 'app-services-admin-dashboard',
  standalone: true,
  imports: [CommonModule, BackofficeLayoutComponent, ModalComponent, ServiceFormComponent],
  templateUrl: './services-admin-dashboard.component.html',
  styleUrls: ['./services-admin-dashboard.component.css']
})
export class ServicesAdminDashboardComponent {
  @ViewChild(ServiceFormComponent) serviceFormComponent?: ServiceFormComponent;

  private authService = inject(AuthService);
  private router = inject(Router);
  private srvApi = inject(SrvApiService);
  private commonApi = inject(CommonApiService);

  activeTab = signal('overview');
  readonly navItems: BackofficeNavItem[] = [
    { id: 'overview', label: 'Overview', icon: 'overview' },
    { id: 'services', label: 'Services', icon: 'services' },
    { id: 'providers', label: 'Providers', icon: 'vendor' }
  ];

  isModalOpen = signal(false);
  isEditing = signal(false);
  isSubmitting = signal(false);
  editingServiceId = signal<number | null>(null);

  services: Array<{ id: number; name: string; category: string; provider: string; price: number; status: string }> = [];

  providers: Array<{ name: string; initials: string; speciality: string; rating: number; serviceCount: number }> = [];

  constructor() {
    this.refreshData();
  }

  openCreateServiceModal(): void {
    this.isEditing.set(false);
    this.editingServiceId.set(null);
    this.isModalOpen.set(true);
    setTimeout(() => {
      this.serviceFormComponent?.resetForm();
    }, 100);
  }

  openEditServiceModal(service: any): void {
    this.isEditing.set(true);
    this.editingServiceId.set(service.id);
    this.isModalOpen.set(true);
    setTimeout(() => {
      this.serviceFormComponent?.setFormData({
        name: service.name,
        category: service.category,
        description: service.description || '',
        price: service.price,
        status: service.status === 'active' ? 'AVAILABLE' : 'UNAVAILABLE'
      });
    }, 100);
  }

  closeServiceModal(): void {
    this.isModalOpen.set(false);
    this.isEditing.set(false);
    this.editingServiceId.set(null);
  }

  submitServiceForm(): void {
    if (!this.serviceFormComponent?.isFormValid()) {
      return;
    }

    this.isSubmitting.set(true);
    const formData = this.serviceFormComponent!.getFormData();
    const serviceId = this.editingServiceId();

    const payload: ServiceWritePayload = {
      name: formData.name,
      description: formData.description,
      category: formData.category,
      pricingType: formData.pricingType,
      price: Number(formData.price),
      status: formData.status,
      rating: 0,
      location: 'N/A',
      imageUrl: formData.imageUrl || undefined,
      providerId: 1
    };

    if (serviceId) {
      this.srvApi.updateService(serviceId, payload).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeServiceModal();
          this.refreshData();
        },
        error: () => {
          this.isSubmitting.set(false);
        }
      });
    } else {
      this.srvApi.createService(payload).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeServiceModal();
          this.refreshData();
        },
        error: () => {
          this.isSubmitting.set(false);
        }
      });
    }
  }

  confirmDeleteService(serviceId: number, serviceName: string): void {
    const confirmed = window.confirm(`Are you sure you want to delete "${serviceName}"?`);
    if (confirmed) {
      this.deleteService(serviceId);
    }
  }

  private refreshData(): void {
    this.srvApi.getServices(0, 100).subscribe({
      next: (page) => {
        this.services = page.content.map((service) => ({
          id: service.id!,
          name: service.name ?? '',
          category: service.category ?? '',
          provider: service.providerName ?? 'N/A',
          price: service.price ?? 0,
          status: String(service.status).toLowerCase() === 'available' ? 'active' : 'pending'
        }));
      },
      error: () => {
        this.services = [];
      }
    });

    this.commonApi.users.list().subscribe({
      next: (users) => {
        this.providers = users
          .filter((user) => user.role === User.RoleEnum.ServiceProvider)
          .map((provider) => ({
            name: provider.name ?? '',
            initials: (provider.name ?? '')
              .split(' ')
              .map((part) => part[0])
              .join('')
              .slice(0, 2)
              .toUpperCase(),
            speciality: 'Service provider',
            rating: 0,
            serviceCount: this.services.filter((service) => service.provider === (provider.name ?? '')).length
          }));
      },
      error: () => {
        this.providers = [];
      }
    });
  }

  private deleteService(serviceId: number): void {
    this.srvApi.deleteService(serviceId).subscribe({
      next: () => {
        this.services = this.services.filter((service) => service.id !== serviceId);
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  onTabChange(tabId: string): void {
    this.activeTab.set(tabId);
  }
}
