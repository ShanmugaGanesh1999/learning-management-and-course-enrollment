package com.skilltrack.course.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Extended response with a full modules -> lessons tree.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDetailResponse {

    private CourseResponse course;

    /**
     * Full modules tree with lessons populated.
     */
    private List<ModuleResponse> modules;
}
