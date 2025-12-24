package com.skilltrack.course.util;

/**
 * Small validation helpers used by services.
 */
public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
