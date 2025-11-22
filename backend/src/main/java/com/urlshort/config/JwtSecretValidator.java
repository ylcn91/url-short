package com.urlshort.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Validates JWT secret configuration at application startup.
 * <p>
 * This validator ensures that the JWT secret is:
 * - Not using the default/example value from application.yml
 * - Sufficiently long (minimum 32 characters for 256-bit security)
 * - Not a common/weak pattern
 * </p>
 * <p>
 * If validation fails in production, the application will refuse to start.
 * In development mode, only warnings are logged.
 * </p>
 *
 * @since 1.0.1
 */
@Component
@Slf4j
public class JwtSecretValidator implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * Weak/default secrets that should never be used in production.
     * These are common examples found in tutorials and documentation.
     */
    private static final List<String> FORBIDDEN_SECRETS = Arrays.asList(
        "secret",
        "your-secret",
        "your-256-bit-secret",
        "change-this",
        "change-me",
        "changeme",
        "please-change",
        "default",
        "your-256-bit-secret-key-change-this-in-production",
        "your-256-bit-secret-key-change-this-in-production-please-make-it-long-enough",
        "your-256-bit-secret-key-change-this-in-production-please-use-strong-random-string"
    );

    private static final int MINIMUM_SECRET_LENGTH = 32; // 256 bits
    private static final int RECOMMENDED_SECRET_LENGTH = 43; // 344 bits (base64 of 32 bytes)

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("=================================================");
        log.info("JWT Secret Security Validation");
        log.info("=================================================");

        ValidationResult result = validateJwtSecret();

        if (!result.isValid()) {
            if (isProductionEnvironment()) {
                // In production, fail fast - refuse to start
                log.error("╔═══════════════════════════════════════════════════════╗");
                log.error("║  CRITICAL SECURITY ERROR - APPLICATION STARTUP FAILED ║");
                log.error("╚═══════════════════════════════════════════════════════╝");
                log.error("");
                log.error("JWT Secret validation failed:");
                result.getErrors().forEach(error -> log.error("  ✗ {}", error));
                log.error("");
                log.error("SOLUTION:");
                log.error("  1. Generate a secure JWT secret:");
                log.error("     openssl rand -base64 32");
                log.error("");
                log.error("  2. Set the JWT_SECRET environment variable:");
                log.error("     export JWT_SECRET=\"your-generated-secret-here\"");
                log.error("");
                log.error("  3. Or update application.yml (not recommended for production)");
                log.error("");
                log.error("For more information, see: docs/PRODUCTION_SECRETS.md");
                log.error("=================================================");

                throw new IllegalStateException(
                    "JWT secret validation failed in production environment. " +
                    "Application cannot start with insecure JWT configuration. " +
                    "See logs for details."
                );
            } else {
                // In development, just warn
                log.warn("╔═══════════════════════════════════════════════════════╗");
                log.warn("║  JWT SECRET SECURITY WARNING                          ║");
                log.warn("╚═══════════════════════════════════════════════════════╝");
                log.warn("");
                log.warn("JWT Secret validation issues detected:");
                result.getErrors().forEach(error -> log.warn("  ⚠ {}", error));
                log.warn("");
                log.warn("While this is acceptable in development, NEVER use this");
                log.warn("configuration in production!");
                log.warn("");
                log.warn("Generate a secure secret with: openssl rand -base64 32");
                log.warn("=================================================");
            }
        } else {
            log.info("✓ JWT Secret validation passed");

            if (result.getWarnings().isEmpty()) {
                log.info("✓ Secret strength: EXCELLENT");
            } else {
                log.info("✓ Secret strength: ACCEPTABLE");
                result.getWarnings().forEach(warning -> log.info("  ℹ {}", warning));
            }

            log.info("=================================================");
        }
    }

    /**
     * Validates the JWT secret against security requirements.
     *
     * @return validation result with errors and warnings
     */
    private ValidationResult validateJwtSecret() {
        ValidationResult result = new ValidationResult();

        // Check 1: Secret must not be null or empty
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            result.addError("JWT secret is null or empty");
            return result; // Fatal error, skip other checks
        }

        // Check 2: Secret must not be a common/default value
        String lowerSecret = jwtSecret.toLowerCase().trim();
        for (String forbidden : FORBIDDEN_SECRETS) {
            if (lowerSecret.contains(forbidden.toLowerCase())) {
                result.addError(
                    "JWT secret contains forbidden pattern: '" + forbidden + "'. " +
                    "This appears to be a default/example value."
                );
            }
        }

        // Check 3: Secret must meet minimum length requirement
        int secretLength = jwtSecret.getBytes(StandardCharsets.UTF_8).length;
        if (secretLength < MINIMUM_SECRET_LENGTH) {
            result.addError(
                String.format(
                    "JWT secret is too short (%d bytes). Minimum required: %d bytes (256 bits). " +
                    "Current security level: %d bits.",
                    secretLength, MINIMUM_SECRET_LENGTH, secretLength * 8
                )
            );
        }

        // Check 4: Warn if secret is below recommended length
        if (secretLength >= MINIMUM_SECRET_LENGTH && secretLength < RECOMMENDED_SECRET_LENGTH) {
            result.addWarning(
                String.format(
                    "JWT secret meets minimum requirements (%d bytes) but is below recommended " +
                    "length (%d bytes). Consider generating a stronger secret with: openssl rand -base64 32",
                    secretLength, RECOMMENDED_SECRET_LENGTH
                )
            );
        }

        // Check 5: Warn if secret appears to be low entropy (all same character, sequential, etc.)
        if (hasLowEntropy(jwtSecret)) {
            result.addWarning(
                "JWT secret appears to have low entropy. Use a cryptographically random secret: " +
                "openssl rand -base64 32"
            );
        }

        return result;
    }

    /**
     * Checks if the secret has suspiciously low entropy.
     * This is a heuristic check, not cryptographically rigorous.
     *
     * @param secret the secret to check
     * @return true if secret appears to have low entropy
     */
    private boolean hasLowEntropy(String secret) {
        if (secret.length() < 8) {
            return true;
        }

        // Check for repeated characters (e.g., "aaaaaaa...")
        long uniqueChars = secret.chars().distinct().count();
        if (uniqueChars < secret.length() / 4) {
            return true;
        }

        // Check for sequential patterns (e.g., "12345678", "abcdefgh")
        int sequentialCount = 0;
        for (int i = 1; i < Math.min(secret.length(), 10); i++) {
            if (secret.charAt(i) == secret.charAt(i - 1) + 1 ||
                secret.charAt(i) == secret.charAt(i - 1) - 1) {
                sequentialCount++;
            }
        }
        if (sequentialCount > secret.length() / 2) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the application is running in a production environment.
     *
     * @return true if running in production
     */
    private boolean isProductionEnvironment() {
        return activeProfile.toLowerCase().contains("prod");
    }

    /**
     * Holds validation results with errors and warnings.
     */
    private static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}
