package com.skilltrack.enrollment.repository;

import com.skilltrack.enrollment.model.Enrollment;
import com.skilltrack.enrollment.model.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Enrollment repository.
 *
 * Performance notes:
 * - All list endpoints should use pagination.
 * - Indexed columns: student_id, course_id, status, completed_at.
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Gets a student's enrollments (used for "My Courses").
     */
    Page<Enrollment> findByStudentId(Long studentId, Pageable pageable);

    /**
     * Checks if a student is enrolled in a course (uniqueness is enforced at DB level too).
     */
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /**
     * Lists enrollments for a course filtered by status (e.g., instructor dashboard).
     */
    Page<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status, Pageable pageable);

    /**
     * Lists enrollments for a course.
     */
    Page<Enrollment> findByCourseId(Long courseId, Pageable pageable);

    /**
     * Lists a student's enrollments by status.
     */
    Page<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status, Pageable pageable);

    /**
     * Counts total enrollments for a course.
     */
    long countByCourseId(Long courseId);

    /**
     * Counts enrollments for a course by a given status.
     */
    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    /**
     * Counts a student's enrollments by a given status (e.g., completed course count).
     */
    long countByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    /**
     * Average progress for a course.
     */
    @Query("SELECT COALESCE(AVG(e.progressPercentage), 0) FROM Enrollment e WHERE e.courseId = :courseId")
    double averageProgressByCourseId(@Param("courseId") Long courseId);

    /**
     * Analytics helper: returns all active and completed enrollments for a student.
     *
     * Notes:
     * - IN_PROGRESS and COMPLETED are treated as "active learning" states.
     * - Ordered by enrolledAt descending for recency.
     */
    @Query("SELECT e FROM Enrollment e " +
            "WHERE e.studentId = :studentId " +
            "AND e.status IN ('IN_PROGRESS', 'COMPLETED') " +
            "ORDER BY e.enrolledAt DESC")
    List<Enrollment> findActiveAndCompletedEnrollments(@Param("studentId") Long studentId);
}
