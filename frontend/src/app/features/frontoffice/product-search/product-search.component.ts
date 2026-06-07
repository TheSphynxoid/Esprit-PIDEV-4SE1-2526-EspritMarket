import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import {
  ProductSearchPage,
  ProductSearchService
} from '../../../services/product-search.service';
import { CategoryResource, MarketplaceApiService } from '../../../services/api';

@Component({
  selector: 'app-product-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HeaderComponent, FooterComponent],
  templateUrl: './product-search.component.html',
  styleUrl: './product-search.component.css'
})
export class ProductSearchComponent implements OnInit {
  private readonly productSearchService = inject(ProductSearchService);
  private readonly marketplaceApi = inject(MarketplaceApiService);

  readonly resultsPage$: Observable<ProductSearchPage> = this.productSearchService.results$;
  readonly loading$ = this.productSearchService.loading$;
  readonly error$ = this.productSearchService.error$;

  readonly queryControl = new FormControl<string>('', { nonNullable: true });
  readonly minPriceControl = new FormControl<string>('');
  readonly maxPriceControl = new FormControl<string>('');
  readonly categoryControl = new FormControl<string>('', { nonNullable: true });

  categories: string[] = [];

  ngOnInit(): void {
    this.productSearchService.reset();

    this.queryControl.valueChanges.subscribe((value) => {
      this.productSearchService.setQuery(value || '');
    });

    this.minPriceControl.valueChanges.subscribe((value) => {
      const numeric = this.toNullableNumber(value);
      this.productSearchService.setMinPrice(numeric);
    });

    this.maxPriceControl.valueChanges.subscribe((value) => {
      const numeric = this.toNullableNumber(value);
      this.productSearchService.setMaxPrice(numeric);
    });

    this.categoryControl.valueChanges.subscribe((value) => {
      this.productSearchService.setCategory(value || '');
    });

    this.loadCategories();
  }

  goToPage(page: number): void {
    this.productSearchService.setPage(page);
  }

  resetFilters(): void {
    this.queryControl.setValue('');
    this.minPriceControl.setValue('');
    this.maxPriceControl.setValue('');
    this.categoryControl.setValue('');
    this.productSearchService.reset();
  }

  getPageNumbers(page: ProductSearchPage): number[] {
    if (!page.totalPages || page.totalPages <= 1) {
      return [1];
    }

    const current = page.number + 1;
    const start = Math.max(1, current - 2);
    const end = Math.min(page.totalPages, current + 2);
    const pages: number[] = [];

    for (let i = start; i <= end; i += 1) {
      pages.push(i);
    }

    return pages;
  }

  private loadCategories(): void {
    this.marketplaceApi.categories.list().subscribe({
      next: (categories: CategoryResource[]) => {
        const names = categories
          .map((category) => String(category.name || '').trim())
          .filter((name) => !!name);
        this.categories = Array.from(new Set(names)).sort((a, b) => a.localeCompare(b));
      },
      error: () => {
        this.categories = [];
      }
    });
  }

  private toNullableNumber(value: string | null): number | null {
    if (value === null || value.trim() === '') {
      return null;
    }

    const numeric = Number(value);
    if (!Number.isFinite(numeric)) {
      return null;
    }

    return numeric;
  }
}
