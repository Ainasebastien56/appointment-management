
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, UserRole } from '../services/auth.service';

export const authGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getStoredToken();
  const role = authService.getCurrentRole();

  // 1. Vérifier que l'utilisateur est connecté.
  if (!token || !role) {
    authService.logout();
    return router.createUrlTree(['/']);
  }

  // 2. Vérifier le rôle nécessaire pour cette page.
  const expectedRole = route.data['role'] as UserRole | undefined;

  if (expectedRole && role !== expectedRole) {
    // Rediriger vers le tableau de bord correspondant à son rôle.
    return router.createUrlTree([
      authService.getDashboardUrl(role),
    ]);
  }

  // 3. Autoriser l'accès.
  return true;
};
