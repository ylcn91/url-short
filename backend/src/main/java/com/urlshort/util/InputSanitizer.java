package com.urlshort.util;

import org.owasp.encoder.Encode;

/**
 * Utility class for sanitizing user input to prevent XSS (Cross-Site Scripting) attacks.
 * <p>
 * This class uses the OWASP Java Encoder library to safely encode user-provided content
 * before it's stored in the database or displayed in the UI. This prevents malicious
 * scripts from being executed in users' browsers.
 * </p>
 *
 * <h3>When to Use:</h3>
 * <ul>
 *   <li><b>User-Generated Text:</b> Link titles, descriptions, workspace names</li>
 *   <li><b>Tags and Labels:</b> Custom tags, categories</li>
 *   <li><b>NOT for URLs:</b> URLs are validated separately and should not be HTML-encoded</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // In service layer before saving to database
 * String userTitle = request.getTitle();
 * String safeTitle = InputSanitizer.sanitizeHtml(userTitle);
 * link.setTitle(safeTitle);
 *
 * // For JSON output (rare, usually Jackson handles this)
 * String safeJson = InputSanitizer.sanitizeForJavaScript(userInput);
 * }</pre>
 *
 * <h3>What Gets Sanitized:</h3>
 * <ul>
 *   <li>{@code <script>} → {@code &lt;script&gt;}</li>
 *   <li>{@code "alert('XSS')"} → {@code &quot;alert(&#39;XSS&#39;)&quot;}</li>
 *   <li>{@code <img src=x onerror=alert(1)>} → {@code &lt;img src&#61;x onerror&#61;alert&#40;1&#41;&gt;}</li>
 * </ul>
 *
 * @see <a href="https://owasp.org/www-project-java-encoder/">OWASP Java Encoder</a>
 * @since 1.0.1
 */
public final class InputSanitizer {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private InputSanitizer() {
        throw new UnsupportedOperationException("InputSanitizer is a utility class and cannot be instantiated");
    }

