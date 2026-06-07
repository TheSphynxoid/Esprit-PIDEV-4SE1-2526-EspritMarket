import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService, UserRole, ThemeService } from '../../../services';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: []
})
export class LoginComponent {

  authService = inject(AuthService);
  themeService = inject(ThemeService);
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  loginForm: FormGroup;
  errorMessage = signal('');
  isLoading = computed(() => this.authService.isLoading());

  constructor() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    // ─── Sauvegarde fromSell dans localStorage dès l'ouverture ───
    // Comme ça même si l'URL change, on ne perd pas l'info
    const fromSell = this.route.snapshot.queryParamMap.get('fromSell');
    if (fromSell === 'true') {
      localStorage.setItem('fromSell', 'true');
    }
  }

  onLogin(): void {
    if (this.loginForm.invalid) return;

    const { email, password } = this.loginForm.value;
    this.errorMessage.set('');

    this.authService.login(email, password).subscribe({
      next: (user) => {

        // ─── Lit fromSell depuis localStorage ───────────────────
        const fromSell = localStorage.getItem('fromSell');

        if (fromSell === 'true') {
          // Nettoie localStorage
          localStorage.removeItem('fromSell');

          // Redirige vers register avec fromSell=true
          this.router.navigate(['/register'], {
            queryParams: { fromSell: 'true' }
          });
          return;
        }

        // ─── Comportement normal ─────────────────────────────────
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || null;
        if (returnUrl) {
          this.router.navigateByUrl(returnUrl);
        } else {
          this.routeByRole(user.role);
        }
      },
      error: (err) => {
        this.errorMessage.set(err || 'Could not log in. Please try again.');
      }
    });
  }

  private routeByRole(role: UserRole): void {
    switch (role) {
      case 'seller':
        this.router.navigate(['/dashboard/vendor']);
        break;
      case 'service_provider':
        this.router.navigate(['/dashboard/service-provider']);
        break;
      case 'deliverer':
        this.router.navigate(['/dashboard/deliverer']);
        break;
      case 'admin_delivery':
        this.router.navigate(['/dashboard/admin-delivery']);
        break;
      case 'admin_market':
        this.router.navigate(['/admin-market']);
        break;
      case 'recruiter':
        this.router.navigate(['/dashboard/recruiter']);
        break;
      case 'event':
        this.router.navigate(['/dashboard/events']);
        break;
      case 'partner':
        this.router.navigate(['/marketplace']);
        break;
      case 'visitor':
      default:
        this.router.navigate(['/home']);
        break;
    }
  }
}