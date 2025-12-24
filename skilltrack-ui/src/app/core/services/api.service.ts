import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable, TimeoutError, catchError, defer, finalize, throwError, timeout } from 'rxjs';

import { LoadingService } from './loading.service';

export interface ApiError {
  timestamp: string;
  status: number;
  message: string;
  path?: string;
  errors?: any;
}

export interface RequestOptions {
  params?: Record<string, string | number | boolean | null | undefined>;
  headers?: HttpHeaders;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = environment.apiBaseUrl;
  private readonly requestTimeoutMs = 30_000;

  constructor(
    private readonly http: HttpClient,
    private readonly loading: LoadingService
  ) {}

  /**
   * Error handling strategy:
   * - Normalize various backend/network errors into a stable ApiError shape
   * - Re-throw as an observable error so components can show a message
   * - Apply a 30s timeout to avoid hanging UI on unreachable services
   */
  get<T>(path: string, options?: RequestOptions): Observable<T> {
    return this.wrap(
      this.http.get<T>(this.baseUrl + path, {
        params: this.toParams(options?.params),
        headers: options?.headers
      }),
      path
    );
  }

  post<T>(path: string, body: unknown, options?: RequestOptions): Observable<T> {
    return this.wrap(
      this.http.post<T>(this.baseUrl + path, body, {
        params: this.toParams(options?.params),
        headers: options?.headers
      }),
      path
    );
  }

  put<T>(path: string, body: unknown, options?: RequestOptions): Observable<T> {
    return this.wrap(
      this.http.put<T>(this.baseUrl + path, body, {
        params: this.toParams(options?.params),
        headers: options?.headers
      }),
      path
    );
  }

  patch<T>(path: string, body: unknown, options?: RequestOptions): Observable<T> {
    return this.wrap(
      this.http.patch<T>(this.baseUrl + path, body, {
        params: this.toParams(options?.params),
        headers: options?.headers
      }),
      path
    );
  }

  delete<T>(path: string, options?: RequestOptions): Observable<T> {
    return this.wrap(
      this.http.delete<T>(this.baseUrl + path, {
        params: this.toParams(options?.params),
        headers: options?.headers
      }),
      path
    );
  }

  private wrap<T>(source$: Observable<T>, path: string): Observable<T> {
    return defer(() => {
      this.loading.start();
      return source$.pipe(
        timeout({ first: this.requestTimeoutMs }),
        catchError((err) => this.toApiError(err, path)),
        finalize(() => this.loading.stop())
      );
    });
  }

  private toApiError(err: unknown, path: string): Observable<never> {
    const now = new Date().toISOString();

    if (err instanceof TimeoutError) {
      const apiErr: ApiError = {
        timestamp: now,
        status: 504,
        message: 'Request timed out',
        path
      };
      return throwError(() => apiErr);
    }

    if (err instanceof HttpErrorResponse) {
      const message =
        (err.error && (err.error.message || err.error.error || err.error.title)) ||
        err.message ||
        'Request failed';

      const apiErr: ApiError = {
        timestamp: now,
        status: err.status || 0,
        message,
        path,
        errors: err.error?.errors ?? err.error
      };
      return throwError(() => apiErr);
    }

    const apiErr: ApiError = {
      timestamp: now,
      status: 0,
      message: 'Unexpected error',
      path,
      errors: err
    };
    return throwError(() => apiErr);
  }

  private toParams(params?: Record<string, string | number | boolean | null | undefined>): HttpParams {
    let p = new HttpParams();
    if (!params) return p;
    for (const [k, v] of Object.entries(params)) {
      if (v === null || v === undefined) continue;
      p = p.set(k, String(v));
    }
    return p;
  }
}
