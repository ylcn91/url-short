/**
 * REST API controllers for the URL shortener service.
 * <p>
 * This package contains all REST controllers that handle HTTP requests and responses
 * for the URL shortener platform. Controllers follow REST best practices and use
 * Spring annotations for routing, validation, and security.
 * </p>
 *
 * <h3>Available Controllers:</h3>
 * <ul>
 *   <li><b>ShortLinkController:</b> Manages short links (CRUD operations, analytics, bulk creation)</li>
 *   <li><b>RedirectController:</b> Handles public URL redirection (no authentication required)</li>
 *   <li><b>WorkspaceController:</b> Manages workspaces and members (settings, member management)</li>
 * </ul>
 *
 * <h3>Common Features:</h3>
 * <ul>
 *   <li>Consistent ApiResponse wrapper for all responses</li>
 *   <li>Comprehensive JavaDoc with request/response examples</li>
 *   <li>Request validation using @Valid and Bean Validation</li>
 *   <li>Security with @PreAuthorize annotations</li>
 *   <li>Logging with @Slf4j for all requests</li>
 *   <li>OpenAPI/Swagger documentation</li>
 * </ul>
 *
 * @see com.urlshort.controller.ShortLinkController
 * @see com.urlshort.controller.RedirectController
 * @see com.urlshort.controller.WorkspaceController
 * @since 1.0
 */
package com.urlshort.controller;
