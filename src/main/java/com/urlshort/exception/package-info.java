/**
 * Custom exceptions and error handling infrastructure.
 *
 * Contains:
 * - Custom exception classes extending RuntimeException for specific error scenarios
 * - GlobalExceptionHandler using @RestControllerAdvice for centralized exception handling
 * - Standardized error responses with request tracing
 *
 * Exception classes:
 * - ResourceNotFoundException: 404 Not Found
 * - DuplicateResourceException: 409 Conflict
 * - InvalidUrlException: 400 Bad Request
 * - LinkExpiredException: 410 Gone
 * - WorkspaceQuotaExceededException: 429 Too Many Requests
 * - UnauthorizedException: 401 Unauthorized
 *
 * Global handlers also manage:
 * - Validation errors: MethodArgumentNotValidException (400)
 * - Malformed JSON: HttpMessageNotReadableException (400)
 * - Uncaught exceptions: Exception (500)
 */
package com.urlshort.exception;
