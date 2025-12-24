import { Injectable } from '@angular/core';
import { Observable, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';

import { ApiService } from './api.service';
import { PageResponse } from '../models/page.model';
import { CourseDetailResponse, CourseSummary, Module } from '../models/course.model';

@Injectable({ providedIn: 'root' })
export class CourseService {
  constructor(private readonly api: ApiService) {}

  /**
   * GET /api/courses with pagination + filters.
   * Note: base URL already includes /api via environment.apiBaseUrl.
   */
  getCourses(
    page = 0,
    size = 10,
    sort = 'createdAt,desc',
    filters?: { category?: string; level?: string; keyword?: string }
  ): Observable<PageResponse<CourseSummary>> {
    return this.api.get<PageResponse<CourseSummary>>('/courses', {
      params: {
        page,
        size,
        sort,
        category: filters?.category,
        level: filters?.level,
        keyword: filters?.keyword
      }
    });
  }

  listPublished(params: {
    page?: number;
    size?: number;
    sort?: string;
    keyword?: string;
    category?: string;
    level?: string;
  }): Observable<PageResponse<CourseSummary>> {
    return this.api.get<PageResponse<CourseSummary>>('/courses', { params });
  }

  getCourseById(id: number): Observable<CourseDetailResponse> {
    return this.api.get<CourseDetailResponse>(`/courses/${id}`);
  }

  getCourse(id: number): Observable<CourseDetailResponse> {
    return this.getCourseById(id);
  }

  /**
   * Debounced search helper.
   * Accepts either a string (single search) or an observable stream of keywords.
   */
  searchCourses(keyword: string | Observable<string>): Observable<CourseSummary[]> {
    const keyword$ = typeof keyword === 'string' ? of(keyword) : keyword;
    return keyword$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((k: string) =>
        this.getCourses(0, 10, 'createdAt,desc', {
          keyword: k && k.trim().length > 0 ? k.trim() : undefined
        })
      ),
      map((page: PageResponse<CourseSummary>) => page.content)
    );
  }

  createCourse(courseData: Partial<CourseSummary> & Record<string, unknown>): Observable<CourseSummary> {
    return this.api.post<CourseSummary>('/courses', courseData);
  }

  updateCourse(id: number, courseData: Partial<CourseSummary> & Record<string, unknown>): Observable<CourseSummary> {
    return this.api.put<CourseSummary>(`/courses/${id}`, courseData);
  }

  publishCourse(id: number): Observable<CourseSummary> {
    return this.api.post<CourseSummary>(`/courses/${id}/publish`, {});
  }

  /**
   * Module management (Instructor/Admin).
   * Backend endpoints are provided by Course-Service and routed via the gateway.
   */
  createModule(courseId: number, title: string): Observable<Module> {
    return this.api.post<Module>(`/courses/${courseId}/modules`, { title });
  }

  deleteModule(moduleId: number): Observable<void> {
    return this.api.delete<void>(`/courses/modules/${moduleId}`);
  }

  deleteCourse(id: number): Observable<void> {
    return this.api.delete<void>(`/courses/${id}`);
  }

  /**
   * Instructor dashboard listing.
   * Backend endpoint (Course-Service): GET /api/courses/instructors/{instructorId}/courses
   */
  getInstructorCourses(
    instructorId: number,
    page = 0,
    size = 10,
    sort = 'createdAt,desc',
    status?: string
  ): Observable<PageResponse<CourseSummary>> {
    return this.api.get<PageResponse<CourseSummary>>(`/courses/instructors/${instructorId}/courses`, {
      params: { page, size, sort, status }
    });
  }
}
