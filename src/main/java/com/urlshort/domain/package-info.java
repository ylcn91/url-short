/**
 * Domain model and JPA entity classes for the URL shortener platform.
 *
 * <p>This package contains the core domain entities that map to the PostgreSQL database schema:
 * <ul>
 *   <li>{@link com.urlshort.domain.Workspace} - Multi-tenant workspace entity</li>
 *   <li>{@link com.urlshort.domain.User} - User accounts with role-based access</li>
 *   <li>{@link com.urlshort.domain.ShortLink} - Core short link entity with deterministic reuse</li>
 *   <li>{@link com.urlshort.domain.ClickEvent} - Analytics click tracking events</li>
 *   <li>{@link com.urlshort.domain.ApiKey} - API key management for programmatic access</li>
 * </ul>
 *
 * <p>All entities follow Spring Boot 3 and JPA best practices:
 * <ul>
 *   <li>Use Jakarta EE annotations (jakarta.*) instead of Java EE (javax.*)</li>
 *   <li>Leverage Lombok for boilerplate reduction</li>
 *   <li>Implement proper equals/hashCode based on business keys</li>
 *   <li>Use @CreationTimestamp and @UpdateTimestamp for audit timestamps</li>
 *   <li>Support soft delete pattern where appropriate</li>
 *   <li>Include validation annotations for data integrity</li>
 * </ul>
 *
 * @see com.urlshort.domain.UserRole
 * @see com.urlshort.domain.DeviceType
 */
package com.urlshort.domain;
