package com.skilltrack.course.model.dto;

import com.skilltrack.course.model.Category;
import com.skilltrack.course.model.CourseLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for creating/updating a course.
 *
 * Security note:
 * - instructorId/createdBy/status are set server-side from the authenticated principal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRequest {

    @NotBlank
    @Size(max = 120)
    private String title;

    @Size(max = 10000)
    private String description;

    @NotNull
    private Category category;

    @NotNull
    private CourseLevel level;

    @DecimalMin("0.0")
    private BigDecimal price;
}
