export type EnrollmentStatus = 'ENROLLED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface EnrollmentResponse {
  id: number;
  courseId: number;
  studentId: number;
  /** Best-effort: may be absent depending on backend response shape. */
  studentName?: string | null;
  status: EnrollmentStatus;
  progressPercentage: number;
  enrolledAt?: string;
  /** Best-effort: if backend ever tracks it; otherwise UI falls back to enrolledAt. */
  lastAccessedAt?: string;
  completedAt?: string;
  certificateIssued?: boolean;
}

export interface MyCourseItem {
  enrollmentId: number;
  courseId: number;
  courseTitle: string | null;
  instructorId: number | null;
  progressPercentage: number;
  status: EnrollmentStatus;
  enrolledAt?: string;
}

export interface ProgressUpdateRequest {
  progressPercentage: number;
}

export interface EnrollmentStatsResponse {
  courseId: number;
  totalEnrollments: number;
  enrolled: number;
  inProgress: number;
  completed: number;
  cancelled: number;
  averageProgress: number;
}
