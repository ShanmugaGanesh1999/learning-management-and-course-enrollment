package com.skilltrack.course.exception;

/**
 * Thrown when a requested resource does not exist.
 *
 * Returned as HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
