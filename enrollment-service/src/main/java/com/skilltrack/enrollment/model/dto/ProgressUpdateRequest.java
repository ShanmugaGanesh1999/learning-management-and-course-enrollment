package com.skilltrack.enrollment.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating enrollment progress.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressUpdateRequest {

    @NotNull
    @Min(0)
    @Max(100)
    private Integer progressPercentage;
}
