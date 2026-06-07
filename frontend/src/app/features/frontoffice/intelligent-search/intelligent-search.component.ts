import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MarketplaceApiService, SemanticSearchResponseResource } from '../../../services/api';
import { FooterComponent, HeaderComponent } from '../../../shared/layout';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-intelligent-search',
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, FooterComponent],
  templateUrl: './intelligent-search.component.html',
    styleUrl: './intelligent-search.component.css'
})
export class IntelligentSearchComponent {
  private readonly marketplaceApi = inject(MarketplaceApiService);

  query = '';
  loading = false;
  response: SemanticSearchResponseResource | null = null;
  error = '';

  onSearch(): void {
    if (!this.query.trim()) return;

    this.loading = true;
    this.error = '';
    
    this.marketplaceApi.semanticSearch({ query: this.query })
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (res) => {
          this.response = res;
        },
        error: (err) => {
          this.error = 'Erreur lors de la recherche intelligente.';
          console.error(err);
        }
      });
  }

  resolveImageUrl(raw: string | null | undefined): string {
    const value = String(raw || '').trim();
    if (!value) return '';
    if (value.startsWith('http')) return value;
    if (value.startsWith('/uploads/') || value.startsWith('uploads/')) {
      return `http://localhost:8088${value.startsWith('/') ? '' : '/'}${value}`;
    }
    if (value.startsWith('/products/') || value.startsWith('products/')) {
      const cleaned = value.replace(/^\/?products\//, '/uploads/products/');
      return `http://localhost:8088${cleaned}`;
    }
    return `http://localhost:8088${value.startsWith('/') ? '' : '/'}${value}`;
  }
}
