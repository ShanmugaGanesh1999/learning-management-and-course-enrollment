import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { EnrollmentResponse, EnrollmentStatus, EnrollmentStatsResponse, MyCourseItem, ProgressUpdateRequest } from '../models/enrollment.model';
import { PageResponse } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class EnrollmentService {
  constructor(private readonly api: ApiService) {}

  /**
   * POST /api/enrollments
   */
  enrollCourse(courseId: number): Observable<EnrollmentResponse> {
    return this.enroll(courseId);
  }

  enroll(courseId: number): Observable<EnrollmentResponse> {
    return this.api.post<EnrollmentResponse>('/enrollments', { courseId });
  }

  /**
   * GET /api/enrollments/my
   */
  getMyEnrollments(page = 0, size = 10): Observable<PageResponse<MyCourseItem>> {
    return this.myEnrollments({ page, size, sort: 'enrolledAt,desc', status: null });
  }

  myEnrollments(params: { page?: number; size?: number; sort?: string; status?: EnrollmentStatus | null }): Observable<PageResponse<MyCourseItem>> {
    return this.api.get<PageResponse<MyCourseItem>>('/enrollments/my', { params });
  }

  updateProgress(enrollmentId: number, progressPercentage: number): Observable<EnrollmentResponse> {
    const body: ProgressUpdateRequest = { progressPercentage };
    return this.api.patch<EnrollmentResponse>(`/enrollments/${enrollmentId}/progress`, body);
  }

  getEnrollment(enrollmentId: number): Observable<EnrollmentResponse> {
    return this.api.get<EnrollmentResponse>(`/enrollments/${enrollmentId}`);
  }

  cancel(enrollmentId: number): Observable<void> {
    return this.api.delete<void>(`/enrollments/${enrollmentId}`);
  }

  issueCertificate(enrollmentId: number): Observable<EnrollmentResponse> {
    return this.api.post<EnrollmentResponse>(`/enrollments/${enrollmentId}/certificate`, {});
  }

  /**
   * Instructor/admin view.
   * Note: backend currently exposes this under Enrollment-Service:
   * GET /api/enrollments/courses/{courseId}/enrollments
   */
  getCourseEnrollments(courseId: number, page = 0, size = 10): Observable<PageResponse<EnrollmentResponse>> {
    return this.courseEnrollments(courseId, { page, size, sort: 'enrolledAt,desc', status: null });
  }

  courseEnrollments(courseId: number, params: { page?: number; size?: number; sort?: string; status?: EnrollmentStatus | null }): Observable<PageResponse<EnrollmentResponse>> {
    return this.api.get<PageResponse<EnrollmentResponse>>(`/enrollments/courses/${courseId}/enrollments`, { params });
  }

  courseStats(courseId: number): Observable<EnrollmentStatsResponse> {
    return this.api.get<EnrollmentStatsResponse>(`/enrollments/courses/${courseId}/stats`);
  }
}
