import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SrvApiService } from '../../services';

interface ComparisonItem {
  serviceId: number;
  serviceName: string;
  category: string;
  pricingType: string;
  price: number;
  startingPrice: number;
  description: string;
  providerId: number;
  providerName: string;
  averageRating: number | null;
  totalReviews: number;
  totalBookings: number;
  completedBookings: number;
  completionRate: number;
  level: string;
  levelNumber: number;
  mlReliabilityScore: number;
  mlRiskLevel: string;
  packageCount: number;
  tags: string[];
  compatibilityScore: number | null;
  compatibilityMatchLevel: string | null;
}

@Component({
  selector: 'app-service-comparison',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (compareCount() > 0) {
      <div class="fixed bottom-0 left-0 right-0 z-50 bg-white dark:bg-neutral-800 border-t border-neutral-200 dark:border-neutral-700 shadow-2xl transition-transform duration-300">
        <div class="container-wide max-w-6xl mx-auto px-4 py-3">
          <div class="flex items-center justify-between gap-4">
            <div class="flex items-center gap-3">
              <div class="flex -space-x-2">
                @for (id of compareList(); track id) {
                  <button (click)="removeFromCompare(id)" class="w-8 h-8 rounded-full bg-esprit-red text-white text-xs font-bold flex items-center justify-center ring-2 ring-white dark:ring-neutral-800 hover:bg-red-700 transition-colors" title="Remove">
                    {{ selectedNames().get(id)?.substring(0, 2) ?? '?' }}
                  </button>
                }
              </div>
              <span class="text-sm font-semibold text-primary dark:text-white">{{ compareCount() }}/5 services selected</span>
            </div>
            <div class="flex gap-2">
              <button (click)="clearComparison()" class="btn btn-secondary text-sm px-3 py-1.5">Clear</button>
              <button (click)="showModal.set(true); loadComparison()" [disabled]="compareCount() < 2" class="btn btn-primary text-sm px-4 py-1.5" [class.opacity-50]="compareCount() < 2" [class.cursor-not-allowed]="compareCount() < 2">
                Compare ({{ compareCount() }})
              </button>
            </div>
          </div>
        </div>
      </div>
    }

    @if (showModal()) {
      <div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50" (click)="showModal.set(false)">
        <div class="bg-white dark:bg-neutral-800 rounded-2xl shadow-2xl max-w-5xl w-full max-h-[85vh] overflow-hidden" (click)="$event.stopPropagation()">
          <div class="flex items-center justify-between px-6 py-4 border-b border-neutral-200 dark:border-neutral-700">
            <h2 class="text-xl font-bold text-primary dark:text-white">Service Comparison</h2>
            <button (click)="showModal.set(false)" class="w-8 h-8 rounded-full hover:bg-neutral-100 dark:hover:bg-neutral-700 flex items-center justify-center transition-colors">
              <svg class="w-5 h-5 text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
          </div>

          @if (loading()) {
            <div class="px-6 py-12 text-center">
              <div class="inline-block w-8 h-8 border-3 border-esprit-red border-t-transparent rounded-full animate-spin"></div>
              <p class="mt-3 text-sm text-secondary">Loading comparison data...</p>
            </div>
          } @else if (comparisonData().length > 0) {
            <div class="overflow-x-auto">
              <table class="w-full min-w-[600px]">
                <thead>
                  <tr class="border-b border-neutral-200 dark:border-neutral-700">
                    <th class="text-left px-6 py-3 text-xs font-semibold text-secondary uppercase tracking-wider w-40">Criteria</th>
                    @for (item of comparisonData(); track item.serviceId) {
                      <th class="text-left px-4 py-3 w-1/{{ comparisonData().length + 1 }}">
                        <button (click)="goToService(item.serviceId)" class="text-sm font-bold text-esprit-red hover:underline">{{ item.serviceName }}</button>
                        <p class="text-xs text-secondary mt-0.5">by {{ item.providerName }}</p>
                      </th>
                    }
                  </tr>
                </thead>
                <tbody class="divide-y divide-neutral-100 dark:divide-neutral-700">
                  <tr>
                    <td class="px-6 py-3 text-sm text-secondary font-medium">Category</td>
                    @for (item of comparisonData(); track item.serviceId) {
                      <td class="px-4 py-3">
                        <span class="text-xs px-2 py-1 rounded-full bg-esprit-red/10 text-esprit-red font-semibold">{{ item.category }}</span>
                      </td>
                    }
                  </tr>
                  <tr>
                    <td class="px-6 py-3 text-sm text-secondary font-medium">Price</td>
                    @for (item of comparisonData(); track item.serviceId) {
                      <td class="px-4 py-3">
                        @if (item.pricingType === 'PACKAGED') {
                          <span class="text-sm font-bold text-primary dark:text-white">From {{ item.startingPrice }} TND</span>
                          <span class="text-xs text-secondary block">({{ item.packageCount }} packages)</span>
                        } @else if (item.pricingType === 'HOURLY') {
                          <span class="text-sm font-bold text-primary dark:text-white">{{ item.price }} TND/h</span>
                        } @else {
                          <span class="text-sm font-bold text-primary dark:text-white">{{ item.price }} TND</span>
                        }
                        <span class="text-xs px-1.5 py-0.5 rounded bg-neutral-100 dark:bg-neutral-700 text-secondary ml-1">{{ item.pricingType }}</span>
                      </td>
                    }
                  </tr>
                  <tr>
                    <td class="px-6 py-3 text-sm text-secondary font-medium">Rating</td>
                    @for (item of comparisonData(); track item.serviceId) {
                      <td class="px-4 py-3">
                        @if (item.averageRating !== null) {
                          <span [class]="item.averageRating >= 4 ? 'text-green-600' : item.averageRating >= 3 ? 'text-amber-600' : 'text-red-600'" class="text-sm font-bold">{{ item.averageRating }}/5</span>
                          <span class="text-xs text-secondary block">({{ item.totalReviews }} reviews)</span>
                        } @else {
                          <span class="text-sm text-secondary">No reviews</span>
                        }
                      </td>
                    }
                  </tr>
                  <tr>
                    <td class="px-6 py-3 text-sm text-secondary font-medium">Bookings</td>
                    @for (item of comparisonData(); track item.serviceId) {
                      <td class="px-4 py-3">
                        <span class="text-sm font-semibold text-primary dark:text-white">{{ item.completedBookings }}</span>
                        <span class="text-xs text-secondary block">of {{ item.totalBookings }} total</span>
                      </td>
                    }
                  </tr>
                  <tr>
                    <td class="px-6 py-3 text-sm text-secondary font-medium">Completion Rate</td>
                    @for (item of comparisonData(); track item.serviceId) {
                      <td class="px-4 py-3">
                        <div class="flex items-center gap-2">
                          <div class="flex-1 h-2 rounded-full bg-neutral-200 dark:bg-neutral-700">
                            <div [class]="item.completionRate >= 80 ? 'bg-green-500' : item.completionRate >= 50 ? 'bg-amber-500' : 'bg-red-500'" class="h-full rounded-full transition-all" [style.width.%]="item.completionRate"></div>
                          </div>
                          <span class="text-xs font-semibold text-primary dark:text-white">{{ item.completionRate }}%</span>
                        </div>
                      </td>
                    }
                  </tr>
                  <tr>
                    <td class="px-6 py-3 text-sm text-secondary font-medium">Level</td>
                    @for (item of comparisonData(); track item.serviceId) {
                      <td class="px-4 py-3">
                        <span [class]="levelBadgeClass(item.level)" class="text-xs px-2 py-1 rounded-full font-semibold">{{ item.level.replace('_', ' ') }}</span>
                      </td>
                    }
                  </tr>
                  <tr>
                    <td class="px-6 py-3 text-sm text-secondary font-medium">Tags</td>
                    @for (item of comparisonData(); track item.serviceId) {
                      <td class="px-4 py-3">
                        @if (item.tags.length > 0) {
                          <div class="flex flex-wrap gap-1">
                            @for (tag of item.tags.slice(0, 4); track tag) {
                              <span class="text-xs px-1.5 py-0.5 rounded bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400">{{ tag }}</span>
                            }
                            @if (item.tags.length > 4) {
                              <span class="text-xs text-secondary">+{{ item.tags.length - 4 }}</span>
                            }
                          </div>
                        } @else {
                          <span class="text-xs text-secondary">None</span>
                        }
                      </td>
                    }
                  </tr>
                  @if (bestValue('averageRating') !== null) {
                    <tr>
                      <td class="px-6 py-3 text-sm text-secondary font-medium">Best Value</td>
                      @for (item of comparisonData(); track item.serviceId) {
                        <td class="px-4 py-3">
                          @if (isBest('averageRating', item.serviceId)) {
                            <span class="text-xs px-2 py-1 rounded-full bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 font-semibold">Top Rated</span>
                          } @else if (isBest('completionRate', item.serviceId)) {
                            <span class="text-xs px-2 py-1 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400 font-semibold">Most Reliable</span>
                          } @else if (isBest('totalBookings', item.serviceId)) {
                            <span class="text-xs px-2 py-1 rounded-full bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400 font-semibold">Most Popular</span>
                          }
                        </td>
                      }
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="px-6 py-4 border-t border-neutral-200 dark:border-neutral-700 flex justify-end gap-2">
              <button (click)="showModal.set(false)" class="btn btn-secondary text-sm">Close</button>
            </div>
          }
        </div>
      </div>
    }
  `
})
export class ServiceComparisonComponent {
  private readonly srvApi = inject(SrvApiService);
  private readonly router: any = null;

  readonly selectedIds = signal<Set<number>>(new Set());
  readonly selectedNames = signal<Map<number, string>>(new Map());
  readonly showModal = signal(false);
  readonly loading = signal(false);
  readonly comparisonData = signal<ComparisonItem[]>([]);
  readonly compareCount = computed(() => this.selectedIds().size);
  readonly compareList = computed(() => Array.from(this.selectedIds()));

  private windowRef = (typeof window !== 'undefined') ? window : null as any;

  toggleCompare(id: number, name: string): void {
    const current = new Set(this.selectedIds());
    if (current.has(id)) {
      current.delete(id);
    } else if (current.size < 5) {
      current.add(id);
    }
    this.selectedIds.set(current);
    const names = new Map(this.selectedNames());
    if (current.has(id)) {
      names.set(id, name);
    } else {
      names.delete(id);
    }
    this.selectedNames.set(names);
  }

  removeFromCompare(id: number): void {
    this.toggleCompare(id, '');
  }

  clearComparison(): void {
    this.selectedIds.set(new Set());
    this.selectedNames.set(new Map());
    this.comparisonData.set([]);
  }

  isInComparison(id: number): boolean {
    return this.selectedIds().has(id);
  }

  loadComparison(): void {
    this.loading.set(true);
    this.srvApi.compareServices(Array.from(this.selectedIds())).subscribe({
      next: (data) => {
        this.comparisonData.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.comparisonData.set([]);
        this.loading.set(false);
      }
    });
  }

  goToService(id: number): void {
    this.showModal.set(false);
    if (this.windowRef) {
      this.windowRef.location.href = '/services/' + id;
    }
  }

  isBest(field: string, serviceId: number): boolean {
    const data = this.comparisonData();
    const best = this.bestValue(field);
    if (best === null) return false;
    const item = data.find((d) => d.serviceId === serviceId);
    if (!item) return false;
    return (item as any)[field] === best;
  }

  bestValue(field: string): number | null {
    const data = this.comparisonData();
    if (data.length === 0) return null;
    const values = data.map((d) => (d as any)[field]).filter((v) => v !== null && v !== undefined);
    if (values.length === 0) return null;
    return Math.max(...values);
  }

  levelBadgeClass(level: string): string {
    switch (level) {
      case 'TOP_RATED': return 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400';
      case 'PRO': return 'bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400';
      case 'RISING': return 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400';
      default: return 'bg-neutral-100 dark:bg-neutral-700 text-neutral-600 dark:text-neutral-400';
    }
  }
}
