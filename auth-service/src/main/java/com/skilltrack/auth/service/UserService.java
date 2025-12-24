package com.skilltrack.auth.service;

import com.skilltrack.auth.exception.BadRequestException;
import com.skilltrack.auth.exception.ResourceNotFoundException;
import com.skilltrack.auth.model.User;
import com.skilltrack.auth.model.dto.ChangePasswordRequest;
import com.skilltrack.auth.model.dto.PageResponse;
import com.skilltrack.auth.model.dto.UpdateUserRequest;
import com.skilltrack.auth.model.dto.UserProfileResponse;
import com.skilltrack.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Loads a user by id.
     *
     * Security considerations:
     * - Callers must enforce authorization (e.g. admin-only or owner-only access).
     * - This method returns a profile DTO (no password).
     */
    public UserProfileResponse getUserById(Long id) {
        return toProfile(requireById(id));
    }

    /**
     * Loads a user by username.
     *
     * Security considerations:
     * - Callers must avoid using this to enumerate users in public endpoints.
     */
    public UserProfileResponse getUserByUsername(String username) {
        return toProfile(requireByUsername(username));
    }

    /**
     * Internal helper that throws a stable 404-style error when the user does not exist.
     */
    public User requireByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Internal helper that throws a stable 404-style error when the user does not exist.
     */
    public User requireById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Updates profile fields (not password).
     *
     * Security considerations:
     * - This method does NOT allow role or password changes.
     * - Callers must verify that the authenticated user is the profile owner or an admin.
     */
    public UserProfileResponse updateUserProfile(Long id, UpdateUserRequest request) {
        User user = requireById(id);

        // Prevent email collisions across accounts.
        userRepository.findByEmailIgnoreCase(request.getEmail())
                .filter(existing -> !Objects.equals(existing.getId(), user.getId()))
                .ifPresent(existing -> {
                    throw new BadRequestException("Email already exists");
                });

        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        User saved = userRepository.save(user);
        return toProfile(saved);
    }

    /**
     * Changes password securely by verifying the current password first.
     *
     * Security considerations:
     * - Requires current password to reduce the impact of a stolen access token.
     * - Never logs passwords or password hashes.
     */
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = requireById(id);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    /**
     * Lists users with pagination and an optional search filter.
     *
     * Security considerations:
     * - Intended for admin-only usage.
     * - Search is limited to username/email and does not expose sensitive fields.
     */
    public PageResponse<UserProfileResponse> getAllUsers(Pageable pageable) {
        return getAllUsers(pageable, null);
    }

    public PageResponse<UserProfileResponse> getAllUsers(Pageable pageable, String search) {
        Page<User> page;
        if (search == null || search.isBlank()) {
            page = userRepository.findAll(pageable);
        } else {
            String q = search.trim();
            page = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, pageable);
        }

        List<UserProfileResponse> content = page.getContent().stream().map(this::toProfile).toList();
        return PageResponse.<UserProfileResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public UserProfileResponse toProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}
