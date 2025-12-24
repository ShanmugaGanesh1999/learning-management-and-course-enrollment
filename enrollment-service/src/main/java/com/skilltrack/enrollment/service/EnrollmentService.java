package com.skilltrack.enrollment.service;

import com.skilltrack.enrollment.exception.ConflictException;
import com.skilltrack.enrollment.exception.ForbiddenOperationException;
import com.skilltrack.enrollment.exception.ResourceNotFoundException;
import com.skilltrack.enrollment.model.Enrollment;
import com.skilltrack.enrollment.model.EnrollmentStatus;
import com.skilltrack.enrollment.model.dto.EnrollmentResponse;
import com.skilltrack.enrollment.model.dto.EnrollmentStatsResponse;
import com.skilltrack.enrollment.model.dto.MyCoursesResponse;
import com.skilltrack.enrollment.model.dto.PageResponse;
import com.skilltrack.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enrollment management business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseClient courseClient;

    /**
     * Enrolls a student into a course.
     *
     * Inter-service communication:
     * - Calls Course-Service to verify the course exists and is PUBLISHED.
     * - Calls to Auth-Service are optional here because studentId is sourced from a validated JWT.
     *
     * Circuit-breaker hint:
     * - The CourseClient wraps calls in try/catch and throws a controlled exception if the upstream is down.
     */
    @Transactional
    public EnrollmentResponse enrollStudent(Long studentId, Long courseId) {
        require(studentId != null, "Missing studentId");
        require(courseId != null, "Missing courseId");

        enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .ifPresent(e -> {
                    throw new ConflictException("Already enrolled");
                });

        // Verify course exists and is published.
        CourseClient.CourseSummary course;
        try {
            course = courseClient.getCoursePublic(courseId);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Course not found");
            }
            throw new ConflictException("Course is not available for enrollment");
        }

        if (course.getId() == null) {
            throw new ResourceNotFoundException("Course not found");
        }

        if (course.getStatus() == null || !course.getStatus().equalsIgnoreCase("PUBLISHED")) {
            throw new ConflictException("Course is not published");
        }

        Enrollment enrollment = Enrollment.builder()
                .studentId(studentId)
                .courseId(courseId)
                .status(EnrollmentStatus.ENROLLED)
                .progressPercentage(0)
                .certificateIssued(false)
                .build();

        try {
            Enrollment saved = enrollmentRepository.save(enrollment);
            log.info("Student enrolled: enrollmentId={} studentId={} courseId={}", saved.getId(), studentId, courseId);
            return toEnrollmentResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // Handles race conditions against the unique constraint (studentId, courseId).
            throw new ConflictException("Already enrolled");
        }
    }

    /**
     * Updates progress for an enrollment.
     *
     * Automatic completion:
     * - When progress reaches 100, the enrollment transitions to COMPLETED and completedAt is set.
     * - Certificate issuing is represented as a boolean flag in this learning project.
     */
    @Transactional
    public EnrollmentResponse updateProgress(Long enrollmentId, Long studentId, int progressPercentage) {
        require(enrollmentId != null, "Missing enrollmentId");
        require(studentId != null, "Missing studentId");
        require(progressPercentage >= 0 && progressPercentage <= 100, "Progress must be 0-100");

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!studentId.equals(enrollment.getStudentId())) {
            throw new ForbiddenOperationException("You do not own this enrollment");
        }

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new ConflictException("Enrollment is cancelled");
        }

        enrollment.setProgressPercentage(progressPercentage);
        enrollment.setLastAccessedAt(LocalDateTime.now());

        if (progressPercentage >= 100) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(LocalDateTime.now());
        } else if (progressPercentage > 0 && enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
            enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
        }

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Progress updated: enrollmentId={} studentId={} progress={} status={}", enrollmentId, studentId, progressPercentage, saved.getStatus());
        return toEnrollmentResponse(saved);
    }

    /**
     * Student dashboard: get a student's enrollments.
     *
     * Denormalization strategy:
     * - Enrollment only stores courseId; for UI convenience we fetch courseTitle/instructorId from Course-Service.
     * - This is best-effort (failure to enrich does not fail the request).
     */
    @Transactional(readOnly = true)
    public PageResponse<MyCoursesResponse> getMyEnrollments(Long studentId, EnrollmentStatus status, Pageable pageable) {
        require(studentId != null, "Missing studentId");

        Page<Enrollment> page = status == null
                ? enrollmentRepository.findByStudentId(studentId, pageable)
                : enrollmentRepository.findByStudentIdAndStatus(studentId, status, pageable);

        List<MyCoursesResponse> content = page.getContent().stream().map(e -> {
            CourseClient.CourseSummary course = null;
            try {
                course = courseClient.getCoursePublic(e.getCourseId());
            } catch (Exception ex) {
                log.debug("Course enrichment failed: courseId={} message={}", e.getCourseId(), ex.getMessage());
            }

            return MyCoursesResponse.builder()
                    .enrollmentId(e.getId())
                    .courseId(e.getCourseId())
                    .courseTitle(course != null ? course.getTitle() : null)
                    .instructorId(course != null ? course.getInstructorId() : null)
                    .progressPercentage(e.getProgressPercentage())
                    .status(e.getStatus())
                    .enrolledAt(e.getEnrolledAt())
                    .build();
        }).toList();

        return PageResponse.<MyCoursesResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * Instructor analytics view: who enrolled in a course.
     *
     * Ownership check:
     * - Enrollment-Service validates ownership by calling Course-Service with the instructor's Bearer token.
     */
    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> getCourseEnrollments(Long courseId, Long callerUserId, String callerRole, String bearerToken, EnrollmentStatus status, Pageable pageable) {
        require(courseId != null, "Missing courseId");
        require(callerUserId != null, "Missing instructorId");

        boolean isAdmin = callerRole != null && callerRole.equalsIgnoreCase("ADMIN");

        if (!isAdmin) {
            CourseClient.CourseSummary course;
            try {
                course = courseClient.getCourseAsCaller(courseId, bearerToken);
            } catch (HttpClientErrorException ex) {
                throw new ForbiddenOperationException("Unable to verify course ownership");
            }

            if (course.getInstructorId() == null || !course.getInstructorId().equals(callerUserId)) {
                throw new ForbiddenOperationException("You do not own this course");
            }
        }

        Page<Enrollment> page = status == null
            ? enrollmentRepository.findByCourseId(courseId, pageable)
            : enrollmentRepository.findByCourseIdAndStatus(courseId, status, pageable);

        List<EnrollmentResponse> content = page.getContent().stream().map(this::toEnrollmentResponse).toList();

        return PageResponse.<EnrollmentResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public void cancelEnrollment(Long enrollmentId, Long studentId) {
        require(enrollmentId != null, "Missing enrollmentId");
        require(studentId != null, "Missing studentId");

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!studentId.equals(enrollment.getStudentId())) {
            throw new ForbiddenOperationException("You do not own this enrollment");
        }

        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new ConflictException("Completed enrollments cannot be cancelled");
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Enrollment cancelled: enrollmentId={} studentId={} courseId={} status={}", enrollmentId, studentId, saved.getCourseId(), saved.getStatus());

        // Refund/cleanup placeholder:
        // - In a real system, cancellation might trigger refunds or access revocation workflows.
    }

    @Transactional(readOnly = true)
    public EnrollmentStatsResponse getEnrollmentStats(Long courseId, Long callerUserId, String callerRole, String bearerToken) {
        require(courseId != null, "Missing courseId");
        require(callerUserId != null, "Missing instructorId");

        boolean isAdmin = callerRole != null && callerRole.equalsIgnoreCase("ADMIN");

        if (!isAdmin) {
            CourseClient.CourseSummary course;
            try {
                course = courseClient.getCourseAsCaller(courseId, bearerToken);
            } catch (HttpClientErrorException ex) {
                throw new ForbiddenOperationException("Unable to verify course ownership");
            }

            if (course.getInstructorId() == null || !course.getInstructorId().equals(callerUserId)) {
                throw new ForbiddenOperationException("You do not own this course");
            }
        }

        long total = enrollmentRepository.countByCourseId(courseId);
        long enrolled = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ENROLLED);
        long inProgress = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.IN_PROGRESS);
        long completed = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.COMPLETED);
        long cancelled = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.CANCELLED);
        double avg = enrollmentRepository.averageProgressByCourseId(courseId);

        return EnrollmentStatsResponse.builder()
                .courseId(courseId)
                .totalEnrollments(total)
                .enrolled(enrolled)
                .inProgress(inProgress)
                .completed(completed)
                .cancelled(cancelled)
                .averageProgress(avg)
                .build();
    }

    /**
     * Issues a certificate for a completed enrollment.
     */
    @Transactional
    public EnrollmentResponse issueCertificate(Long enrollmentId, Long studentId) {
        require(enrollmentId != null, "Missing enrollmentId");
        require(studentId != null, "Missing studentId");

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!studentId.equals(enrollment.getStudentId())) {
            throw new ForbiddenOperationException("You do not own this enrollment");
        }

        if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            throw new ConflictException("Enrollment is not completed");
        }

        if (Boolean.TRUE.equals(enrollment.getCertificateIssued())) {
            throw new ConflictException("Certificate already issued");
        }

        enrollment.setCertificateIssued(true);
        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Certificate issued: enrollmentId={} studentId={} courseId={}", enrollmentId, studentId, saved.getCourseId());
        return toEnrollmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollment(Long enrollmentId, Long callerUserId, String callerRole, String bearerToken) {
        require(enrollmentId != null, "Missing enrollmentId");
        require(callerUserId != null, "Missing caller userId");

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        boolean isAdmin = callerRole != null && callerRole.equalsIgnoreCase("ADMIN");
        boolean isInstructor = callerRole != null && callerRole.equalsIgnoreCase("INSTRUCTOR");

        if (isAdmin) {
            return toEnrollmentResponse(enrollment);
        }

        if (!isInstructor) {
            // STUDENT: must own enrollment
            if (!callerUserId.equals(enrollment.getStudentId())) {
                throw new ForbiddenOperationException("You do not have access to this enrollment");
            }
            return toEnrollmentResponse(enrollment);
        }

        // INSTRUCTOR: must own the course
        CourseClient.CourseSummary course;
        try {
            course = courseClient.getCourseAsCaller(enrollment.getCourseId(), bearerToken);
        } catch (HttpClientErrorException ex) {
            throw new ForbiddenOperationException("Unable to verify course ownership");
        }

        if (course.getInstructorId() == null || !course.getInstructorId().equals(callerUserId)) {
            throw new ForbiddenOperationException("You do not have access to this enrollment");
        }

        return toEnrollmentResponse(enrollment);
    }

    private EnrollmentResponse toEnrollmentResponse(Enrollment e) {
        return EnrollmentResponse.builder()
                .id(e.getId())
                .courseId(e.getCourseId())
                .studentId(e.getStudentId())
                .status(e.getStatus())
                .progressPercentage(e.getProgressPercentage())
                .enrolledAt(e.getEnrolledAt())
                .completedAt(e.getCompletedAt())
                .certificateIssued(e.getCertificateIssued())
                .build();
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
