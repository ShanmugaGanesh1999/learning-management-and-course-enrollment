package com.skilltrack.course.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * Request payload for creating/updating a lesson.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 20000)
    private String content;

    @URL
    @Size(max = 500)
    private String videoUrl;

    @NotNull
    private Integer duration;

    private Integer orderIndex;
}
