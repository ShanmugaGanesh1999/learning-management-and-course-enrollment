package com.skilltrack.course.controller;

import com.skilltrack.course.config.JwtAuthenticationFilter.JwtUserPrincipal;
import com.skilltrack.course.exception.ForbiddenOperationException;
import com.skilltrack.course.model.CourseLevel;
import com.skilltrack.course.model.CourseStatus;
import com.skilltrack.course.model.dto.CourseDetailResponse;
import com.skilltrack.course.model.dto.CourseRequest;
import com.skilltrack.course.model.dto.CourseResponse;
import com.skilltrack.course.model.dto.LessonRequest;
import com.skilltrack.course.model.dto.LessonResponse;
import com.skilltrack.course.model.dto.ModuleRequest;
import com.skilltrack.course.model.dto.ModuleResponse;
import com.skilltrack.course.model.dto.PageResponse;
import com.skilltrack.course.model.dto.ReorderRequest;
import com.skilltrack.course.service.CourseService;
import com.skilltrack.course.service.LessonService;
import com.skilltrack.course.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Course endpoints.
 *
 * Note: The base path is served under server.servlet.context-path (/api/courses).
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course listing and management")
public class CourseController {

    private final CourseService courseService;
    private final ModuleService moduleService;
    private final LessonService lessonService;

    @GetMapping
    @Operation(
            summary = "List published courses",
            description = "Public listing of PUBLISHED courses with optional filtering/searching."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of courses")
    })
    public ResponseEntity<PageResponse<CourseResponse>> list(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort in the form 'field,asc|desc'", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "Keyword search applied to title+description")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Category enum name (e.g., PROGRAMMING)")
            @RequestParam(required = false) String category,
            @Parameter(description = "Course level")
            @RequestParam(required = false) CourseLevel level
    ) {
        // Pagination defaults (as required): page=0, size=10, sort=createdAt,desc
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), parseSort(sort));
        return ResponseEntity.ok(courseService.searchCourses(keyword, category, level, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get course",
            description = "Returns course detail with modules and lessons. Non-admins can only view PUBLISHED courses unless they are the owning instructor."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course detail"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<CourseDetailResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseDetail(id));
    }

    @GetMapping("/{id}/detail")
    @Operation(
            summary = "Get course detail (optimized)",
            description = "Returns CourseDetailResponse with nested modules/lessons using an EntityGraph to avoid N+1 queries."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course detail"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<CourseDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(
            summary = "Create course",
            description = "Creates a new course owned by the authenticated instructor/admin. New courses start as DRAFT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        JwtUserPrincipal jwt = requireJwtPrincipal();

        // Course creation flow:
        // 1) instructorId comes from the JWT
        // 2) service creates a DRAFT course
        CourseResponse created = courseService.createCourse(jwt.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Update course", description = "Updates a course owned by the authenticated instructor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<CourseResponse> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        CourseResponse updated = courseService.updateCourse(id, jwt.userId(), request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Delete course", description = "Deletes a course owned by the authenticated instructor.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        courseService.deleteCourse(id, jwt.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Publish course", description = "Publishes a course owned by the authenticated instructor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Published"),
            @ApiResponse(responseCode = "400", description = "Invalid state"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<CourseResponse> publish(@PathVariable Long id) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        CourseResponse published = courseService.publishCourse(id, jwt.userId());
        return ResponseEntity.ok(published);
    }

    @GetMapping("/instructors/{instructorId}/courses")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(
            summary = "List instructor courses",
            description = "Instructor dashboard listing (includes DRAFT and PUBLISHED). Instructors can only view their own courses."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of instructor courses"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<PageResponse<CourseResponse>> instructorCourses(
            @PathVariable Long instructorId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) CourseStatus status
    ) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        if (!isAdmin(jwt) && !instructorId.equals(jwt.userId())) {
            throw new ForbiddenOperationException("You can only view your own courses");
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), parseSort(sort));
        return ResponseEntity.ok(courseService.getInstructorCourses(instructorId, status, pageable));
    }

    @PostMapping("/{id}/modules")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Create module", description = "Creates a module inside a course owned by the authenticated instructor.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ModuleResponse> createModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        ModuleResponse created = moduleService.createModule(id, jwt.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/modules")
    @Operation(
            summary = "Get modules",
            description = "Returns ordered modules for a course and populates lessons for each module. Visibility follows course rules (students only see PUBLISHED)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Module list"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<ModuleResponse>> getModules(@PathVariable Long id) {
        // Enforce course visibility before returning outline.
        courseService.getCourse(id, false);
        return ResponseEntity.ok(moduleService.getModulesForCourse(id, true));
    }

    @PutMapping("/modules/{moduleId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Update module", description = "Updates a module owned by the authenticated instructor via course ownership.")
    public ResponseEntity<ModuleResponse> updateModule(@PathVariable Long moduleId, @Valid @RequestBody ModuleRequest request) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        return ResponseEntity.ok(moduleService.updateModule(moduleId, jwt.userId(), request));
    }

    @DeleteMapping("/modules/{moduleId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Delete module", description = "Deletes a module (and cascades to delete its lessons).")
    public ResponseEntity<Void> deleteModule(@PathVariable Long moduleId) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        moduleService.deleteModule(moduleId, jwt.userId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/modules/reorder")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Reorder modules", description = "Reorders modules for a course in a single transaction.")
    public ResponseEntity<Void> reorderModules(@PathVariable Long id, @Valid @RequestBody List<ReorderRequest> request) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        moduleService.reorderModules(id, jwt.userId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/modules/{moduleId}/lessons")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Create lesson", description = "Creates a lesson inside a module owned by the authenticated instructor.")
    public ResponseEntity<LessonResponse> createLesson(@PathVariable Long moduleId, @Valid @RequestBody LessonRequest request) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        LessonResponse created = lessonService.createLesson(moduleId, jwt.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Update lesson", description = "Updates a lesson owned by the authenticated instructor via course ownership.")
    public ResponseEntity<LessonResponse> updateLesson(@PathVariable Long lessonId, @Valid @RequestBody LessonRequest request) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        return ResponseEntity.ok(lessonService.updateLesson(lessonId, jwt.userId(), request));
    }

    @DeleteMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Delete lesson", description = "Deletes a lesson.")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long lessonId) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        lessonService.deleteLesson(lessonId, jwt.userId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/modules/{moduleId}/lessons/reorder")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Reorder lessons", description = "Reorders lessons for a module in a single transaction.")
    public ResponseEntity<Void> reorderLessons(@PathVariable Long moduleId, @Valid @RequestBody List<ReorderRequest> request) {
        JwtUserPrincipal jwt = requireJwtPrincipal();
        lessonService.reorderLessons(moduleId, jwt.userId(), request);
        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",", 2);
        String property = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "desc";

        // Whitelist sortable properties.
        if (!property.equals("createdAt")
                && !property.equals("updatedAt")
                && !property.equals("title")
                && !property.equals("category")
                && !property.equals("level")
                && !property.equals("status")) {
            property = "createdAt";
        }

        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, property);
    }

    private JwtUserPrincipal requireJwtPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (!(principal instanceof JwtUserPrincipal jwt)) {
            throw new ForbiddenOperationException("Not authenticated");
        }
        return jwt;
    }

    private boolean isAdmin(JwtUserPrincipal jwt) {
        return jwt.role() != null && jwt.role().equalsIgnoreCase("ADMIN");
    }
}
