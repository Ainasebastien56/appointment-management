import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getStoredToken();
  
  
  // Les endpoints de connexion et d'inscription n'ont pas besoin du JWT.
  if (!token || req.url.includes('/api/auth/')){
    return next(req);
  }

  const authReq = req.clone({
    setHeaders:{
      Authorization:`Bearer ${token}`,
    },
  });


  return next(authReq);
};
