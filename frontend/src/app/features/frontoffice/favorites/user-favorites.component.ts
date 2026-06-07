import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { SkeletonComponent } from '../../../shared/components';
import { SrvApiService, AuthService } from '../../../services';
import { ServiceResponse as ServiceResource } from '@esprit-market/api-types';

@Component({
  selector: 'app-user-favorites',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent, SkeletonComponent],
  templateUrl: './user-favorites.component.html',
  styleUrls: []
})
export class UserFavoritesComponent implements OnInit {
  private readonly srvApi = inject(SrvApiService);
  private readonly authService = inject(AuthService);
  readonly router = inject(Router);

  readonly favorites = signal<ServiceResource[]>([]);
  readonly isLoading = signal(true);

  ngOnInit(): void {
    this.loadFavorites();
  }

  removeFavorite(serviceId: number | undefined, event: Event): void {
    event.stopPropagation();
    if (serviceId == null) return;
    this.srvApi.removeFavorite(serviceId).subscribe({
      next: () => {
        this.favorites.update(list => list.filter((s) => s.id !== serviceId));
      }
    });
  }

  goToServiceDetail(serviceId: number | undefined): void {
    if (serviceId == null) return;
    this.router.navigate(['/services', serviceId]);
  }

  private loadFavorites(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    this.srvApi.getFavoriteServices(0, 100).subscribe({
      next: (page) => {
        this.favorites.set(page.content);
        this.isLoading.set(false);
      },
      error: () => {
        this.favorites.set([]);
        this.isLoading.set(false);
      }
    });
  }
}
