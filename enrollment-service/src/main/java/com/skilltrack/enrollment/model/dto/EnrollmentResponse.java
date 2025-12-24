package com.skilltrack.enrollment.model.dto;

import com.skilltrack.enrollment.model.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload for enrollment.
 *
 * Never exposes sensitive internal fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {

    private Long id;
    private Long courseId;
    private Long studentId;
    private EnrollmentStatus status;
    private Integer progressPercentage;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private Boolean certificateIssued;
}
