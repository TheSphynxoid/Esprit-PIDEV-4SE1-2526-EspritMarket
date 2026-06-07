import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {
  BehaviorSubject,
  Observable,
  catchError,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  finalize,
  map,
  of,
  shareReplay,
  switchMap,
  tap
} from 'rxjs';

import { ApiConfigService } from './api';

export interface ProductSearchResult {
  productId: number;
  name: string;
  price: number;
  storeName: string;
  categoryName: string;
}

export interface ProductSearchPage {
  content: ProductSearchResult[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

interface SearchState {
  q: string;
  minPrice: number | null;
  maxPrice: number | null;
  category: string;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class ProductSearchService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);

  private readonly querySubject = new BehaviorSubject<string>('');
  private readonly minPriceSubject = new BehaviorSubject<number | null>(null);
  private readonly maxPriceSubject = new BehaviorSubject<number | null>(null);
  private readonly categorySubject = new BehaviorSubject<string>('');
  private readonly pageSubject = new BehaviorSubject<number>(0);
  private readonly sizeSubject = new BehaviorSubject<number>(12);

  private readonly loadingSubject = new BehaviorSubject<boolean>(false);
  private readonly errorSubject = new BehaviorSubject<string | null>(null);

  readonly loading$ = this.loadingSubject.asObservable();
  readonly error$ = this.errorSubject.asObservable();

  readonly results$: Observable<ProductSearchPage> = combineLatest([
    this.querySubject,
    this.minPriceSubject,
    this.maxPriceSubject,
    this.categorySubject,
    this.pageSubject,
    this.sizeSubject
  ]).pipe(
    debounceTime(400),
    map(([q, minPrice, maxPrice, category, page, size]) => ({
      q: (q || '').trim(),
      minPrice,
      maxPrice,
      category: (category || '').trim(),
      page,
      size
    })),
    distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
    tap(() => {
      this.loadingSubject.next(true);
      this.errorSubject.next(null);
    }),
    switchMap((state) => this.searchFromApi(state)),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  setQuery(query: string): void {
    this.querySubject.next(query || '');
    this.pageSubject.next(0);
  }

  setMinPrice(minPrice: number | null): void {
    this.minPriceSubject.next(minPrice);
    this.pageSubject.next(0);
  }

  setMaxPrice(maxPrice: number | null): void {
    this.maxPriceSubject.next(maxPrice);
    this.pageSubject.next(0);
  }

  setCategory(category: string): void {
    this.categorySubject.next(category || '');
    this.pageSubject.next(0);
  }

  setPage(page: number): void {
    this.pageSubject.next(Math.max(0, page));
  }

  setSize(size: number): void {
    const safeSize = Math.max(1, Math.min(50, size));
    this.sizeSubject.next(safeSize);
    this.pageSubject.next(0);
  }

  reset(): void {
    this.querySubject.next('');
    this.minPriceSubject.next(null);
    this.maxPriceSubject.next(null);
    this.categorySubject.next('');
    this.pageSubject.next(0);
  }

  private searchFromApi(state: SearchState): Observable<ProductSearchPage> {
    const params = this.toHttpParams(state);
    const url = this.apiConfig.buildUrl('/api/search/products');

    return this.http.get<ProductSearchPage>(url, { params }).pipe(
      catchError(() => {
        this.errorSubject.next('Unable to load search results right now.');
        return of(this.emptyPage(state.page, state.size));
      }),
      finalize(() => {
        this.loadingSubject.next(false);
      })
    );
  }

  private toHttpParams(state: SearchState): HttpParams {
    let params = new HttpParams()
      .set('page', String(state.page))
      .set('size', String(state.size));

    if (state.q) {
      params = params.set('q', state.q);
    }
    if (state.minPrice !== null && Number.isFinite(state.minPrice)) {
      params = params.set('minPrice', String(state.minPrice));
    }
    if (state.maxPrice !== null && Number.isFinite(state.maxPrice)) {
      params = params.set('maxPrice', String(state.maxPrice));
    }
    if (state.category) {
      params = params.set('category', state.category);
    }

    return params;
  }

  private emptyPage(page: number, size: number): ProductSearchPage {
    return {
      content: [],
      number: page,
      size,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    };
  }
}
