package com.skilltrack.course.model.dto;

import com.skilltrack.course.model.Category;
import com.skilltrack.course.model.CourseLevel;
import com.skilltrack.course.model.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload for a course.
 *
 * Never exposes internal fields like createdBy.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private Category category;
    private CourseLevel level;
    private Long instructorId;
    private BigDecimal price;
    private CourseStatus status;

    /**
     * Included as a lightweight outline (no deep lesson tree by default).
     */
    private List<ModuleResponse> modules;

    private int moduleCount;
    private int lessonCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
