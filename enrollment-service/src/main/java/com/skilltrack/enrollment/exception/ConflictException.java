package com.skilltrack.enrollment.exception;

/**
 * Thrown when the request conflicts with current server state.
 *
 * Returned as HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
