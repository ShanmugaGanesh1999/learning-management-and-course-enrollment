package com.skilltrack.enrollment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enrollment statistics for a course.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentStatsResponse {

    private Long courseId;

    private long totalEnrollments;
    private long enrolled;
    private long inProgress;
    private long completed;
    private long cancelled;

    private double averageProgress;
}
