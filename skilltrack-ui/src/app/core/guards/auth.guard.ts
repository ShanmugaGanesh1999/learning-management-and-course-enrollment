import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';

import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private readonly auth: AuthService, private readonly router: Router) {}

  /**
   * Guard lifecycle:
   * - runs before the route activates
   * - if not authenticated, returns a UrlTree to redirect to /login
   */
  canActivate(): boolean | UrlTree {
    if (this.auth.isLoggedIn()) {
      return true;
    }

    this.auth.logout(false);
    return this.router.parseUrl('/login');
  }
}
