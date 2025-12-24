package com.skilltrack.auth.model;

/**
 * Role definitions for SkillTrack.
 *
 * STUDENT: Can browse courses, enroll, and track progress.
 * INSTRUCTOR: Can create and manage their own courses.
 * ADMIN: Can oversee users and courses across the platform.
 */
public enum Role {
    STUDENT,
    INSTRUCTOR,
    ADMIN
}
