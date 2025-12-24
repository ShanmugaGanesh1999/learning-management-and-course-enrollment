package com.skilltrack.course.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response format.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String message;
    private String path;
    private List<FieldErrorItem> errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldErrorItem {
        private String field;
        private String message;
    }
}
