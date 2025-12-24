package com.skilltrack.course.repository;

import com.skilltrack.course.model.Category;
import com.skilltrack.course.model.Course;
import com.skilltrack.course.model.CourseLevel;
import com.skilltrack.course.model.CourseStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Course repository.
 *
 * Performance notes:
 * - Listing endpoints should avoid fetching modules/lessons to prevent N+1 query behavior.
 * - For detail endpoints, use {@link EntityGraph} or JOIN FETCH to fetch the course outline in one query.
 */
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    /**
     * Fetch course detail including modules and lessons in a single query (N+1 avoidance).
     */

    @EntityGraph(attributePaths = {"modules", "modules.lessons"})
    Optional<Course> findWithModulesAndLessonsById(Long id);

    /**
     * Get all courses by status (e.g., published-only listings).
     * Index usage: benefits from idx_courses_status.
     */
    List<Course> findByStatus(CourseStatus status);

    /**
     * Get all courses created by a specific instructor.
     * Index usage: benefits from idx_courses_instructor.
     */
    List<Course> findByInstructorId(Long instructorId);

    /**
     * Get an instructor's courses filtered by status.
     */
    List<Course> findByInstructorIdAndStatus(Long instructorId, CourseStatus status);

    /**
     * Get courses in a category filtered by status.
     * Index usage: benefits from idx_courses_category and idx_courses_status.
     */
    List<Course> findByCategoryAndStatus(Category category, CourseStatus status);

    /**
     * Search by title (contains) within a status using pagination.
     * Note: this only searches title; use {@link #searchPublishedCourses(String, Pageable)} for title+description.
     */
    Page<Course> findByStatusAndTitleContainingIgnoreCase(CourseStatus status, String keyword, Pageable pageable);

    /**
     * Full-text-ish search for published courses (title OR description).
     *
     * Performance characteristics:
     * - Uses LIKE predicates; for large datasets consider a FULLTEXT index or external search engine.
     * - Still benefits from idx_courses_status to narrow the candidate set first.
     */
    @Query("SELECT c FROM Course c " +
            "WHERE c.status = 'PUBLISHED' " +
            "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY c.createdAt DESC")
    Page<Course> searchPublishedCourses(@Param("keyword") String keyword, Pageable pageable);

        /**
         * Search/filter published courses with optional category/level filters.
         *
         * Notes:
         * - category/level are optional (pass null to ignore that filter)
         * - keyword is optional; when null/blank, it behaves like "match all" within the published set
         */
        @Query("SELECT c FROM Course c " +
            "WHERE c.status = 'PUBLISHED' " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:level IS NULL OR c.level = :level) " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "     OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))" +
            "ORDER BY c.createdAt DESC")
        Page<Course> searchPublishedCoursesFiltered(
            @Param("keyword") String keyword,
            @Param("category") Category category,
            @Param("level") CourseLevel level,
            Pageable pageable
        );

    /**
     * Advanced filtering by status + category + level.
     */
    Page<Course> findByStatusAndCategoryAndLevel(CourseStatus status, Category category, CourseLevel level, Pageable pageable);
}
