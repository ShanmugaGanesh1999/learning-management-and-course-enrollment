package com.skilltrack.course.repository;

import com.skilltrack.course.model.Module;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, Long> {

    /**
     * Get all modules for a course.
     * Index usage: benefits from idx_modules_course_order.
     */
    List<Module> findByCourseId(Long courseId);

    /**
     * Get ordered modules for a course.
     * Ordering uses orderIndex which is indexed with course_id.
     */
    List<Module> findByCourseIdOrderByOrderIndex(Long courseId);

    List<Module> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    /**
     * Fetch ordered modules with their lessons for detail views.
     */
    @EntityGraph(attributePaths = {"lessons"})
    List<Module> findWithLessonsByCourseIdOrderByOrderIndexAsc(Long courseId);

    /**
     * Count modules in a course.
     */
    long countByCourseId(Long courseId);

    /**
     * Batch counts for a set of courses (reduces N+1 when building listings).
     *
     * @return rows of (courseId, moduleCount)
     */
    @Query("SELECT m.course.id, COUNT(m) FROM Module m WHERE m.course.id IN :courseIds GROUP BY m.course.id")
    List<Object[]> countModulesByCourseIds(@Param("courseIds") List<Long> courseIds);
}
