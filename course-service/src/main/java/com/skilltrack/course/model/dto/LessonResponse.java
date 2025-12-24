package com.skilltrack.course.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload for a lesson.
 *
 * Never includes internal JPA relationships.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {

    private Long id;
    private Long moduleId;
    private String title;
    private String content;
    private String videoUrl;
    private Integer duration;
    private Integer orderIndex;
    private LocalDateTime createdAt;
}
