import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { finalize, timeout } from 'rxjs/operators';

import { MarketplaceApiService, VisualSearchResponseResource } from '../../../services/api';
import { FooterComponent, HeaderComponent } from '../../../shared/layout';

@Component({
  selector: 'app-visual-search',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent],
  templateUrl: './visual-search.component.html',
    styleUrl: './visual-search.component.css'
})
export class VisualSearchComponent {
  private readonly marketplaceApi = inject(MarketplaceApiService);
  private readonly cdr = inject(ChangeDetectorRef);
  private loadingWatchdog: ReturnType<typeof setTimeout> | null = null;

  loading = false;
  error = '';
  warning = '';        // ← ajouté
  previewUrl = '';
  imageBase64 = '';
  response: VisualSearchResponseResource | null = null;

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    this.error = '';
    this.warning = '';   // ← ajouté
    this.response = null;

    if (!file) {
      this.previewUrl = '';
      this.imageBase64 = '';
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.error = 'Veuillez sélectionner un fichier image valide.';
      this.previewUrl = '';
      this.imageBase64 = '';
      return;
    }

    const maxBytes = 5 * 1024 * 1024;
    if (file.size > maxBytes) {
      this.error = 'Image trop volumineuse (max 5 MB).';
      this.previewUrl = '';
      this.imageBase64 = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const base64 = typeof reader.result === 'string' ? reader.result : '';
      this.previewUrl = base64;
      this.imageBase64 = base64;
      this.cdr.markForCheck();
    };
    reader.onerror = () => {
      this.error = 'Impossible de lire ce fichier image.';
      this.previewUrl = '';
      this.imageBase64 = '';
      this.cdr.markForCheck();
    };
    reader.readAsDataURL(file);
  }

  runVisualSearch(): void {
    if (!this.imageBase64) {
      this.error = 'Veuillez uploader une image avant de lancer la recherche.';
      return;
    }

    this.startLoading();
    this.error = '';
    this.warning = '';   // ← ajouté
    this.response = null;

    this.marketplaceApi.visualSearch({ imageBase64: this.imageBase64 })
      .pipe(
        timeout(20000),
        finalize(() => {
          this.stopLoading();
        })
      )
      .subscribe({
        next: (response: VisualSearchResponseResource) => {
          this.response = response;

          // ← modifié : avertissement discret si résultats présents, erreur sinon
          if (response.style === 'ai-unavailable') {
            if (response.results && response.results.length > 0) {
              this.warning = 'Analyse IA indisponible — résultats basés sur la recherche visuelle.';
              this.error = '';
            } else {
              this.error = 'Le service IA est indisponible. Aucun résultat trouvé.';
            }
          }

          this.cdr.markForCheck();
        },
        error: (err: any) => {
          if (String(err?.name || '').toLowerCase() === 'timeouterror') {
            this.error = 'La recherche visuelle a pris trop de temps. Reessayez avec une image plus legere.';
            this.applyLocalFallbackResponse();
            this.cdr.markForCheck();
            return;
          }

          this.error = err?.error?.message || 'La recherche visuelle a échoué.';

          if (Number(err?.status || 0) >= 400) {
            this.applyLocalFallbackResponse();
          }
          this.cdr.markForCheck();
        }
      });
  }

  private startLoading(): void {
    this.loading = true;
    if (this.loadingWatchdog) {
      clearTimeout(this.loadingWatchdog);
    }
    this.loadingWatchdog = setTimeout(() => {
      this.loading = false;
      if (!this.error) {
        this.error = 'Requete trop longue. Veuillez reessayer.';
      }
      if (!this.response) {
        this.applyLocalFallbackResponse();
      }
      this.cdr.markForCheck();
    }, 25000);
    this.cdr.markForCheck();
  }

  private stopLoading(): void {
    this.loading = false;
    if (this.loadingWatchdog) {
      clearTimeout(this.loadingWatchdog);
      this.loadingWatchdog = null;
    }
    this.cdr.markForCheck();
  }

  private applyLocalFallbackResponse(): void {
    this.response = {
      detectedType: 'Analyse IA indisponible',
      colors: [],
      style: 'ai-unavailable',
      keywords: [],
      results: []
    };
  }

  getNoResultMessage(): string {
    if (!this.response) {
      return 'Aucun produit similaire trouve.';
    }

    if (this.response.style === 'ai-unavailable') {
      return 'Aucun produit similaire trouvé pour le moment.';
    }

    const type = String(this.response.detectedType || '').trim();
    if (type && type.toLowerCase() !== 'n/a' && type.toLowerCase() !== 'exact-match') {
      return `Désolé, ce produit (${type}) n'est pas disponible dans notre marketplace pour le moment.`;
    }

    return "Ce produit n'est pas disponible dans la marketplace.";
  }

  getTagList(values: string[] | undefined): string {
    return (values || []).join(' • ');
  }

  resolveImageUrl(raw: string | null | undefined): string {
    const value = String(raw || '').trim();
    if (!value) return '';

    if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:image/')) {
      return value;
    }

    if (value.startsWith('/uploads/')) {
      return `http://localhost:8088${value}`;
    }

    if (value.startsWith('uploads/')) {
      return `http://localhost:8088/${value}`;
    }

    if (value.startsWith('/products/') || value.startsWith('products/')) {
      const cleaned = value.replace(/^\/?products\//, '/uploads/products/');
      return `http://localhost:8088${cleaned}`;
    }

    return value;
  }
}