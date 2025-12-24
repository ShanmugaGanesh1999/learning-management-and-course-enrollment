package com.skilltrack.enrollment.exception;

import com.skilltrack.enrollment.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Centralized exception handler.
 *
 * Security:
 * - Returns meaningful messages without exposing stack traces to clients.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.debug("Validation error: path={} errors={}", request.getRequestURI(), ex.getBindingResult().getErrorCount());
        List<ErrorResponse.FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        log.info("Bad request: path={} message={}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.info("Not found: path={} message={}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler({ForbiddenOperationException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException ex, HttpServletRequest request) {
        log.info("Forbidden: path={} message={}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        log.info("Conflict: path={} message={}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<ErrorResponse> handleUpstream(UpstreamServiceException ex, HttpServletRequest request) {
        log.warn("Upstream error: path={} message={}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error: path={} message={}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request.getRequestURI(), List.of());
    }

    private ErrorResponse.FieldErrorItem toFieldError(FieldError fe) {
        return ErrorResponse.FieldErrorItem.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .build();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path, List<ErrorResponse.FieldErrorItem> errors) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .message(message)
                .path(path)
                .errors(errors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
