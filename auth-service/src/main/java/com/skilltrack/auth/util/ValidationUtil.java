package com.skilltrack.auth.util;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
