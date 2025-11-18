package com.urlshort.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for generating deterministic short codes from URLs.
 * <p>
 * This class implements the core short code generation algorithm for URL shortening.
 * It uses SHA-256 hashing combined with Base58 encoding to generate short, unique,
 * and deterministic codes for URLs within a workspace context.
 * </p>
 * <p>
 * The algorithm guarantees:
 * </p>
 * <ul>
 *   <li><b>Determinism:</b> The same URL and workspace ID always produce the same short code</li>
 *   <li><b>Workspace Isolation:</b> Different workspaces can have different codes for the same URL</li>
 *   <li><b>Collision Resistance:</b> Using 64 bits of SHA-256 hash provides excellent uniqueness</li>
 *   <li><b>Collision Handling:</b> Retry mechanism with salt for the rare case of collisions</li>
 * </ul>
 * <p>
 * This class is thread-safe as all methods are static and use thread-safe algorithms
 * (SHA-256 instances are created per invocation).
 * </p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Generate short code for a URL in a workspace
 * String normalizedUrl = UrlCanonicalizer.canonicalize("http://example.com");
 * String shortCode = ShortCodeGenerator.generateShortCode(
 *     normalizedUrl,
 *     123456L,  // workspace ID
 *     0         // retry salt (0 for first attempt)
 * );
 * // Returns: 10-character Base58 code like "MaSgB7xKpQ"
 *
 * // Generate code with retry salt (for collision handling)
 * String retryCode = ShortCodeGenerator.generateShortCode(
 *     normalizedUrl,
 *     123456L,
 *     1  // First retry
 * );
 * // Returns: Different code due to salt
 * }</pre>
 *
 * <h2>Algorithm Details:</h2>
 * <ol>
 *   <li>Construct hash input: normalizedUrl + "|" + workspaceId [+ "|" + retrySalt]</li>
 *   <li>Compute SHA-256 hash (32 bytes)</li>
 *   <li>Extract first 8 bytes (64 bits) for encoding</li>
 *   <li>Encode to Base58 with target length of 10 characters</li>
 * </ol>
 *
 * <h2>Short Code Properties:</h2>
 * <ul>
 *   <li>Default length: 10 characters</li>
 *   <li>Character set: Base58 (58^10 ≈ 4.3 × 10^17 possible codes)</li>
 *   <li>Collision probability with 1M URLs in workspace: 0.0116%</li>
 *   <li>URL-safe and human-readable</li>
 * </ul>
 *
 * @see UrlCanonicalizer for URL normalization
 * @see Base58Encoder for encoding details
 * @see <a href="https://en.wikipedia.org/wiki/SHA-2">SHA-256 Algorithm</a>
 * @since 1.0
 */
