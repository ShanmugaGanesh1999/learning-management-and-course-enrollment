package com.skilltrack.enrollment.controller;

import com.skilltrack.enrollment.model.EnrollmentStatus;
import com.skilltrack.enrollment.model.dto.EnrollmentRequest;
import com.skilltrack.enrollment.model.dto.EnrollmentResponse;
import com.skilltrack.enrollment.model.dto.EnrollmentStatsResponse;
import com.skilltrack.enrollment.model.dto.MyCoursesResponse;
import com.skilltrack.enrollment.model.dto.PageResponse;
import com.skilltrack.enrollment.model.dto.ProgressUpdateRequest;
import com.skilltrack.enrollment.config.JwtAuthenticationFilter.JwtUserPrincipal;
import com.skilltrack.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Enrollment and progress management")
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @Operation(summary = "Enroll in a course", description = "Student enrolls into a published course. Duplicate enrollments return 409.")
        @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> enroll(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody EnrollmentRequest request
    ) {
        EnrollmentResponse response = enrollmentService.enrollStudent(principal.userId(), request.getCourseId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{enrollmentId}/progress")
    @Operation(summary = "Update progress", description = "Student updates progress percentage for their enrollment.")
        @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> updateProgress(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long enrollmentId,
            @Valid @RequestBody ProgressUpdateRequest request
    ) {
        EnrollmentResponse response = enrollmentService.updateProgress(enrollmentId, principal.userId(), request.getProgressPercentage());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "My enrollments", description = "Lists the authenticated student's enrollments, optionally filtered by status.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PageResponse<MyCoursesResponse>> myEnrollments(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false) EnrollmentStatus status,
            @Parameter(description = "0-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrolledAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ResponseEntity.ok(enrollmentService.getMyEnrollments(principal.userId(), status, pageable));
    }

    @GetMapping("/courses/{courseId}/enrollments")
    @Operation(summary = "Course enrollments", description = "Instructor/admin lists enrollments for a course. Instructors must own the course.")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ResponseEntity<PageResponse<EnrollmentResponse>> courseEnrollments(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable Long courseId,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrolledAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ResponseEntity.ok(enrollmentService.getCourseEnrollments(courseId, principal.userId(), principal.role(), authorization, status, pageable));
    }

    @GetMapping("/{enrollmentId}")
    @Operation(summary = "Enrollment details", description = "Returns enrollment details. Student must own it; instructor must own the course; admin can view all.")
    public ResponseEntity<EnrollmentResponse> getEnrollment(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable Long enrollmentId
    ) {
        EnrollmentResponse response = enrollmentService.getEnrollment(enrollmentId, principal.userId(), principal.role(), authorization);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{enrollmentId}")
    @Operation(summary = "Cancel enrollment", description = "Student cancels their enrollment.")
        @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long enrollmentId
    ) {
        enrollmentService.cancelEnrollment(enrollmentId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/courses/{courseId}/stats")
    @Operation(summary = "Enrollment stats", description = "Instructor analytics for a course they own.")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ResponseEntity<EnrollmentStatsResponse> stats(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentStats(courseId, principal.userId(), principal.role(), authorization));
    }

    @PostMapping("/{enrollmentId}/certificate")
    @Operation(summary = "Issue certificate", description = "Issues a completion certificate for the student's completed enrollment.")
        @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> certificate(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long enrollmentId
    ) {
        return ResponseEntity.ok(enrollmentService.issueCertificate(enrollmentId, principal.userId()));
    }

    private Sort parseSort(String sort) {
        // format: field,dir
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }
        String[] parts = sort.split(",");
        if (parts.length == 0) {
            return Sort.unsorted();
        }

        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1
                ? Sort.Direction.fromOptionalString(parts[1].trim()).orElse(Sort.Direction.DESC)
                : Sort.Direction.DESC;

        return Sort.by(direction, property);
    }
}
