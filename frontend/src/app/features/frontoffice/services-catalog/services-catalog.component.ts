import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { ServiceResponse } from '@esprit-market/api-types';
import { FooterComponent, HeaderComponent } from '../../../shared/layout';
import { SkeletonComponent } from '../../../shared/components';
import { ServiceComparisonComponent } from '../../../shared/components/service-comparison.component';
import { SrvApiService } from '../../../services';

@Component({
  selector: 'app-services-catalog',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent, SkeletonComponent, ServiceComparisonComponent],
  templateUrl: './services-catalog.component.html',
  styleUrls: []
})
export class ServicesCatalogComponent implements OnInit {
  private readonly srvApi = inject(SrvApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly viewMode = signal<'grid' | 'list'>('grid');
  readonly searchTerm = signal('');
  readonly selectedCategory = signal<ServiceResponse.CategoryEnum | ''>('');
  readonly minPrice = signal<number | null>(null);
  readonly maxPrice = signal<number | null>(null);
  readonly loading = signal(false);

  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = signal(12);

  readonly hasNextPage = computed(() => this.currentPage() < this.totalPages() - 1);
  readonly hasPrevPage = computed(() => this.currentPage() > 0);
  readonly pageNumber = computed(() => this.currentPage() + 1);

  readonly services = signal<Array<{
    id: number;
    name: string;
    category: string;
    description: string;
    rating: number;
    price: number;
    startingPrice: number;
    pricingType: string;
    providerName: string;
    imageUrl: string;
  }>>([]);

  comparison = new ServiceComparisonComponent();

  readonly categories = Object.values(ServiceResponse.CategoryEnum);

  ngOnInit(): void {
    this.loadInitial();
  }

  goToBookings(): void {
    this.router.navigate(['/bookings']);
  }

  goToProjects(): void {
    this.router.navigate(['/projects']);
  }

  onSearch(): void {
    const term = this.searchTerm().trim();
    if (!term) {
      this.loadInitial();
      return;
    }

    this.loading.set(true);
    this.srvApi.searchServices(term, this.currentPage(), this.pageSize()).subscribe({
      next: (page) => {
        this.services.set(page.content.map((s) => this.mapService(s)));
        this.updatePaginationMeta(page);
        this.loading.set(false);
      },
      error: () => {
        this.services.set([]);
        this.loading.set(false);
      }
    });
  }

  onCategoryFilter(): void {
    const category = this.selectedCategory();
    if (!category) {
      this.loadInitial();
      return;
    }

    this.loading.set(true);
    this.srvApi.getServicesByCategory(category as ServiceResponse.CategoryEnum, this.currentPage(), this.pageSize()).subscribe({
      next: (page) => {
        this.services.set(page.content.map((s) => this.mapService(s)));
        this.updatePaginationMeta(page);
        this.loading.set(false);
      },
      error: () => {
        this.services.set([]);
        this.loading.set(false);
      }
    });
  }

  onPriceFilter(): void {
    this.currentPage.set(0);
    this.loadFilteredServices();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedCategory.set('');
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.currentPage.set(0);
    this.loadInitial();
  }

  nextPage(): void {
    if (!this.hasNextPage()) return;
    this.currentPage.update((p) => p + 1);
    this.applyCurrentFilter();
  }

  prevPage(): void {
    if (!this.hasPrevPage()) return;
    this.currentPage.update((p) => p - 1);
    this.applyCurrentFilter();
  }

  goToServiceDetail(serviceId: number): void {
    this.router.navigate(['/services', serviceId]);
  }

  private loadInitial(): void {
    const providerId = this.route.snapshot.queryParamMap.get('provider');
    if (providerId) {
      this.loading.set(true);
      this.srvApi.getServicesByProvider(+providerId, this.currentPage(), this.pageSize()).subscribe({
        next: (page) => {
          this.services.set(page.content.map((s) => this.mapService(s)));
          this.updatePaginationMeta(page);
          this.loading.set(false);
        },
        error: () => {
          this.services.set([]);
          this.loading.set(false);
        }
      });
      return;
    }

    this.loading.set(true);
    this.srvApi.getServices(this.currentPage(), this.pageSize()).subscribe({
      next: (page) => {
        this.services.set(page.content.map((s) => this.mapService(s)));
        this.updatePaginationMeta(page);
        this.loading.set(false);
      },
      error: () => {
        this.services.set([]);
        this.loading.set(false);
      }
    });
  }

  private applyCurrentFilter(): void {
    const term = this.searchTerm().trim();
    const category = this.selectedCategory();
    const minP = this.minPrice();
    const maxP = this.maxPrice();

    if (minP !== null || maxP !== null) {
      this.loadFilteredServices();
    } else if (term) {
      this.onSearch();
    } else if (category) {
      this.onCategoryFilter();
    } else {
      this.loadInitial();
    }
  }

  private loadFilteredServices(): void {
    this.loading.set(true);
    this.srvApi.filterServices({
      category: this.selectedCategory() as ServiceResponse.CategoryEnum || undefined,
      minPrice: this.minPrice() ?? undefined,
      maxPrice: this.maxPrice() ?? undefined,
      page: this.currentPage(),
      size: this.pageSize()
    }).subscribe({
      next: (page) => {
        this.services.set(page.content.map((s) => this.mapService(s)));
        this.updatePaginationMeta(page);
        this.loading.set(false);
      },
      error: () => {
        this.services.set([]);
        this.loading.set(false);
      }
    });
  }

  private updatePaginationMeta(page: { totalPages: number; totalElements: number }): void {
    this.totalPages.set(page.totalPages);
    this.totalElements.set(page.totalElements);
  }

  private mapService(s: any): any {
    return {
      id: s.id,
      name: s.name,
      category: s.category,
      description: s.description,
      rating: s.rating ?? 0,
      price: s.price,
      startingPrice: s.startingPrice ?? s.price,
      pricingType: s.pricingType ?? 'HOURLY',
      providerName: s.providerName ?? 'Provider',
      imageUrl: s.imageUrl ?? '',
      packageCount: (s.packages?.length as number) ?? 0
    } as any;
  }
}