public final class ShortCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(ShortCodeGenerator.class);

    /**
     * SHA-256 algorithm identifier.
     */
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Separator used between components in hash input.
     */
    private static final String SEPARATOR = "|";

    /**
     * Default target length for generated short codes.
     */
    private static final int DEFAULT_CODE_LENGTH = 10;

    /**
     * Number of bytes to extract from hash for encoding.
     * Using 8 bytes (64 bits) provides good collision resistance.
     */
    private static final int HASH_BYTES_TO_USE = 8;

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private ShortCodeGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates a deterministic short code from a normalized URL and workspace ID.
     * <p>
     * This is the primary method for generating short codes. It implements the
     * deterministic hashing algorithm specified in the URL shortening specification.
     * </p>
     * <p>
     * The generated short code is exactly 10 characters long and uses the Base58
     * alphabet (excluding visually ambiguous characters: 0, O, I, l).
     * </p>
     *
     * <h3>Hash Input Format:</h3>
     * <ul>
     *   <li>Without salt (retrySalt = 0): normalizedUrl + "|" + workspaceId</li>
     *   <li>With salt (retrySalt > 0): normalizedUrl + "|" + workspaceId + "|" + retrySalt</li>
     * </ul>
     *
     * <h3>Algorithm Steps:</h3>
     * <ol>
     *   <li>Construct hash input string with separator</li>
     *   <li>Compute SHA-256 hash (32 bytes)</li>
     *   <li>Extract first 8 bytes (64 bits)</li>
     *   <li>Encode to Base58 with length 10</li>
     * </ol>
     *
     * <h3>Examples:</h3>
     * <pre>{@code
     * // First attempt (no salt)
     * String code1 = ShortCodeGenerator.generateShortCode(
     *     "http://example.com/api/users?id=123&name=john",
     *     123456L,
     *     0
     * );
     * // Returns: "MaSgB7xKpQ" (example)
     *
     * // Same inputs always produce same output
     * String code2 = ShortCodeGenerator.generateShortCode(
     *     "http://example.com/api/users?id=123&name=john",
     *     123456L,
     *     0
     * );
     * // code1.equals(code2) is true
     *
     * // Different workspace produces different code
     * String code3 = ShortCodeGenerator.generateShortCode(
     *     "http://example.com/api/users?id=123&name=john",
     *     789012L,  // Different workspace
     *     0
     * );
     * // code1.equals(code3) is false
     *
     * // Retry with salt (for collision handling)
     * String code4 = ShortCodeGenerator.generateShortCode(
     *     "http://example.com/api/users?id=123&name=john",
     *     123456L,
     *     1  // Salt = 1
     * );
     * // code1.equals(code4) is false
     * }</pre>
     *
     * @param normalizedUrl the canonical URL (must be pre-normalized using UrlCanonicalizer)
     * @param workspaceId the workspace identifier (must be non-null)
     * @param retrySalt the retry salt for collision handling (0 for first attempt, 1+ for retries)
     * @return a 10-character Base58 encoded short code
     * @throws IllegalArgumentException if normalizedUrl is null/empty or workspaceId is null
     * @throws IllegalStateException if SHA-256 algorithm is not available (should never happen)
     */
    public static String generateShortCode(String normalizedUrl, Long workspaceId, int retrySalt) {
        return generateShortCode(normalizedUrl, workspaceId, retrySalt, DEFAULT_CODE_LENGTH);
    }

    /**
     * Generates a deterministic short code with a custom length.
     * <p>
     * This overload allows specifying a custom code length, which can be useful
     * for generating longer codes when higher collision resistance is needed,
     * or shorter codes for specific use cases.
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * // Generate 12-character code for extra collision resistance
     * String longCode = ShortCodeGenerator.generateShortCode(
     *     "http://example.com/page",
     *     123456L,
     *     0,
     *     12  // Custom length
     * );
     * // Returns: 12-character Base58 code
     * }</pre>
     *
     * @param normalizedUrl the canonical URL (must be pre-normalized)
     * @param workspaceId the workspace identifier
     * @param retrySalt the retry salt for collision handling
     * @param codeLength the desired length of the short code (typically 10-12)
     * @return a Base58 encoded short code of the specified length
     * @throws IllegalArgumentException if inputs are invalid or codeLength is non-positive
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    public static String generateShortCode(String normalizedUrl, Long workspaceId,
                                          int retrySalt, int codeLength) {
        // Validate inputs
        validateInputs(normalizedUrl, workspaceId, retrySalt, codeLength);

        // Construct hash input
        String hashInput = constructHashInput(normalizedUrl, workspaceId, retrySalt);

        // Compute hash
        byte[] hashBytes = computeHash(hashInput);

        // Encode to Base58
        String shortCode = Base58Encoder.encode(hashBytes, codeLength);

        log.debug("Generated short code '{}' for URL '{}' in workspace {} with salt {}",
                  shortCode, normalizedUrl, workspaceId, retrySalt);

        return shortCode;
    }

    /**
     * Validates the inputs for short code generation.
     *
     * @param normalizedUrl the normalized URL
     * @param workspaceId the workspace ID
     * @param retrySalt the retry salt
     * @param codeLength the desired code length
     * @throws IllegalArgumentException if any input is invalid
     */
    private static void validateInputs(String normalizedUrl, Long workspaceId,
                                       int retrySalt, int codeLength) {
        if (normalizedUrl == null || normalizedUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Normalized URL cannot be null or empty");
        }

        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID cannot be null");
        }

        if (retrySalt < 0) {
            throw new IllegalArgumentException("Retry salt must be non-negative, got: " + retrySalt);
        }

        if (codeLength <= 0) {
            throw new IllegalArgumentException("Code length must be positive, got: " + codeLength);
        }
    }

    /**
     * Constructs the hash input string according to the algorithm specification.
     * <p>
     * Format: normalizedUrl + "|" + workspaceId [+ "|" + retrySalt if retrySalt > 0]
     * </p>
     *
     * @param normalizedUrl the normalized URL
     * @param workspaceId the workspace ID
     * @param retrySalt the retry salt
     * @return the constructed hash input string
     */
    private static String constructHashInput(String normalizedUrl, Long workspaceId, int retrySalt) {
        StringBuilder input = new StringBuilder();
        input.append(normalizedUrl);
        input.append(SEPARATOR);
        input.append(workspaceId);

        // Add retry salt if this is a retry attempt
        if (retrySalt > 0) {
            input.append(SEPARATOR);
            input.append(retrySalt);
        }

        String result = input.toString();
        log.trace("Hash input constructed: {}", result);
        return result;
    }

    /**
     * Computes the SHA-256 hash of the input string.
     *
     * @param input the string to hash
     * @return the 32-byte SHA-256 hash
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    private static byte[] computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            if (log.isTraceEnabled()) {
                log.trace("SHA-256 hash computed: {} bytes", hash.length);
            }

            return hash;
        } catch (NoSuchAlgorithmException e) {
            // This should never happen as SHA-256 is required in all JVMs
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates a short code directly from a URL (performs canonicalization internally).
     * <p>
     * This is a convenience method that combines URL canonicalization and short code
     * generation in a single call. It's equivalent to:
     * </p>
     * <pre>{@code
     * String canonical = UrlCanonicalizer.canonicalize(url);
     * return generateShortCode(canonical, workspaceId, retrySalt);
     * }</pre>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * // Generate code directly from raw URL
     * String code = ShortCodeGenerator.generateShortCodeFromUrl(
     *     "HTTP://Example.com:80/path?z=1&a=2",
     *     123456L,
     *     0
     * );
     * // Automatically canonicalizes to "http://example.com/path?a=2&z=1"
     * // Then generates short code
     * }</pre>
     *
     * @param url the raw URL (will be canonicalized)
     * @param workspaceId the workspace identifier
     * @param retrySalt the retry salt for collision handling
     * @return a 10-character Base58 encoded short code
     * @throws IllegalArgumentException if URL is invalid or cannot be canonicalized
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    public static String generateShortCodeFromUrl(String url, Long workspaceId, int retrySalt) {
        String canonicalUrl = UrlCanonicalizer.canonicalize(url);
        return generateShortCode(canonicalUrl, workspaceId, retrySalt);
    }

    /**
     * Generates multiple short codes with increasing retry salts.
     * <p>
     * This method is useful for testing or when you need to generate alternative
     * codes in case of collisions. It returns an array of codes, each generated
     * with a different retry salt (0 through maxRetries-1).
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * String normalizedUrl = UrlCanonicalizer.canonicalize("http://example.com");
     * String[] codes = ShortCodeGenerator.generateShortCodesWithRetries(
     *     normalizedUrl,
     *     123456L,
     *     3  // Generate 3 alternatives
     * );
     * // codes[0] = code with salt 0
     * // codes[1] = code with salt 1
     * // codes[2] = code with salt 2
     * }</pre>
     *
     * @param normalizedUrl the canonical URL
     * @param workspaceId the workspace identifier
     * @param maxRetries the number of codes to generate (1-10 recommended)
     * @return array of short codes with different salts
     * @throws IllegalArgumentException if inputs are invalid or maxRetries is non-positive
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    public static String[] generateShortCodesWithRetries(String normalizedUrl,
                                                         Long workspaceId,
                                                         int maxRetries) {
        if (maxRetries <= 0) {
            throw new IllegalArgumentException("Max retries must be positive");
        }

        String[] codes = new String[maxRetries];
        for (int i = 0; i < maxRetries; i++) {
            codes[i] = generateShortCode(normalizedUrl, workspaceId, i);
        }

        log.debug("Generated {} short codes with retries for workspace {}",
                  maxRetries, workspaceId);

        return codes;
    }

    /**
     * Returns the default code length used by this generator.
     *
     * @return the default code length (10 characters)
     */
    public static int getDefaultCodeLength() {
        return DEFAULT_CODE_LENGTH;
    }

    /**
     * Returns the hash algorithm used for short code generation.
     *
     * @return the hash algorithm name ("SHA-256")
     */
    public static String getHashAlgorithm() {
        return HASH_ALGORITHM;
    }
}
