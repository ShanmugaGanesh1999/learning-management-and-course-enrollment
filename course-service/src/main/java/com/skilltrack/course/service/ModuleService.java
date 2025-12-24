package com.skilltrack.course.service;

import com.skilltrack.course.exception.ForbiddenOperationException;
import com.skilltrack.course.exception.ResourceNotFoundException;
import com.skilltrack.course.model.Course;
import com.skilltrack.course.model.Lesson;
import com.skilltrack.course.model.Module;
import com.skilltrack.course.model.dto.ModuleRequest;
import com.skilltrack.course.model.dto.LessonResponse;
import com.skilltrack.course.model.dto.ModuleResponse;
import com.skilltrack.course.model.dto.ReorderRequest;
import com.skilltrack.course.repository.CourseRepository;
import com.skilltrack.course.repository.ModuleRepository;
import com.skilltrack.course.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleService {

    private final LessonService lessonService;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;

    /**
     * Creates a module within a course.
     *
     * Ordering rule:
     * - The server assigns the next {@code orderIndex} to keep module ordering consistent.
     */
    @Transactional
    public ModuleResponse createModule(Long courseId, Long instructorId, ModuleRequest request) {
        ValidationUtil.require(courseId != null, "Missing courseId");
        ValidationUtil.require(instructorId != null, "Missing instructorId");
        ValidationUtil.require(request != null, "Missing request");

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        requireOwnership(course, instructorId);

        int nextOrderIndex = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
                .map(Module::getOrderIndex)
                .filter(i -> i != null)
                .max(Integer::compareTo)
                .map(i -> i + 1)
                .orElse(0);

        Module module = Module.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .orderIndex(nextOrderIndex)
                .course(course)
                .build();

        Module saved = moduleRepository.save(module);
        log.info("Created module: id={} courseId={} orderIndex={}", saved.getId(), courseId, saved.getOrderIndex());
        return toModuleResponse(saved, false);
    }

    @Transactional
    public ModuleResponse updateModule(Long moduleId, Long instructorId, ModuleRequest request) {
        ValidationUtil.require(moduleId != null, "Missing moduleId");
        ValidationUtil.require(instructorId != null, "Missing instructorId");
        ValidationUtil.require(request != null, "Missing request");

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
        Course course = module.getCourse();
        requireOwnership(course, instructorId);

        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());
        if (request.getOrderIndex() != null) {
            module.setOrderIndex(request.getOrderIndex());
        }

        Module saved = moduleRepository.save(module);
        log.info("Updated module: id={} courseId={} orderIndex={}", saved.getId(), saved.getCourseId(), saved.getOrderIndex());
        return toModuleResponse(saved, false);
    }

    /**
     * Deletes a module.
     *
     * Cascade impact:
     * - Module -> Lesson is configured with cascade + orphanRemoval, so deleting a module will remove its lessons.
     */
    @Transactional
    public void deleteModule(Long moduleId, Long instructorId) {
        ValidationUtil.require(moduleId != null, "Missing moduleId");
        ValidationUtil.require(instructorId != null, "Missing instructorId");

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
        requireOwnership(module.getCourse(), instructorId);

        moduleRepository.delete(module);
        log.info("Deleted module: id={} courseId={}", moduleId, module.getCourseId());
    }

    /**
     * Reorders modules in a course.
     *
     * Transaction note:
     * - This operation updates multiple rows; performing it in a single transaction avoids partial ordering updates.
     */
    @Transactional
    public void reorderModules(Long courseId, Long instructorId, List<ReorderRequest> reorderRequests) {
        ValidationUtil.require(courseId != null, "Missing courseId");
        ValidationUtil.require(instructorId != null, "Missing instructorId");
        ValidationUtil.require(reorderRequests != null && !reorderRequests.isEmpty(), "Missing reorder requests");

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        requireOwnership(course, instructorId);

        List<Module> modules = moduleRepository.findByCourseId(courseId);
        Map<Long, Module> moduleById = modules.stream().collect(Collectors.toMap(Module::getId, Function.identity()));

        for (ReorderRequest rr : reorderRequests) {
            Module module = moduleById.get(rr.getId());
            if (module == null) {
                throw new IllegalArgumentException("Module does not belong to course");
            }
            module.setOrderIndex(rr.getOrderIndex());
        }

        moduleRepository.saveAll(modules);
        log.info("Reordered modules: courseId={} count={}", courseId, reorderRequests.size());
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> getModulesForCourse(Long courseId, boolean includeLessons) {
        ValidationUtil.require(courseId != null, "Missing courseId");
        List<Module> modules = includeLessons
                ? moduleRepository.findWithLessonsByCourseIdOrderByOrderIndexAsc(courseId)
                : moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        return toModuleResponses(modules, includeLessons);
    }

    public List<ModuleResponse> toModuleResponses(List<Module> modules, boolean includeLessons) {
        if (modules == null) {
            return List.of();
        }
        return modules.stream().map(m -> toModuleResponse(m, includeLessons)).toList();
    }

    public ModuleResponse toModuleResponse(Module module, boolean includeLessons) {
        List<Lesson> lessons = module.getLessons() == null ? List.of() : module.getLessons();
        List<LessonResponse> lessonResponses = includeLessons
                ? lessons.stream().map(lessonService::toLessonResponse).toList()
                : null;

        return ModuleResponse.builder()
                .id(module.getId())
                .courseId(module.getCourseId())
                .title(module.getTitle())
                .description(module.getDescription())
                .orderIndex(module.getOrderIndex())
                .createdAt(module.getCreatedAt())
                .lessonCount(lessons.size())
                .lessons(lessonResponses)
                .build();
    }

    private void requireOwnership(Course course, Long instructorId) {
        if (course == null) {
            throw new ResourceNotFoundException("Course not found");
        }
        if (!course.isBelongsToInstructor(instructorId)) {
            throw new ForbiddenOperationException("You do not own this course");
        }
    }
}
