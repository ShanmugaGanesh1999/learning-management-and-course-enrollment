package com.skilltrack.auth.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {
    private final Instant timestamp;
    private final int status;
    private final String message;
    private final String path;
    private final List<FieldErrorItem> errors;

    @Getter
    @Builder
    public static class FieldErrorItem {
        private final String field;
        private final String message;
    }
}
