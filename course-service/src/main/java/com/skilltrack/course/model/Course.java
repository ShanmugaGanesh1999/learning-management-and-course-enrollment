package com.skilltrack.course.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Course aggregate root.
 *
 * Represents a single course created by an instructor.
 *
 * Security/audit fields:
 * - createdBy: username that created the course (from JWT subject)
 * - createdAt / updatedAt: timestamps managed by Hibernate
 */
@Entity
@Table(
        name = "courses",
        indexes = {
        @Index(name = "idx_courses_status", columnList = "status"),
                @Index(name = "idx_courses_title", columnList = "title"),
                @Index(name = "idx_courses_category", columnList = "category"),
                @Index(name = "idx_courses_instructor", columnList = "instructor_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Course title displayed in listings.
     */
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String title;

    /**
     * Long-form description (supports large text).
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Course category used for filtering.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    /**
     * Difficulty level displayed to learners.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseLevel level;

    /**
     * Instructor's userId from Auth-Service (ownership boundary across services).
     */
    @NotNull
    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    /**
     * Optional price for paid courses.
     */
    @DecimalMin("0.0")
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Publication status.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;

    /**
     * Modules belonging to this course.
     *
     * Relationship:
     * - One course has many modules.
     * - Cascade delete ensures modules (and their lessons) are removed if the course is deleted.
     */
    @OneToMany(
            mappedBy = "course",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Module> modules = new ArrayList<>();

    /**
     * Audit: created timestamp.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Audit: last update timestamp.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Audit: username who created the course.
     */
    @Column(nullable = false, length = 50)
    private String createdBy;

    /**
     * @return true if the course is visible to students.
     */
    public boolean isPublished() {
        return this.status == CourseStatus.PUBLISHED;
    }

    /**
     * Ownership check used by instructor-only operations.
     */
    public boolean isBelongsToInstructor(Long instructorId) {
        return instructorId != null && instructorId.equals(this.instructorId);
    }
}
