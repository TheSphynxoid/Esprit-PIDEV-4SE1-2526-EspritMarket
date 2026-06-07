import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { JwtService } from '../services/api';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const jwtService = inject(JwtService);
  const router = inject(Router);

  if (authService.isLoggedIn() && !jwtService.isTokenExpired()) {
    return true;
  }

  if (jwtService.isTokenExpired()) {
    authService.logout();
  }

  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const jwtService = inject(JwtService);
  const router = inject(Router);

  if (!authService.isLoggedIn() || jwtService.isTokenExpired()) {
    if (jwtService.isTokenExpired()) {
      authService.logout();
    }
    router.navigate(['/login']);
    return false;
  }

  const expectedRoles = route.data['roles'] as string[];
  const currentRole = authService.currentUser()?.role;

  if (expectedRoles && currentRole && expectedRoles.includes(currentRole)) {
    return true;
  }

  // Redirect to role-specific dashboards
  switch (currentRole) {
    case 'seller':
      router.navigate(['/dashboard/vendor']);
      break;
    case 'service_provider':
      router.navigate(['/dashboard/service-provider']);
      break;
    case 'deliverer':
      router.navigate(['/dashboard/deliverer']);
      break;
    case 'admin_delivery':
      router.navigate(['/dashboard/admin-delivery']);
      break;
    case 'admin_market':
      router.navigate(['/admin-market']);
      break;
    case 'recruiter':
      router.navigate(['/dashboard/recruiter']);
      break;
    case 'event':
      router.navigate(['/dashboard/events']);
      break;
    case 'partner':
      router.navigate(['/marketplace']);
      break;
    case 'visitor':
    default:
      router.navigate(['/home']);
  }

  return false;
};
