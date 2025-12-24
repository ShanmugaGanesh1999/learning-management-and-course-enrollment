import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Simple global loading indicator.
 *
 * ApiService increments a counter per in-flight request and decrements on completion.
 * Components can subscribe to `loading$` if they want to show a global spinner.
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private inFlight = 0;
  private readonly loadingSubject = new BehaviorSubject<boolean>(false);

  loading$(): Observable<boolean> {
    return this.loadingSubject.asObservable();
  }

  start(): void {
    this.inFlight += 1;
    this.loadingSubject.next(true);
  }

  stop(): void {
    this.inFlight = Math.max(0, this.inFlight - 1);
    this.loadingSubject.next(this.inFlight > 0);
  }
}
