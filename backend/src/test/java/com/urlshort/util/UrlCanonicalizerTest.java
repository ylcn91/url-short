package com.urlshort.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for UrlCanonicalizer utility class.
 * Tests URL normalization with various inputs including edge cases.
 */
@DisplayName("UrlCanonicalizer Tests")
class UrlCanonicalizerTest {

    @Test
    @DisplayName("Should lowercase scheme and host")
    void testLowercaseSchemeAndHost() {
        String input = "HTTP://Example.COM/path";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com/path", result);
    }

    @Test
    @DisplayName("Should remove default HTTP port 80")
    void testRemoveDefaultHttpPort() {
        String input = "http://example.com:80/path";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com/path", result);
        assertFalse(result.contains(":80"));
    }

    @Test
    @DisplayName("Should remove default HTTPS port 443")
    void testRemoveDefaultHttpsPort() {
        String input = "https://example.com:443/path";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("https://example.com/path", result);
        assertFalse(result.contains(":443"));
    }

    @Test
    @DisplayName("Should keep non-default ports")
    void testKeepNonDefaultPort() {
        String input = "http://example.com:8080/path";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com:8080/path", result);
        assertTrue(result.contains(":8080"));
    }

    @Test
    @DisplayName("Should sort query parameters alphabetically")
    void testSortQueryParameters() {
        String input = "http://example.com/path?z=1&a=2&m=3";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com/path?a=2&m=3&z=1", result);
    }

    @Test
    @DisplayName("Should remove fragment identifiers")
    void testRemoveFragment() {
        String input = "http://example.com/path#section";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com/path", result);
        assertFalse(result.contains("#"));
    }

    @Test
    @DisplayName("Should remove trailing slash from non-root paths")
    void testRemoveTrailingSlash() {
        String input = "http://example.com/path/";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com/path", result);
    }

    @Test
    @DisplayName("Should keep trailing slash for root path")
    void testKeepRootSlash() {
        String input = "http://example.com/";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com/", result);
    }

    @Test
    @DisplayName("Should collapse multiple slashes in path")
    void testCollapseMultipleSlashes() {
        String input = "http://example.com//a///b////c";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com/a/b/c", result);
        assertFalse(result.contains("//"));
    }

    @Test
    @DisplayName("Should add missing http scheme")
    void testAddMissingScheme() {
        String input = "example.com/path";
        String result = UrlCanonicalizer.canonicalize(input);

        assertTrue(result.startsWith("http://"));
        assertEquals("http://example.com/path", result);
    }

    @Test
    @DisplayName("Should handle protocol-relative URLs")
    void testProtocolRelativeUrl() {
        String input = "//example.com/path";
        String result = UrlCanonicalizer.canonicalize(input);

        assertTrue(result.startsWith("http://"));
        assertEquals("http://example.com/path", result);
    }

    @Test
    @DisplayName("Should add missing path")
    void testAddMissingPath() {
        String input = "http://example.com";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com/", result);
    }

    @Test
    @DisplayName("Should trim whitespace")
    void testTrimWhitespace() {
        String input = "  http://example.com/path  ";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com/path", result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should throw exception for null or empty URLs")
    void testNullOrEmptyUrl(String input) {
        assertThrows(IllegalArgumentException.class,
            () -> UrlCanonicalizer.canonicalize(input));
    }

    @Test
    @DisplayName("Should reject FTP scheme")
    void testRejectFtpScheme() {
        String input = "ftp://example.com/file";
        assertThrows(IllegalArgumentException.class,
            () -> UrlCanonicalizer.canonicalize(input));
    }

    @Test
    @DisplayName("Should reject invalid URL format")
    void testRejectInvalidUrl() {
        String input = "http://";
        assertThrows(IllegalArgumentException.class,
            () -> UrlCanonicalizer.canonicalize(input));
    }

    @Test
    @DisplayName("Should be deterministic - same input produces same output")
    void testDeterminism() {
        String input = "HTTP://Example.com:80/Path?z=1&a=2#frag";

        String first = UrlCanonicalizer.canonicalize(input);
        String second = UrlCanonicalizer.canonicalize(input);

        assertEquals(first, second);
    }

    @Test
    @DisplayName("Should be idempotent - canonicalize(canonicalize(url)) == canonicalize(url)")
    void testIdempotence() {
        String input = "HTTP://Example.com:80/Path?z=1&a=2#frag";

        String once = UrlCanonicalizer.canonicalize(input);
        String twice = UrlCanonicalizer.canonicalize(once);

        assertEquals(once, twice);
    }

