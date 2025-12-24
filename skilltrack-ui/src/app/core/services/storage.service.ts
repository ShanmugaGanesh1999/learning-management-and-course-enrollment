import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly tokenKey = 'accessToken';
  private readonly refreshTokenKey = 'refreshToken';

  getToken(): string | null {
    return sessionStorage.getItem(this.tokenKey);
  }

  setToken(token: string): void {
    sessionStorage.setItem(this.tokenKey, token);
  }

  clearToken(): void {
    sessionStorage.removeItem(this.tokenKey);
  }

  getRefreshToken(): string | null {
    return sessionStorage.getItem(this.refreshTokenKey);
  }

  setRefreshToken(token: string): void {
    sessionStorage.setItem(this.refreshTokenKey, token);
  }

  clearRefreshToken(): void {
    sessionStorage.removeItem(this.refreshTokenKey);
  }

  clearAuth(): void {
    this.clearToken();
    this.clearRefreshToken();
  }
}
