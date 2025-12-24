package com.skilltrack.course.service;

import com.skilltrack.course.exception.ForbiddenOperationException;
import com.skilltrack.course.exception.ResourceNotFoundException;
import com.skilltrack.course.model.Course;
import com.skilltrack.course.model.Lesson;
import com.skilltrack.course.model.Module;
import com.skilltrack.course.model.dto.LessonRequest;
import com.skilltrack.course.model.dto.LessonResponse;
import com.skilltrack.course.model.dto.ReorderRequest;
import com.skilltrack.course.repository.LessonRepository;
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
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;

    /**
     * Creates a lesson within a module.
     *
     * Ordering rule:
     * - The server assigns the next {@code orderIndex} to keep lesson ordering consistent.
     */
    @Transactional
    public LessonResponse createLesson(Long moduleId, Long instructorId, LessonRequest request) {
        ValidationUtil.require(moduleId != null, "Missing moduleId");
        ValidationUtil.require(instructorId != null, "Missing instructorId");
        ValidationUtil.require(request != null, "Missing request");

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
        requireOwnership(module.getCourse(), instructorId);

        int nextOrderIndex = lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId).stream()
                .map(Lesson::getOrderIndex)
                .filter(i -> i != null)
                .max(Integer::compareTo)
                .map(i -> i + 1)
                .orElse(0);

        Lesson lesson = Lesson.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .videoUrl(request.getVideoUrl())
                .duration(request.getDuration())
                .orderIndex(nextOrderIndex)
                .module(module)
                .build();

        Lesson saved = lessonRepository.save(lesson);
        log.info("Created lesson: id={} moduleId={} orderIndex={}", saved.getId(), moduleId, saved.getOrderIndex());
        return toLessonResponse(saved);
    }

    @Transactional
    public LessonResponse updateLesson(Long lessonId, Long instructorId, LessonRequest request) {
        ValidationUtil.require(lessonId != null, "Missing lessonId");
        ValidationUtil.require(instructorId != null, "Missing instructorId");
        ValidationUtil.require(request != null, "Missing request");

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        requireOwnership(lesson.getModule().getCourse(), instructorId);

        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getContent());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setDuration(request.getDuration());
        if (request.getOrderIndex() != null) {
            lesson.setOrderIndex(request.getOrderIndex());
        }

        Lesson saved = lessonRepository.save(lesson);
        log.info("Updated lesson: id={} moduleId={} orderIndex={}", saved.getId(), saved.getModuleId(), saved.getOrderIndex());
        return toLessonResponse(saved);
    }

    @Transactional
    public void deleteLesson(Long lessonId, Long instructorId) {
        ValidationUtil.require(lessonId != null, "Missing lessonId");
        ValidationUtil.require(instructorId != null, "Missing instructorId");

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        requireOwnership(lesson.getModule().getCourse(), instructorId);

        lessonRepository.delete(lesson);
        log.info("Deleted lesson: id={} moduleId={}", lessonId, lesson.getModuleId());
    }

    /**
     * Reorders lessons in a module.
     *
     * Transaction note:
     * - This operation updates multiple rows; performing it in a single transaction avoids partial ordering updates.
     */
    @Transactional
    public void reorderLessons(Long moduleId, Long instructorId, List<ReorderRequest> reorderRequests) {
        ValidationUtil.require(moduleId != null, "Missing moduleId");
        ValidationUtil.require(instructorId != null, "Missing instructorId");
        ValidationUtil.require(reorderRequests != null && !reorderRequests.isEmpty(), "Missing reorder requests");

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
        requireOwnership(module.getCourse(), instructorId);

        List<Lesson> lessons = lessonRepository.findByModuleId(moduleId);
        Map<Long, Lesson> lessonById = lessons.stream().collect(Collectors.toMap(Lesson::getId, Function.identity()));

        for (ReorderRequest rr : reorderRequests) {
            Lesson lesson = lessonById.get(rr.getId());
            if (lesson == null) {
                throw new IllegalArgumentException("Lesson does not belong to module");
            }
            lesson.setOrderIndex(rr.getOrderIndex());
        }

        lessonRepository.saveAll(lessons);
        log.info("Reordered lessons: moduleId={} count={}", moduleId, reorderRequests.size());
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsForModule(Long moduleId) {
        ValidationUtil.require(moduleId != null, "Missing moduleId");
        return lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId).stream().map(this::toLessonResponse).toList();
    }

    public LessonResponse toLessonResponse(Lesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .moduleId(lesson.getModuleId())
                .title(lesson.getTitle())
                .content(lesson.getContent())
                .videoUrl(lesson.getVideoUrl())
                .duration(lesson.getDuration())
                .orderIndex(lesson.getOrderIndex())
                .createdAt(lesson.getCreatedAt())
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
