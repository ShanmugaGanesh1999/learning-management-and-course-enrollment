package com.skilltrack.enrollment.exception;

/**
 * Thrown when the caller is authenticated but not allowed to perform an action.
 *
 * Returned as HTTP 403.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
