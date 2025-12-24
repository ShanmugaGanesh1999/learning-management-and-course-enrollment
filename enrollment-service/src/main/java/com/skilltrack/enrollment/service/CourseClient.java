package com.skilltrack.enrollment.service;

import com.skilltrack.enrollment.exception.UpstreamServiceException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Course-Service client.
 *
 * Inter-service communication notes:
 * - Enrollment-Service calls Course-Service to verify course existence and publication status.
 * - We wrap calls in try/catch to avoid leaking low-level client exceptions (basic circuit-breaker style).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseClient {

    private final RestTemplate restTemplate;

    @Value("${services.course.base-url}")
    private String courseServiceBaseUrl;

    public CourseSummary getCoursePublic(Long courseId) {
        try {
            String url = courseServiceBaseUrl + "/" + courseId;
            return restTemplate.getForObject(url, CourseDetailResponse.class).toSummary();
        } catch (HttpClientErrorException ex) {
            // 404/403 etc are treated as "not enrollable" by business logic.
            log.debug("Course lookup failed: courseId={} status={} body={}", courseId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new UpstreamServiceException("Course service unavailable", ex);
        } catch (Exception ex) {
            throw new UpstreamServiceException("Course service error", ex);
        }
    }

    public CourseSummary getCourseAsCaller(Long courseId, String bearerToken) {
        try {
            String url = courseServiceBaseUrl + "/" + courseId;
            HttpHeaders headers = new HttpHeaders();
            if (bearerToken != null && !bearerToken.isBlank()) {
                headers.set("Authorization", bearerToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<CourseDetailResponse> resp = restTemplate.exchange(url, HttpMethod.GET, entity, CourseDetailResponse.class);
            CourseDetailResponse body = resp.getBody();
            if (body == null) {
                throw new UpstreamServiceException("Course service returned empty response");
            }
            return body.toSummary();
        } catch (HttpClientErrorException ex) {
            log.debug("Course lookup (caller) failed: courseId={} status={} body={}", courseId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new UpstreamServiceException("Course service unavailable", ex);
        } catch (Exception ex) {
            throw new UpstreamServiceException("Course service error", ex);
        }
    }

    @Data
    public static class CourseSummary {
        private Long id;
        private String title;
        private Long instructorId;
        private String status;
    }

    @Data
    public static class CourseDetailResponse {
        private CourseResponse course;

        public CourseSummary toSummary() {
            CourseSummary s = new CourseSummary();
            if (course != null) {
                s.setId(course.getId());
                s.setTitle(course.getTitle());
                s.setInstructorId(course.getInstructorId());
                s.setStatus(course.getStatus());
            }
            return s;
        }
    }

    @Data
    public static class CourseResponse {
        private Long id;
        private String title;
        private Long instructorId;
        private String status;
    }
}
