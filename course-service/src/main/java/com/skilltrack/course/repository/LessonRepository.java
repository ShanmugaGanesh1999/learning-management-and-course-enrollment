package com.skilltrack.course.repository;

import com.skilltrack.course.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /**
     * Get lessons in a module.
     * Index usage: benefits from idx_lessons_module_order.
     */
    List<Lesson> findByModuleId(Long moduleId);

    /**
     * Get ordered lessons in a module.
     */
    List<Lesson> findByModuleIdOrderByOrderIndex(Long moduleId);

    List<Lesson> findByModuleIdOrderByOrderIndexAsc(Long moduleId);

    /**
     * Count lessons in a module.
     */
    long countByModuleId(Long moduleId);

    /**
     * Batch lesson counts by course IDs (reduces N+1 when building listings).
     *
     * @return rows of (courseId, lessonCount)
     */
    @Query("SELECT m.course.id, COUNT(l) FROM Lesson l JOIN l.module m WHERE m.course.id IN :courseIds GROUP BY m.course.id")
    List<Object[]> countLessonsByCourseIds(@Param("courseIds") List<Long> courseIds);
}
