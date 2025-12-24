package com.skilltrack.course.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * Lesson represents a single learning unit within a module.
 *
 * Video URL format:
 * - When present, videoUrl must be a valid URL (e.g., https://example.com/video.mp4).
 */
@Entity
@Table(
    name = "lessons",
    indexes = {
        @Index(name = "idx_lessons_module_order", columnList = "module_id,order_index")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Convenience FK view (read-only). Use module relationship for writes.
     */
    @Column(name = "module_id", insertable = false, updatable = false)
    private Long moduleId;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @URL
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /**
     * Lesson duration in minutes.
     */
    @NotNull
    @Column(nullable = false)
    private Integer duration;

    @NotNull
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;
}
