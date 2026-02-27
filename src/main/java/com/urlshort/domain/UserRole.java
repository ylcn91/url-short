package com.urlshort.domain;

/**
 * User role enumeration for workspace access control.
 * Roles define the level of access and permissions within a workspace:
 * - ADMIN: Full access to workspace settings, user management, and all links
 * - MEMBER: Standard access to create, edit, and delete their own links
 * - VIEWER: Read-only access to view links and analytics
 */
public enum UserRole {
    /**
     * Full access to workspace settings, user management, and all links.
     */
    ADMIN,

    /**
     * Standard access to create, edit, and delete their own links.
     */
    MEMBER,

    /**
     * Read-only access to view links and analytics.
     */
    VIEWER
}
