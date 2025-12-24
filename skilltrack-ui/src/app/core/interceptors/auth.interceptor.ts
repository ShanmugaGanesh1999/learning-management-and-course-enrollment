import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.getToken();

  // Never attach Bearer token to refresh-token endpoint.
  // This keeps refresh independent of possibly-expired access tokens.
  const isRefresh = req.url.includes('/auth/refresh-token');

  const alreadyRetried = req.headers.get('X-Retry-After-Refresh') === '1';

  const requestWithAuth = !token || isRefresh
    ? req
    : req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });

  return next(requestWithAuth).pipe(
    catchError((err: any) => {
      // 401 handling:
      // - try to refresh token once
      // - replay the original request with the new token
      // - if refresh fails, logout and redirect to /login
      if (err?.status !== 401 || isRefresh || alreadyRetried) {
        return throwError(() => err);
      }

      return auth.refreshToken().pipe(
        switchMap((res) => {
          if (!res?.token) {
            auth.logout(false);
            void router.navigateByUrl('/login');
            return throwError(() => err);
          }

          const retried = req.clone({
            setHeaders: {
              Authorization: `Bearer ${res.token}`,
              'X-Retry-After-Refresh': '1'
            }
          });

          return next(retried);
        }),
        catchError(() => {
          auth.logout(false);
          void router.navigateByUrl('/login');
          return throwError(() => err);
        })
      );
    })
  );
};
