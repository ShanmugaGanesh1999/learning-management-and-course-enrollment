import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { AuthService } from '../services/auth.service';

/**
 * Role-based route protection.
 *
 * Guard lifecycle:
 * - runs before route activation
 * - can return boolean (allow/deny) or UrlTree (redirect)
 *
 * Usage (routes):
 * - data: { role: 'ADMIN' } or data: { roles: ['ADMIN','INSTRUCTOR'] }
 */
@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private readonly auth: AuthService, private readonly router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): boolean | UrlTree {
    const role = route.data['role'] as string | undefined;
    const roles = route.data['roles'] as string[] | undefined;

    const required = roles ?? (role ? [role] : []);
    if (required.length === 0) {
      return true;
    }

    if (!this.auth.isLoggedIn()) {
      return this.router.parseUrl('/login');
    }

    const ok = required.some((r) => this.auth.hasRole(r as any));
    if (ok) {
      return true;
    }

    return this.router.parseUrl('/unauthorized');
  }
}
