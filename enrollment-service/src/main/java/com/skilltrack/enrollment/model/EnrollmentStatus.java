package com.skilltrack.enrollment.model;

/**
 * Enrollment lifecycle status.
 *
 * Typical transitions:
 * - ENROLLED -> IN_PROGRESS when the student starts learning
 * - IN_PROGRESS -> COMPLETED when progress reaches 100%
 * - (ENROLLED|IN_PROGRESS) -> CANCELLED if the student drops the course
 */
public enum EnrollmentStatus {

    /**
     * Just enrolled and not started yet.
     */
    ENROLLED,

    /**
     * Student started learning.
     */
    IN_PROGRESS,

    /**
     * Student completed the course.
     */
    COMPLETED,

    /**
     * Student cancelled/dropped the enrollment.
     */
    CANCELLED
}
