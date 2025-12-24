package com.skilltrack.enrollment.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating an enrollment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentRequest {

    @NotNull
    private Long courseId;
}
