package com.urlshort.security;

import com.urlshort.domain.User;
import com.urlshort.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 *
 * This service loads user-specific data during authentication. It retrieves
 * user information from the database and handles workspace context for
 * multi-tenancy support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their email address (username).
     *
     * This method is called by Spring Security during authentication to retrieve
     * user details. It fetches the user from the database, excluding soft-deleted
     * users, and wraps the entity in a CustomUserDetails object.
     *
     * @param email the user's email address
     * @return UserDetails containing user information
     * @throws UsernameNotFoundException if the user is not found or is deleted
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmailAndIsDeletedFalse(email)
            .orElseThrow(() -> {
                log.warn("User not found with email: {}", email);
                return new UsernameNotFoundException("User not found with email: " + email);
            });

        log.debug("User found: {} (workspace: {})", user.getEmail(), user.getWorkspace().getId());
        return CustomUserDetails.from(user);
    }

    /**
     * Loads a user by their ID.
     *
     * This is useful for JWT token validation where we have the user ID
     * in the token claims.
     *
     * @param userId the user's ID
     * @return UserDetails containing user information
     * @throws UsernameNotFoundException if the user is not found or is deleted
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", userId);

        User user = userRepository.findById(userId)
            .filter(u -> !u.getIsDeleted())
            .orElseThrow(() -> {
                log.warn("User not found with ID: {}", userId);
                return new UsernameNotFoundException("User not found with ID: " + userId);
            });

        log.debug("User found: {} (workspace: {})", user.getEmail(), user.getWorkspace().getId());
        return CustomUserDetails.from(user);
    }
}
