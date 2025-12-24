package com.skilltrack.course.service;

import com.skilltrack.course.config.JwtAuthenticationFilter.JwtUserPrincipal;
import com.skilltrack.course.exception.ForbiddenOperationException;
import com.skilltrack.course.exception.ResourceNotFoundException;
import com.skilltrack.course.model.Category;
import com.skilltrack.course.model.Course;
import com.skilltrack.course.model.CourseLevel;
import com.skilltrack.course.model.CourseStatus;
import com.skilltrack.course.model.dto.CourseDetailResponse;
import com.skilltrack.course.model.dto.CourseRequest;
import com.skilltrack.course.model.dto.CourseResponse;
import com.skilltrack.course.model.dto.ModuleResponse;
import com.skilltrack.course.model.dto.PageResponse;
import com.skilltrack.course.repository.CourseRepository;
import com.skilltrack.course.repository.LessonRepository;
import com.skilltrack.course.repository.ModuleRepository;
import com.skilltrack.course.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final ModuleService moduleService;
        private final ModuleRepository moduleRepository;
        private final LessonRepository lessonRepository;

    /**
     * Creates a course owned by the given instructor.
     *
     * Security:
     * - instructorId and createdBy must come from a validated JWT (not from client input).
     */
    public CourseResponse createCourse(CourseRequest request, Long instructorId, String createdBy) {
                return createCourse(instructorId, request, createdBy);
    }

        /**
         * Creates a course owned by the given instructor.
         *
         * Business rule:
         * - New courses start in {@link CourseStatus#DRAFT} so instructors can build out
         *   modules/lessons before publishing to students.
         */
        @Transactional
        public CourseResponse createCourse(Long instructorId, CourseRequest request) {
                JwtUserPrincipal principal = requirePrincipal();
                return createCourse(instructorId, request, principal.username());
        }

        @Transactional
        public CourseResponse createCourse(Long instructorId, CourseRequest request, String createdBy) {
                ValidationUtil.require(instructorId != null, "Missing instructorId");
                ValidationUtil.require(createdBy != null && !createdBy.isBlank(), "Missing createdBy");
                ValidationUtil.require(request != null, "Missing request");

                Course course = Course.builder()
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .category(request.getCategory())
                                .level(request.getLevel())
                                .price(request.getPrice())
                                .instructorId(instructorId)
                                .createdBy(createdBy)
                                .status(CourseStatus.DRAFT)
                                .build();

                Course saved = courseRepository.save(course);
                log.info("Created course: id={} instructorId={} status={}", saved.getId(), instructorId, saved.getStatus());
                return toCourseResponse(saved, List.of(), 0, 0);
        }

        /**
         * Updates an existing course.
         *
         * Ownership rule:
         * - Only the owning instructor can modify their course.
         * - We compare the caller's instructorId (from JWT) with the course.instructorId.
         */
        @Transactional
        public CourseResponse updateCourse(Long courseId, Long instructorId, CourseRequest request) {
                ValidationUtil.require(courseId != null, "Missing courseId");
                ValidationUtil.require(instructorId != null, "Missing instructorId");
                ValidationUtil.require(request != null, "Missing request");

                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
                requireOwnership(course, instructorId);

                course.setTitle(request.getTitle());
                course.setDescription(request.getDescription());
                course.setCategory(request.getCategory());
                course.setLevel(request.getLevel());
                course.setPrice(request.getPrice());
                // createdAt is managed by Hibernate and is not modified here.

                Course saved = courseRepository.save(course);
                log.info("Updated course: id={} instructorId={} status={}", saved.getId(), instructorId, saved.getStatus());
                return toCourseResponse(saved, List.of(), 0, 0);
        }

        /**
         * Publishes a course so it becomes visible to students.
         *
         * Publication workflow:
         * - Verify ownership.
         * - (Optional business rule) Require at least one module that contains at least one lesson.
         * - Set status to {@link CourseStatus#PUBLISHED}.
         */
        @Transactional
        public CourseResponse publishCourse(Long courseId, Long instructorId) {
                ValidationUtil.require(courseId != null, "Missing courseId");
                ValidationUtil.require(instructorId != null, "Missing instructorId");

                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
                requireOwnership(course, instructorId);

                boolean hasModuleWithLesson = moduleRepository.findByCourseId(courseId).stream()
                                .anyMatch(m -> lessonRepository.countByModuleId(m.getId()) > 0);
                ValidationUtil.require(hasModuleWithLesson, "Course must have at least one module with at least one lesson before publishing");

                course.setStatus(CourseStatus.PUBLISHED);
                Course saved = courseRepository.save(course);
                log.info("Published course: id={} instructorId={}", saved.getId(), instructorId);
                return toCourseResponse(saved, List.of(), 0, 0);
        }

        /**
         * Gets a course with optional module/lesson inclusion.
         *
         * Visibility rules:
         * - Anonymous callers and STUDENT callers can only see {@link CourseStatus#PUBLISHED} courses.
         * - The owning instructor can see their own DRAFT courses.
         */
        @Transactional(readOnly = true)
        public Object getCourse(Long courseId, boolean includeModules) {
                ValidationUtil.require(courseId != null, "Missing courseId");
                JwtUserPrincipal principal = optionalPrincipal().orElse(null);

                Course course = includeModules
                                ? courseRepository.findWithModulesAndLessonsById(courseId).orElse(null)
                                : courseRepository.findById(courseId).orElse(null);

                if (course == null) {
                        throw new ResourceNotFoundException("Course not found");
                }

                enforceVisibility(course, principal);

                if (!includeModules) {
                        return toCourseResponse(course, List.of(), 0, 0);
                }

                List<ModuleResponse> outline = moduleService.toModuleResponses(course.getModules(), false);
                List<ModuleResponse> modules = moduleService.toModuleResponses(course.getModules(), true);
                CourseResponse courseResponse = toCourseResponse(course, outline, modules.size(), courseResponseLessonCount(modules));
                return CourseDetailResponse.builder().course(courseResponse).modules(modules).build();
        }

        /**
         * Deletes a course.
         *
         * Cascade impact:
         * - Because Course -> Module -> Lesson is configured with cascade + orphanRemoval,
         *   deleting the course will delete all associated modules and lessons.
         */
        @Transactional
        public void deleteCourse(Long courseId, Long instructorId) {
                ValidationUtil.require(courseId != null, "Missing courseId");
                ValidationUtil.require(instructorId != null, "Missing instructorId");

                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
                requireOwnership(course, instructorId);

                // Enrollment safety check:
                // If/when Enrollment-Service exposes a "count enrollments for course" endpoint,
                // this is the place to block deletion when enrollments exist.

                courseRepository.delete(course);
                log.info("Deleted course: id={} instructorId={}", courseId, instructorId);
        }

        /**
         * Searches published courses (student view) with optional filters.
         *
         * Search implementation:
         * - Uses LIKE predicates on title/description (case-insensitive).
         * - Applies optional category/level filters.
         */
        @Transactional(readOnly = true)
        public PageResponse<CourseResponse> searchCourses(String keyword, String category, CourseLevel level, Pageable pageable) {
                Category parsedCategory = parseCategory(category).orElse(null);
                String normalizedKeyword = keyword == null ? null : keyword.trim();
                if (normalizedKeyword != null && normalizedKeyword.isBlank()) {
                        normalizedKeyword = "";
                }

                Page<Course> page = courseRepository.searchPublishedCoursesFiltered(normalizedKeyword, parsedCategory, level, pageable);
                return toCoursePageResponse(page);
        }

        /**
         * Instructor dashboard view: returns all courses for an instructor, including DRAFT.
         */
        @Transactional(readOnly = true)
        public PageResponse<CourseResponse> getInstructorCourses(Long instructorId, Pageable pageable) {
                ValidationUtil.require(instructorId != null, "Missing instructorId");
                Page<Course> page = courseRepository.findAll(
                                (root, query, cb) -> cb.equal(root.get("instructorId"), instructorId),
                                pageable
                );
                return toCoursePageResponse(page);
        }

        /**
         * Instructor dashboard view: returns courses for an instructor, optionally filtered by status.
         */
        @Transactional(readOnly = true)
        public PageResponse<CourseResponse> getInstructorCourses(Long instructorId, CourseStatus status, Pageable pageable) {
                ValidationUtil.require(instructorId != null, "Missing instructorId");
                Page<Course> page = courseRepository.findAll(
                                (root, query, cb) -> {
                                        if (status == null) {
                                                return cb.equal(root.get("instructorId"), instructorId);
                                        }
                                        return cb.and(
                                                        cb.equal(root.get("instructorId"), instructorId),
                                                        cb.equal(root.get("status"), status)
                                        );
                                },
                                pageable
                );
                return toCoursePageResponse(page);
        }

        /**
         * Student view: returns only published courses.
         *
         * Pagination defaults are enforced at the controller layer.
         */
        @Transactional(readOnly = true)
        public PageResponse<CourseResponse> getPublishedCourses(Pageable pageable) {
                Page<Course> page = courseRepository.findAll(
                                (root, query, cb) -> cb.equal(root.get("status"), CourseStatus.PUBLISHED),
                                pageable
                );
                return toCoursePageResponse(page);
        }

    public PageResponse<CourseResponse> listCourses(Pageable pageable) {
        // Backward-compatible method; use getPublishedCourses/searchCourses for public listings.
        Page<Course> page = courseRepository.findAll(pageable);
        return toCoursePageResponse(page);
    }

    public CourseDetailResponse getCourseDetail(Long id) {
                Course course = courseRepository.findWithModulesAndLessonsById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
                JwtUserPrincipal principal = optionalPrincipal().orElse(null);
                enforceVisibility(course, principal);

                List<ModuleResponse> modules = moduleService.toModuleResponses(course.getModules(), true);
                List<ModuleResponse> outline = moduleService.toModuleResponses(course.getModules(), false);
                CourseResponse courseResponse = toCourseResponse(course, outline, modules.size(), courseResponseLessonCount(modules));
                return CourseDetailResponse.builder().course(courseResponse).modules(modules).build();
    }

    public CourseResponse toCourseResponse(Course course, List<ModuleResponse> modules) {
                return toCourseResponse(course, modules, 0, 0);
    }

        public CourseResponse toCourseResponse(Course course, List<ModuleResponse> modules, int moduleCount, int lessonCount) {
                return CourseResponse.builder()
                                .id(course.getId())
                                .title(course.getTitle())
                                .description(course.getDescription())
                                .category(course.getCategory())
                                .level(course.getLevel())
                                .instructorId(course.getInstructorId())
                                .price(course.getPrice())
                                .status(course.getStatus())
                                .modules(modules)
                                .moduleCount(moduleCount)
                                .lessonCount(lessonCount)
                                .createdAt(course.getCreatedAt())
                                .updatedAt(course.getUpdatedAt())
                                .build();
        }

        private PageResponse<CourseResponse> toCoursePageResponse(Page<Course> page) {
                List<Long> courseIds = page.getContent().stream().map(Course::getId).filter(Objects::nonNull).toList();
                Map<Long, Integer> moduleCounts = moduleRepository.countModulesByCourseIds(courseIds).stream()
                                .collect(Collectors.toMap(
                                                r -> (Long) r[0],
                                                r -> ((Long) r[1]).intValue()
                                ));
                Map<Long, Integer> lessonCounts = lessonRepository.countLessonsByCourseIds(courseIds).stream()
                                .collect(Collectors.toMap(
                                                r -> (Long) r[0],
                                                r -> ((Long) r[1]).intValue()
                                ));

                List<CourseResponse> content = page.getContent().stream()
                                .map(c -> toCourseResponse(c, List.of(), moduleCounts.getOrDefault(c.getId(), 0), lessonCounts.getOrDefault(c.getId(), 0)))
                                .toList();

                return PageResponse.<CourseResponse>builder()
                                .content(content)
                                .pageNumber(page.getNumber())
                                .pageSize(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .first(page.isFirst())
                                .last(page.isLast())
                                .build();
        }

        private void requireOwnership(Course course, Long instructorId) {
                if (!course.isBelongsToInstructor(instructorId)) {
                        throw new ForbiddenOperationException("You do not own this course");
                }
        }

        private void enforceVisibility(Course course, JwtUserPrincipal principal) {
                if (course.getStatus() == CourseStatus.PUBLISHED) {
                        return;
                }

                if (principal == null) {
                        throw new ResourceNotFoundException("Course not found");
                }

                boolean isAdmin = principal.role() != null && principal.role().equalsIgnoreCase("ADMIN");
                if (isAdmin || course.isBelongsToInstructor(principal.userId())) {
                        return;
                }

                // Authenticated but not allowed to view the draft.
                throw new ForbiddenOperationException("Course is not published");
        }

        private Optional<Category> parseCategory(String category) {
                if (category == null || category.isBlank()) {
                        return Optional.empty();
                }
                try {
                        return Optional.of(Category.valueOf(category.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException("Invalid category");
                }
        }

        private Optional<JwtUserPrincipal> optionalPrincipal() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                Object principal = authentication != null ? authentication.getPrincipal() : null;
                return principal instanceof JwtUserPrincipal jwt ? Optional.of(jwt) : Optional.empty();
        }

        private JwtUserPrincipal requirePrincipal() {
                return optionalPrincipal().orElseThrow(() -> new ForbiddenOperationException("Not authenticated"));
        }

        private static int courseResponseLessonCount(List<ModuleResponse> modules) {
                if (modules == null) {
                        return 0;
                }
                return modules.stream().mapToInt(m -> m.getLessons() == null ? 0 : m.getLessons().size()).sum();
        }
}
