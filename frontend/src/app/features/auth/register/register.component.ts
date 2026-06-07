import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService, UserRole } from '../../../services';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: []
})
export class RegisterComponent implements OnInit {
  private authService: AuthService;
  registerForm: FormGroup;
  currentStep = 1;
  totalSteps = 3;
  isSubmitting = false;
  errorMessage = '';
  backendUrl = environment.apiBaseUrl;
  fromSellButton = false;

  userRoles: { id: UserRole; label: string; icon: string; description: string }[] = [
    { id: 'visitor', label: 'User', icon: '👤', description: 'Buy and sell' },
    { id: 'seller', label: 'Seller', icon: '🏪', description: 'Sell products' },
    { id: 'service_provider', label: 'Service provider', icon: '🧰', description: 'Offer services' },
    { id: 'deliverer', label: 'Deliverer', icon: '🚗', description: 'Deliver packages' },
    { id: 'admin_delivery', label: 'Admin delivery', icon: '🗂️', description: 'Manage deliveries' },
    { id: 'recruiter', label: 'Recruiter', icon: '💼', description: 'Post job offers' },
    { id: 'event', label: 'Event Manager', icon: '🎪', description: 'Manage events' },
    { id: 'partner', label: 'Partner', icon: '🤝', description: 'Collaborate with the platform' },
    { id: 'admin_market', label: 'Admin Market', icon: '🏢', description: 'Manage student requests' }
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    authService: AuthService
  ) {
    this.authService = authService;
    this.registerForm = this.fb.group(
      {
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', Validators.required],
        role: ['visitor', Validators.required]
      },
      { validators: this.passwordMatch }
    );
  }

  ngOnInit(): void {
    const fromSellUrl = this.route.snapshot.queryParamMap.get('fromSell') === 'true';
    const fromSellStorage = localStorage.getItem('fromSell') === 'true';

    this.fromSellButton = fromSellUrl || fromSellStorage;

    if (this.fromSellButton) {
      localStorage.removeItem('fromSell');
      this.totalSteps = 2;
      // Seller access is granted only after approval/verification, never directly at registration.
      this.registerForm.get('role')?.setValue('visitor');
      this.cdr.detectChanges();
    }
  }

  passwordMatch(group: FormGroup): { [key: string]: any } | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { mismatch: true };
  }

  isCurrentStepValid(): boolean {
    switch (this.currentStep) {
      case 1:
        return (
          (this.registerForm.get('firstName')?.valid ?? false) &&
          (this.registerForm.get('lastName')?.valid ?? false) &&
          (this.registerForm.get('email')?.valid ?? false)
        );
      case 2:
        return (
          (this.registerForm.get('password')?.valid ?? false) &&
          (this.registerForm.get('confirmPassword')?.valid ?? false) &&
          !this.registerForm.errors?.['mismatch']
        );
      case 3:
        return this.registerForm.get('role')?.valid ?? false;
      default:
        return false;
    }
  }

  selectRole(role: string): void {
    this.registerForm.get('role')?.setValue(role);
  }

  onNext(): void {
    if (this.isCurrentStepValid()) {
      if (this.currentStep < this.totalSteps) {
        this.currentStep++;
      } else {
        this.onRegister();
      }
    }
  }

  onBack(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  onRegister(): void {
    if (this.registerForm.valid) {
      const firstName = this.registerForm.get('firstName')?.value as string;
      const lastName = this.registerForm.get('lastName')?.value as string;
      const email = this.registerForm.get('email')?.value as string;
      const password = this.registerForm.get('password')?.value as string;
      const role = this.registerForm.get('role')?.value as UserRole;

      this.isSubmitting = true;
      this.errorMessage = '';
      this.cdr.markForCheck();

      this.authService.register(email, password, `${firstName} ${lastName}`.trim(), role).subscribe({
        next: (user) => {
          this.isSubmitting = false;
          this.cdr.markForCheck();

          if (this.fromSellButton) {
            this.router.navigate(['/create-store'], { queryParams: { fresh: '1' } });
            return;
          }

          this.navigateByRole(user.role);
        },
        error: (error) => {
          this.isSubmitting = false;
          this.errorMessage = typeof error === 'string' ? error : 'Registration failed';
          this.cdr.markForCheck();
        }
      });
    }
  }

  private navigateByRole(role: UserRole): void {
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
      case 'recruiter':
        this.router.navigate(['/dashboard/recruiter']);
        break;
      case 'event':
        this.router.navigate(['/dashboard/events']);
        break;
      case 'partner':
        this.router.navigate(['/marketplace']);
        break;
      case 'admin_market':
        this.router.navigate(['/admin-market']);
        break;
      case 'visitor':
      default:
        this.router.navigate(['/home']);
        break;
    }
  }
}