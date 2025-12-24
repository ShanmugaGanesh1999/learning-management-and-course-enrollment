import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of, map, tap } from 'rxjs';

import { ApiService } from './api.service';
import { StorageService } from './storage.service';
import { UserProfile } from '../models/user.model';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  refreshToken?: string;
  userDetails: UserProfile;
  expiresIn?: number;
  tokenType?: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface TokenRefreshResponse {
  token: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  fullName: string;
  role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly user$ = new BehaviorSubject<UserProfile | null>(null);

  constructor(
    private readonly api: ApiService,
    private readonly storage: StorageService,
    private readonly router: Router
  ) {
    const token = this.storage.getToken();
    if (token && this.isTokenValid(token)) {
      // Best-effort restore identity; a real app would call /auth/me.
      this.user$.next(this.decodeUserFromToken(token));
    }
  }

  currentUser(): Observable<UserProfile | null> {
    return this.user$.asObservable();
  }

  getToken(): string | null {
    return this.storage.getToken();
  }

  /**
   * Token storage strategy:
   * - access token is stored in sessionStorage so a browser refresh keeps the session
   * - sessionStorage clears automatically when the tab/window is closed
   * - user profile is cached in-memory (BehaviorSubject) and can be refreshed via /auth/me
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.api.post<LoginResponse>('/auth/login', credentials).pipe(
      tap((res) => {
        this.storage.setToken(res.token);

        if (res.refreshToken) {
          this.storage.setRefreshToken(res.refreshToken);
        }

        this.user$.next(res.userDetails ?? this.decodeUserFromToken(res.token));
      })
    );
  }

  register(registerData: RegisterRequest): Observable<UserProfile> {
    return this.api.post<UserProfile>('/auth/register', registerData);
  }

  /**
   * Returns cached user if present; otherwise fetches from /auth/me.
   * Use this on app bootstrap or guarded routes to ensure profile is available.
   */
  getCurrentUser(): Observable<UserProfile | null> {
    const cached = this.user$.value;
    if (cached) {
      return of(cached);
    }
    if (!this.isLoggedIn()) {
      return of(null);
    }
    return this.api.get<UserProfile>('/auth/me').pipe(tap((u) => this.user$.next(u)));
  }

  me(): Observable<UserProfile> {
    return this.api.get<UserProfile>('/auth/me').pipe(tap((u) => this.user$.next(u)));
  }

  refreshToken(): Observable<TokenRefreshResponse> {
    const refreshToken = this.storage.getRefreshToken();
    if (!refreshToken) {
      return of({ token: '' } as TokenRefreshResponse);
    }

    const body: RefreshTokenRequest = { refreshToken };
    return this.api.post<TokenRefreshResponse>('/auth/refresh-token', body).pipe(
      tap((res) => {
        if (res.token) {
          this.storage.setToken(res.token);
          this.user$.next(this.decodeUserFromToken(res.token) ?? this.user$.value);
        }
        if (res.refreshToken) {
          this.storage.setRefreshToken(res.refreshToken);
        }
      })
    );
  }

  /**
   * Cleanup strategy:
   * - clear both access/refresh tokens from sessionStorage
   * - drop in-memory user cache
   * - redirect to /login so guards and UI are consistent
   */
  logout(redirectToLogin = true): void {
    this.storage.clearAuth();
    this.user$.next(null);

    if (redirectToLogin) {
      void this.router.navigateByUrl('/login');
    }
  }

  isLoggedIn(): boolean {
    const token = this.storage.getToken();
    return !!token && this.isTokenValid(token);
  }

  isAuthenticated(): boolean {
    return this.isLoggedIn();
  }

  hasRole(role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN'): boolean {
    const cached = this.user$.value;
    if (cached?.role) {
      return cached.role === role;
    }
    const token = this.storage.getToken();
    const decoded = token ? this.decodeUserFromToken(token) : null;
    return decoded?.role === role;
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  isTokenValid(token: string): boolean {
    const payload = this.decodeJwtPayload(token);
    if (!payload?.exp) return true;
    const nowSec = Math.floor(Date.now() / 1000);
    return payload.exp > nowSec;
  }

  private decodeUserFromToken(token: string): UserProfile | null {
    const payload = this.decodeJwtPayload(token);
    if (!payload) return null;
    return {
      id: payload.userId ?? null,
      username: payload.sub ?? null,
      role: payload.role ?? null,
      email: null,
      fullName: null
    };
  }

  private decodeJwtPayload(token: string): any | null {
    try {
      const parts = token.split('.');
      if (parts.length < 2) return null;
      const json = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json);
    } catch {
      return null;
    }
  }
}
