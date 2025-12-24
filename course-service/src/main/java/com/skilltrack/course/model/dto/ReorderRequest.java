package com.skilltrack.course.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reorder payload for modules/lessons.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderRequest {

    @NotNull
    private Long id;

    @NotNull
    private Integer orderIndex;
}
