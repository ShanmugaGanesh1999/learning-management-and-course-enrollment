package com.skilltrack.course.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating/updating a module.
 *
 * Ordering note:
 * - orderIndex controls module ordering within a course.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 10000)
    private String description;

    private Integer orderIndex;
}
