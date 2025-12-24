package com.skilltrack.course.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload for a module.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleResponse {

    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer orderIndex;
    private LocalDateTime createdAt;

    /**
     * Summary count to avoid always embedding full lessons.
     */
    private int lessonCount;

    /**
     * Present for detail responses.
     */
    private List<LessonResponse> lessons;
}
