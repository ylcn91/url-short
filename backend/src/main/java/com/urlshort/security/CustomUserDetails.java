package com.urlshort.security;

import com.urlshort.domain.User;
import com.urlshort.domain.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom implementation of Spring Security's UserDetails interface.
 *
 * This class wraps the User entity and provides Spring Security with
 * the necessary authentication and authorization information. It includes
 * workspace context to support multi-tenancy.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final Long workspaceId;
    private final String email;
    private final String password;
    private final String fullName;
    private final UserRole role;
    private final boolean isDeleted;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Creates CustomUserDetails from a User entity.
     *
     * @param user the user entity
     */
    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.workspaceId = user.getWorkspace().getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.isDeleted = user.getIsDeleted();
        this.authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    /**
     * Static factory method to create CustomUserDetails from a User entity.
     *
     * @param user the user entity
     * @return CustomUserDetails instance
     */
    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isDeleted;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !isDeleted;
    }

    /**
     * Checks if the user has admin role.
     *
     * @return true if user is an admin
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Checks if the user has member or higher role.
     *
     * @return true if user is a member or admin
     */
    public boolean isMemberOrHigher() {
        return role == UserRole.MEMBER || role == UserRole.ADMIN;
    }

    /**
     * Checks if the user can only view (viewer role).
     *
     * @return true if user is a viewer
     */
    public boolean isViewer() {
        return role == UserRole.VIEWER;
    }
}
