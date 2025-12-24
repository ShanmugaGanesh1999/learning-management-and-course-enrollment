package com.skilltrack.enrollment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Enrollment links a student (Auth-Service userId) to a course (Course-Service courseId).
 *
 * Audit fields:
 * - enrolledAt: when the enrollment was created
 * - lastAccessedAt: last time the student accessed the course (can be updated by later prompts)
 * - completedAt: set when the enrollment reaches COMPLETED
 *
 * Status lifecycle:
 * - ENROLLED: newly enrolled, progress typically 0
 * - IN_PROGRESS: student started learning
 * - COMPLETED: student finished (usually progress 100)
 * - CANCELLED: student dropped out
 */
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
                // Composite uniqueness: a student can only enroll once per course.
                @UniqueConstraint(name = "uk_enrollments_student_course", columnNames = {"student_id", "course_id"})
        },
        indexes = {
                @Index(name = "idx_enrollments_student", columnList = "student_id"),
                @Index(name = "idx_enrollments_course", columnList = "course_id"),
                @Index(name = "idx_enrollments_status", columnList = "status"),
                @Index(name = "idx_enrollments_completed_at", columnList = "completed_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student userId from Auth-Service.
     */
    @NotNull
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * Course id from Course-Service.
     */
    @NotNull
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /**
     * When the enrollment was created.
     */
    @CreationTimestamp
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    /**
     * Progress percentage (0-100).
     */
    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "progress_percentage", nullable = false)
    private Integer progressPercentage;

    /**
     * Current status of the enrollment.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    /**
     * Timestamp when the course was completed.
     * Nullable because an enrollment may never complete.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Whether a completion certificate has been issued.
     */
    @NotNull
    @Builder.Default
    @Column(name = "certificate_issued", nullable = false)
    private Boolean certificateIssued = false;

    /**
     * Last time the student accessed the course.
     */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    public boolean isCompleted() {
        return this.status == EnrollmentStatus.COMPLETED;
    }

    /**
     * Friendly progress description for UI/debugging.
     */
    public String getProgressDescription() {
        int progress = this.progressPercentage == null ? 0 : this.progressPercentage;
        return progress + "%" + " (" + (this.status == null ? "UNKNOWN" : this.status.name()) + ")";
    }
}
