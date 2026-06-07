import { Injectable } from '@angular/core';
import { signal, computed } from '@angular/core';

export interface VendorStats {
  totalSales: number;
  productsCount: number;
  rating: number;
  reviewCount: number;
  monthlyGrowth: number;
  currency: string;
}

export interface VendorProduct {
  id: number;
  name: string;
  price: number;
  stock: number;
  sales: number;
  rating: number;
  status: 'active' | 'inactive' | 'out-of-stock';
}

export interface VendorOrder {
  id: string;
  orderNumber: string;
  customer: string;
  total: number;
  status: 'pending' | 'shipped' | 'delivered';
  date: Date;
}

export interface DeliveryStats {
  completedDeliveries: number;
  pendingDeliveries: number;
  earnings: number;
  rating: number;
  averageRating: number;
  currency: string;
}

export interface RecruiterStats {
  activeOffers: number;
  totalApplications: number;
  hiredCandidates: number;
  rating: number;
  currency: string;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  // Vendor Dashboard
  private vendorStats = signal<VendorStats>({
    totalSales: 2450,
    productsCount: 12,
    rating: 4.8,
    reviewCount: 89,
    monthlyGrowth: 15,
    currency: 'DT'
  });

  private vendorProducts = signal<VendorProduct[]>([
    {
      id: 1,
      name: 'Advanced JavaScript',
      price: 45,
      stock: 15,
      sales: 234,
      rating: 4.8,
      status: 'active'
    },
    {
      id: 2,
      name: 'TypeScript Reference',
      price: 35,
      stock: 8,
      sales: 156,
      rating: 4.6,
      status: 'active'
    },
    {
      id: 3,
      name: 'Angular Guide',
      price: 55,
      stock: 0,
      sales: 89,
      rating: 4.7,
      status: 'out-of-stock'
    }
  ]);

  private vendorOrders = signal<VendorOrder[]>([
    {
      id: '1',
      orderNumber: 'ORD-2025-001',
      customer: 'Fatima Khouja',
      total: 450,
      status: 'delivered',
      date: new Date('2025-10-15')
    },
    {
      id: '2',
      orderNumber: 'ORD-2025-002',
      customer: 'Mohamed Saidi',
      total: 120,
      status: 'shipped',
      date: new Date('2025-10-18')
    },
    {
      id: '3',
      orderNumber: 'ORD-2025-003',
      customer: 'Leila Mansouri',
      total: 250,
      status: 'pending',
      date: new Date('2025-10-19')
    }
  ]);

  // Delivery Dashboard
  private deliveryStats = signal<DeliveryStats>({
    completedDeliveries: 145,
    pendingDeliveries: 8,
    earnings: 3200,
    rating: 4.9,
    averageRating: 4.85,
    currency: 'DT'
  });

  // Recruiter Dashboard
  private recruiterStats = signal<RecruiterStats>({
    activeOffers: 5,
    totalApplications: 156,
    hiredCandidates: 12,
    rating: 4.7,
    currency: 'DT'
  });

  // Computed values - Vendor
  vendorStats$ = computed(() => this.vendorStats());
  vendorProducts$ = computed(() => this.vendorProducts());
  vendorOrders$ = computed(() => this.vendorOrders());

  vendorPendingOrders = computed(() =>
    this.vendorOrders().filter(o => o.status === 'pending')
  );

  vendorOutOfStock = computed(() =>
    this.vendorProducts().filter(p => p.status === 'out-of-stock').length
  );

  // Computed values - Delivery
  deliveryStats$ = computed(() => this.deliveryStats());

  // Computed values - Recruiter
  recruiterStats$ = computed(() => this.recruiterStats());

  constructor() {}

  // Vendor methods
  updateVendorStats(updates: Partial<VendorStats>): void {
    this.vendorStats.update(stats => ({ ...stats, ...updates }));
  }

  addVendorProduct(product: Omit<VendorProduct, 'id'>): void {
    const newProduct: VendorProduct = {
      ...product,
      id: Math.max(0, ...this.vendorProducts().map(p => p.id)) + 1
    };
    this.vendorProducts.update(products => [...products, newProduct]);
  }

  updateVendorProduct(id: number, updates: Partial<VendorProduct>): void {
    this.vendorProducts.update(products =>
      products.map(p => p.id === id ? { ...p, ...updates } : p)
    );
  }

  deleteVendorProduct(id: number): void {
    this.vendorProducts.update(products => products.filter(p => p.id !== id));
  }

  updateVendorOrderStatus(orderId: string, status: VendorOrder['status']): void {
    this.vendorOrders.update(orders =>
      orders.map(o => o.id === orderId ? { ...o, status } : o)
    );
  }

  // Delivery methods
  updateDeliveryStats(updates: Partial<DeliveryStats>): void {
    this.deliveryStats.update(stats => ({ ...stats, ...updates }));
  }

  completeDelivery(orderId: string): void {
    this.deliveryStats.update(stats => ({
      ...stats,
      completedDeliveries: stats.completedDeliveries + 1,
      pendingDeliveries: Math.max(0, stats.pendingDeliveries - 1)
    }));
  }

  // Recruiter methods
  updateRecruiterStats(updates: Partial<RecruiterStats>): void {
    this.recruiterStats.update(stats => ({ ...stats, ...updates }));
  }

  addApplication(): void {
    this.recruiterStats.update(stats => ({
      ...stats,
      totalApplications: stats.totalApplications + 1
    }));
  }

  hireCandidate(): void {
    this.recruiterStats.update(stats => ({
      ...stats,
      hiredCandidates: stats.hiredCandidates + 1
    }));
  }

  // General dashboard methods
  getDashboardStats(role: 'vendor' | 'deliverer' | 'recruiter'): any {
    switch (role) {
      case 'vendor':
        return this.vendorStats$();
      case 'deliverer':
        return this.deliveryStats$();
      case 'recruiter':
        return this.recruiterStats$();
    }
  }

  getRevenueChart(role: 'vendor' | 'deliverer'): Array<{ month: string; revenue: number }> {
    const baseData = [
      { month: 'Jan', revenue: 1200 },
      { month: 'Feb', revenue: 1900 },
      { month: 'Mar', revenue: 1600 },
      { month: 'Apr', revenue: 2400 },
      { month: 'May', revenue: 2200 },
      { month: 'Jun', revenue: 2800 },
      { month: 'Jul', revenue: 3200 },
      { month: 'Aug', revenue: 2900 },
      { month: 'Sep', revenue: 3100 },
      { month: 'Oct', revenue: 2450 }
    ];

    if (role === 'deliverer') {
      return baseData.map(d => ({ ...d, revenue: Math.round(d.revenue * 0.6) }));
    }

    return baseData;
  }
}
