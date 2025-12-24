package com.skilltrack.enrollment.model.dto;

import com.skilltrack.enrollment.model.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Denormalized response for "My Courses" view.
 *
 * This is intentionally a convenience shape for the UI and may include fields
 * looked up from other services (e.g., courseTitle/instructorId).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyCoursesResponse {

    private Long enrollmentId;
    private Long courseId;
    private String courseTitle;
    private Long instructorId;
    private Integer progressPercentage;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;
}