    @ParameterizedTest
    @CsvSource({
        "'http://example.com/path', 'HTTP://EXAMPLE.COM/path', true",
        "'http://example.com/', 'http://example.com:80/', true",
        "'http://example.com/path', 'http://example.com/path#section', true",
        "'http://example.com/path?a=1&b=2', 'http://example.com/path?b=2&a=1', true",
        "'http://example.com/path', 'http://example.com/path/', true",
        "'http://example.com/a/b', 'http://example.com//a///b', true"
    })
    @DisplayName("Should produce same canonical form for equivalent URLs")
    void testEquivalentUrls(String url1, String url2, boolean shouldMatch) {
        String canonical1 = UrlCanonicalizer.canonicalize(url1);
        String canonical2 = UrlCanonicalizer.canonicalize(url2);

        if (shouldMatch) {
            assertEquals(canonical1, canonical2);
        } else {
            assertNotEquals(canonical1, canonical2);
        }
    }

    @Test
    @DisplayName("Should handle URLs with complex query strings")
    void testComplexQueryString() {
        String input = "http://example.com/search?q=hello+world&lang=en&page=1";
        String result = UrlCanonicalizer.canonicalize(input);

        assertNotNull(result);
        assertTrue(result.contains("?"));
        // Query params should be sorted
        assertTrue(result.indexOf("lang=") < result.indexOf("page="));
        assertTrue(result.indexOf("page=") < result.indexOf("q="));
    }

    @Test
    @DisplayName("Should handle URLs with port and query")
    void testPortAndQuery() {
        String input = "http://example.com:8080/path?b=2&a=1";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com:8080/path?a=1&b=2", result);
    }

    @Test
    @DisplayName("Should handle HTTPS URLs correctly")
    void testHttpsUrl() {
        String input = "HTTPS://Example.com:443/secure";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("https://example.com/secure", result);
    }

    @Test
    @DisplayName("Should handle URLs with userinfo")
    void testUrlWithUserinfo() {
        String input = "http://user:pass@example.com/path";
        String result = UrlCanonicalizer.canonicalize(input);

        assertNotNull(result);
        // Userinfo should be preserved in canonicalization
        assertTrue(result.contains("example.com"));
    }

    @Test
    @DisplayName("Should handle URLs with international domain names")
    void testInternationalDomainNames() {
        String input = "http://münchen.de/path";
        // This should work with proper IDN handling
        assertDoesNotThrow(() -> UrlCanonicalizer.canonicalize(input));
    }

    @Test
    @DisplayName("Should handle URLs with encoded characters in path")
    void testEncodedPath() {
        String input = "http://example.com/path%20with%20spaces";
        String result = UrlCanonicalizer.canonicalize(input);

        assertNotNull(result);
        assertTrue(result.contains("example.com"));
    }

    @Test
    @DisplayName("Should handle URLs with multiple query parameters with same key")
    void testDuplicateQueryKeys() {
        String input = "http://example.com/path?tag=java&tag=spring&tag=boot";
        String result = UrlCanonicalizer.canonicalize(input);

        assertNotNull(result);
        assertTrue(result.contains("?"));
    }

    @Test
    @DisplayName("Should not allow instantiation")
    void testCannotInstantiate() {
        assertThrows(UnsupportedOperationException.class,
            () -> {
                var constructor = UrlCanonicalizer.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            },
            "Utility class should not be instantiable");
    }

    @Test
    @DisplayName("Should handle very long URLs")
    void testLongUrl() {
        String longPath = "a/".repeat(100);
        String input = "http://example.com/" + longPath + "?z=1&a=2";
        String result = UrlCanonicalizer.canonicalize(input);

        assertNotNull(result);
        assertTrue(result.startsWith("http://example.com/"));
    }

    @Test
    @DisplayName("Should handle URLs with only query parameters")
    void testQueryOnly() {
        String input = "http://example.com?param=value";
        String result = UrlCanonicalizer.canonicalize(input);

        assertEquals("http://example.com/?param=value", result);
    }

    @Test
    @DisplayName("Should preserve case sensitivity in path")
    void testCaseSensitivePath() {
        String input = "http://example.com/Path/To/Resource";
        String result = UrlCanonicalizer.canonicalize(input);

        // Path should be case-sensitive (only scheme and host are lowercased)
        assertTrue(result.contains("/Path/To/Resource"));
    }

    @Test
    @DisplayName("Should preserve case sensitivity in query values")
    void testCaseSensitiveQueryValues() {
        String input = "http://example.com/path?Name=John&age=30";
        String result = UrlCanonicalizer.canonicalize(input);

        // Query parameters should maintain case
        assertTrue(result.contains("Name=John"));
    }
}
