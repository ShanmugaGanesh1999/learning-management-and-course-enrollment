package com.skilltrack.enrollment.exception;

/**
 * Thrown when an upstream service (Auth/Course) is unavailable or returns an unexpected response.
 *
 * Returned as HTTP 503.
 */
public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(String message) {
        super(message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
