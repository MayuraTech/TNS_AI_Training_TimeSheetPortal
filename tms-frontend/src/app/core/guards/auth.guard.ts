import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Role } from '../models/user.model';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    router.navigate(['/auth/login']);
    return false;
  }

  if (auth.user()?.forcePasswordChange) {
    router.navigate(['/auth/change-password']);
    return false;
  }

  return true;
};

export const roleGuard = (requiredRole: Role): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    router.navigate(['/auth/login']);
    return false;
  }

  if (!auth.hasRole(requiredRole)) {
    const dashRoute = auth.getRoleDashboardRoute(auth.activeRole()!);
    router.navigate([dashRoute]);
    return false;
  }

  return true;
};

export const forcePasswordChangeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    router.navigate(['/auth/login']);
    return false;
  }

  return true;
};