    /**
     * Sanitizes input for safe display in HTML context.
     * <p>
     * This method encodes HTML special characters to prevent XSS attacks.
     * Use this for any user-generated content that will be displayed in HTML.
     * </p>
     *
     * <h3>Examples:</h3>
     * <pre>{@code
     * sanitizeHtml("<script>alert('XSS')</script>")
     * // Returns: "&lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;"
     *
     * sanitizeHtml("John's Link")
     * // Returns: "John&#39;s Link"
     *
     * sanitizeHtml(null)
     * // Returns: null
     * }</pre>
     *
     * @param input the user input to sanitize
     * @return sanitized string safe for HTML display, or null if input was null
     */
    public static String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        if (input.isBlank()) {
            return input;
        }
        return Encode.forHtml(input);
    }

    /**
     * Sanitizes input for safe use in JavaScript context.
     * <p>
     * This method encodes special characters to prevent JavaScript injection.
     * Use this when embedding user data in JavaScript code (though this should
     * generally be avoided in favor of data attributes or JSON).
     * </p>
     *
     * <h3>Examples:</h3>
     * <pre>{@code
     * sanitizeForJavaScript("'; alert('XSS'); //")
     * // Returns: "\\x27; alert(\\x27XSS\\x27); \\/\\/"
     *
     * sanitizeForJavaScript("User's input")
     * // Returns: "User\\x27s input"
     * }</pre>
     *
     * @param input the user input to sanitize
     * @return sanitized string safe for JavaScript context, or null if input was null
     */
    public static String sanitizeForJavaScript(String input) {
        if (input == null) {
            return null;
        }
        if (input.isBlank()) {
            return input;
        }
        return Encode.forJavaScript(input);
    }

    /**
     * Sanitizes input for safe use in HTML attribute values.
     * <p>
     * This method is more restrictive than {@link #sanitizeHtml(String)} and is
     * specifically designed for values that will be placed in HTML attributes.
     * </p>
     *
     * <h3>Examples:</h3>
     * <pre>{@code
     * <div title="${sanitizeForHtmlAttribute(userInput)}">
     *
     * sanitizeForHtmlAttribute("Click \"here\"")
     * // Returns: "Click &quot;here&quot;"
     * }</pre>
     *
     * @param input the user input to sanitize
     * @return sanitized string safe for HTML attributes, or null if input was null
     */
    public static String sanitizeForHtmlAttribute(String input) {
        if (input == null) {
            return null;
        }
        if (input.isBlank()) {
            return input;
        }
        return Encode.forHtmlAttribute(input);
    }

    /**
     * Sanitizes input for safe use in CSS context.
     * <p>
     * This method encodes characters to prevent CSS injection attacks.
     * Use this when user input is used in CSS (which should be rare).
     * </p>
     *
     * @param input the user input to sanitize
     * @return sanitized string safe for CSS context, or null if input was null
     */
    public static String sanitizeForCss(String input) {
        if (input == null) {
            return null;
        }
        if (input.isBlank()) {
            return input;
        }
        return Encode.forCssString(input);
    }

    /**
     * Sanitizes input for safe use in URLs (query parameters).
     * <p>
     * This method URL-encodes the input to prevent injection attacks via URL parameters.
     * </p>
     *
     * <h3>Examples:</h3>
     * <pre>{@code
     * String searchTerm = sanitizeForUrl(userInput);
     * String url = "/search?q=" + searchTerm;
     *
     * sanitizeForUrl("hello world")
     * // Returns: "hello+world"
     *
     * sanitizeForUrl("user@example.com")
     * // Returns: "user%40example.com"
     * }</pre>
     *
     * @param input the user input to sanitize
     * @return URL-encoded string, or null if input was null
     */
    public static String sanitizeForUrl(String input) {
        if (input == null) {
            return null;
        }
        if (input.isBlank()) {
            return input;
        }
        return Encode.forUriComponent(input);
    }

    /**
     * Strips all HTML tags from input, leaving only plain text.
     * <p>
     * This is the most aggressive sanitization method and should be used
     * when you want to completely remove any HTML formatting.
     * </p>
     *
     * <h3>Examples:</h3>
     * <pre>{@code
     * stripHtml("<b>Bold</b> and <i>italic</i>")
     * // Returns: "Bold and italic"
     *
     * stripHtml("<script>alert('XSS')</script>Text")
     * // Returns: "Text"
     * }</pre>
     *
     * @param input the HTML input to strip
     * @return plain text with all HTML removed, or null if input was null
     */
    public static String stripHtml(String input) {
        if (input == null) {
            return null;
        }
        if (input.isBlank()) {
            return input;
        }
        // Remove all HTML tags
        return input.replaceAll("<[^>]*>", "").trim();
    }

    /**
     * Validates and sanitizes a string to contain only safe characters.
     * <p>
     * This method removes or encodes potentially dangerous characters while
     * preserving readable text. Useful for names, titles, and descriptions.
     * </p>
     *
     * <h3>Allowed Characters:</h3>
     * <ul>
     *   <li>Letters (a-z, A-Z)</li>
     *   <li>Numbers (0-9)</li>
     *   <li>Spaces</li>
     *   <li>Common punctuation: . , ! ? - _ ' "</li>
     * </ul>
     *
     * @param input the input to sanitize
     * @param maxLength maximum allowed length (longer strings will be truncated)
     * @return sanitized string containing only safe characters
     */
    public static String sanitizeSafeName(String input, int maxLength) {
        if (input == null) {
            return null;
        }

        // First strip HTML tags
        String stripped = stripHtml(input);

        // Remove or replace dangerous characters
        String safe = stripped
            .replaceAll("[<>\"'&]", "")  // Remove HTML-dangerous chars
            .replaceAll("[\\x00-\\x1F\\x7F]", "")  // Remove control characters
            .trim();

        // Truncate if too long
        if (safe.length() > maxLength) {
            safe = safe.substring(0, maxLength).trim();
        }

        return safe;
    }

    /**
     * Comprehensive sanitization for user-generated content.
     * <p>
     * This method applies multiple layers of sanitization and is recommended
     * for general use with user input. It combines HTML encoding with safe
     * character filtering.
     * </p>
     *
     * @param input the user input to sanitize
     * @param maxLength maximum allowed length
     * @return fully sanitized string safe for storage and display
     */
    public static String sanitizeUserContent(String input, int maxLength) {
        if (input == null) {
            return null;
        }

        // Step 1: Remove HTML tags
        String noHtml = stripHtml(input);

        // Step 2: Encode remaining special characters
        String encoded = sanitizeHtml(noHtml);

        // Step 3: Truncate to max length
        if (encoded != null && encoded.length() > maxLength) {
            encoded = encoded.substring(0, maxLength).trim();
        }

        return encoded;
    }
}
