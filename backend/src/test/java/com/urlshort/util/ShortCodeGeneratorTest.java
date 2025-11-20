package com.urlshort.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ShortCodeGenerator utility class.
 * Tests deterministic short code generation with various inputs.
 */
@DisplayName("ShortCodeGenerator Tests")
class ShortCodeGeneratorTest {

    @Test
    @DisplayName("Should generate 10-character short code by default")
    void testDefaultLength() {
        String url = "http://example.com/test";
        Long workspaceId = 123456L;

        String code = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);

        assertNotNull(code);
        assertEquals(10, code.length(), "Default code length should be 10");
    }

    @Test
    @DisplayName("Should be deterministic - same inputs produce same code")
    void testDeterminism() {
        String url = "http://example.com/api/users";
        Long workspaceId = 789012L;

        String code1 = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);
        String code2 = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);

        assertEquals(code1, code2, "Same inputs should always produce same code");
    }

    @Test
    @DisplayName("Should generate different codes for different URLs")
    void testDifferentUrls() {
        Long workspaceId = 123456L;

        String code1 = ShortCodeGenerator.generateShortCode("http://example.com/page1", workspaceId, 0);
        String code2 = ShortCodeGenerator.generateShortCode("http://example.com/page2", workspaceId, 0);

        assertNotEquals(code1, code2, "Different URLs should produce different codes");
    }

    @Test
    @DisplayName("Should generate different codes for different workspaces")
    void testDifferentWorkspaces() {
        String url = "http://example.com/test";

        String code1 = ShortCodeGenerator.generateShortCode(url, 111L, 0);
        String code2 = ShortCodeGenerator.generateShortCode(url, 222L, 0);

        assertNotEquals(code1, code2, "Different workspaces should produce different codes");
    }

    @Test
    @DisplayName("Should generate different codes with different retry salts")
    void testRetrySalts() {
        String url = "http://example.com/test";
        Long workspaceId = 123456L;

        String code0 = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);
        String code1 = ShortCodeGenerator.generateShortCode(url, workspaceId, 1);
        String code2 = ShortCodeGenerator.generateShortCode(url, workspaceId, 2);

        assertNotEquals(code0, code1);
        assertNotEquals(code1, code2);
        assertNotEquals(code0, code2);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 8, 10, 12, 15, 20})
    @DisplayName("Should generate codes of specified lengths")
    void testCustomLengths(int length) {
        String url = "http://example.com/test";
        Long workspaceId = 123456L;

        String code = ShortCodeGenerator.generateShortCode(url, workspaceId, 0, length);

        assertEquals(length, code.length());
    }

    @Test
    @DisplayName("Should only contain Base58 characters")
    void testBase58Alphabet() {
        String url = "http://example.com/test";
        Long workspaceId = 123456L;

        String code = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);

        assertTrue(code.matches("[1-9A-HJ-NP-Za-km-z]+"),
            "Code should only contain Base58 characters");
        assertFalse(code.contains("0"), "Should not contain zero");
        assertFalse(code.contains("O"), "Should not contain capital O");
        assertFalse(code.contains("I"), "Should not contain capital I");
        assertFalse(code.contains("l"), "Should not contain lowercase l");
    }

    @Test
    @DisplayName("Should throw exception for null URL")
    void testNullUrl() {
        assertThrows(IllegalArgumentException.class,
            () -> ShortCodeGenerator.generateShortCode(null, 123L, 0));
    }

    @Test
    @DisplayName("Should throw exception for empty URL")
    void testEmptyUrl() {
        assertThrows(IllegalArgumentException.class,
            () -> ShortCodeGenerator.generateShortCode("", 123L, 0));
        assertThrows(IllegalArgumentException.class,
            () -> ShortCodeGenerator.generateShortCode("   ", 123L, 0));
    }

    @Test
    @DisplayName("Should throw exception for null workspace ID")
    void testNullWorkspaceId() {
        assertThrows(IllegalArgumentException.class,
            () -> ShortCodeGenerator.generateShortCode("http://example.com", null, 0));
    }

    @Test
    @DisplayName("Should throw exception for negative retry salt")
    void testNegativeRetrySalt() {
        assertThrows(IllegalArgumentException.class,
            () -> ShortCodeGenerator.generateShortCode("http://example.com", 123L, -1));
    }

    @Test
    @DisplayName("Should throw exception for non-positive code length")
    void testInvalidCodeLength() {
        String url = "http://example.com/test";
        Long workspaceId = 123L;

        assertThrows(IllegalArgumentException.class,
            () -> ShortCodeGenerator.generateShortCode(url, workspaceId, 0, 0));
        assertThrows(IllegalArgumentException.class,
            () -> ShortCodeGenerator.generateShortCode(url, workspaceId, 0, -1));
    }

    @Test
    @DisplayName("Should generate unique codes for batch of URLs")
    void testBatchUniqueness() {
        Long workspaceId = 123456L;
        Set<String> codes = new HashSet<>();

        // Generate codes for 100 different URLs
        for (int i = 0; i < 100; i++) {
            String url = "http://example.com/page/" + i;
            String code = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);
            codes.add(code);
        }

        // All codes should be unique
        assertEquals(100, codes.size(), "All generated codes should be unique");
    }

    @Test
    @DisplayName("Should handle URLs with query parameters")
    void testUrlsWithQueryParams() {
        Long workspaceId = 123456L;

        String code1 = ShortCodeGenerator.generateShortCode(
            "http://example.com/search?q=hello&page=1",
            workspaceId, 0);

        String code2 = ShortCodeGenerator.generateShortCode(
            "http://example.com/search?q=world&page=1",
            workspaceId, 0);

        assertNotEquals(code1, code2);
    }

    @Test
    @DisplayName("Should handle very long URLs")
    void testLongUrls() {
        String longPath = "a/".repeat(100);
        String url = "http://example.com/" + longPath + "resource";
        Long workspaceId = 123456L;

        String code = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);

        assertNotNull(code);
        assertEquals(10, code.length());
    }

    @Test
    @DisplayName("Should handle URLs with special characters")
    void testUrlsWithSpecialCharacters() {
        Long workspaceId = 123456L;

        String code = ShortCodeGenerator.generateShortCode(
            "http://example.com/path?name=John%20Doe&email=test@example.com",
            workspaceId, 0);

        assertNotNull(code);
        assertEquals(10, code.length());
    }

    @Test
    @DisplayName("Should handle large workspace IDs")
    void testLargeWorkspaceIds() {
        String url = "http://example.com/test";

        String code1 = ShortCodeGenerator.generateShortCode(url, Long.MAX_VALUE, 0);
        String code2 = ShortCodeGenerator.generateShortCode(url, Long.MAX_VALUE - 1, 0);

        assertNotEquals(code1, code2);
        assertEquals(10, code1.length());
        assertEquals(10, code2.length());
    }

    @Test
    @DisplayName("Should handle high retry salt values")
    void testHighRetrySalts() {
        String url = "http://example.com/test";
        Long workspaceId = 123L;

        String code1 = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);
        String code100 = ShortCodeGenerator.generateShortCode(url, workspaceId, 100);
        String code1000 = ShortCodeGenerator.generateShortCode(url, workspaceId, 1000);

        // All should be different
        assertNotEquals(code1, code100);
        assertNotEquals(code100, code1000);
        assertNotEquals(code1, code1000);
    }

    @Test
    @DisplayName("Should generate consistent codes across multiple calls")
    void testConsistencyAcrossMultipleCalls() {
        String url = "http://example.com/consistent";
        Long workspaceId = 999L;

        // Generate same code 10 times
        String firstCode = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);

        for (int i = 0; i < 10; i++) {
            String code = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);
            assertEquals(firstCode, code,
                "Code should be consistent across multiple generations");
        }
    }

    @ParameterizedTest
    @CsvSource({
        "'http://example.com/path1', 123, 'http://example.com/path2', 123",
        "'http://example.com/test', 111, 'http://example.com/test', 222",
        "'http://example.com/page', 100, 'http://different.com/page', 100"
    })
    @DisplayName("Should generate different codes for different URL-Workspace combinations")
    void testDifferentCombinations(String url1, Long ws1, String url2, Long ws2) {
        String code1 = ShortCodeGenerator.generateShortCode(url1, ws1, 0);
        String code2 = ShortCodeGenerator.generateShortCode(url2, ws2, 0);

        assertNotEquals(code1, code2);
    }

    @Test
    @DisplayName("Should be URL-safe")
    void testUrlSafe() {
        String url = "http://example.com/test";
        Long workspaceId = 123L;

        String code = ShortCodeGenerator.generateShortCode(url, workspaceId, 0);

        // Should not contain URL-unsafe characters
        assertFalse(code.contains(" "));
        assertFalse(code.contains("/"));
        assertFalse(code.contains("?"));
        assertFalse(code.contains("&"));
        assertFalse(code.contains("="));
        assertFalse(code.contains("+"));
        assertFalse(code.contains("#"));
    }

    @Test
    @DisplayName("Should handle HTTP and HTTPS URLs differently")
    void testHttpVsHttps() {
        Long workspaceId = 123L;

        String httpCode = ShortCodeGenerator.generateShortCode("http://example.com/test", workspaceId, 0);
        String httpsCode = ShortCodeGenerator.generateShortCode("https://example.com/test", workspaceId, 0);

        assertNotEquals(httpCode, httpsCode,
            "HTTP and HTTPS versions should produce different codes");
    }

    @Test
    @DisplayName("Should not allow instantiation")
    void testCannotInstantiate() {
        assertThrows(UnsupportedOperationException.class,
            () -> {
                var constructor = ShortCodeGenerator.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            },
            "Utility class should not be instantiable");
    }

    @Test
    @DisplayName("Should handle workspace ID 0")
    void testWorkspaceIdZero() {
        String url = "http://example.com/test";

        String code = ShortCodeGenerator.generateShortCode(url, 0L, 0);

        assertNotNull(code);
        assertEquals(10, code.length());
    }

    @Test
    @DisplayName("Should generate different codes for similar URLs with small differences")
    void testSimilarUrls() {
        Long workspaceId = 123L;

        String code1 = ShortCodeGenerator.generateShortCode("http://example.com/test1", workspaceId, 0);
        String code2 = ShortCodeGenerator.generateShortCode("http://example.com/test2", workspaceId, 0);
        String code3 = ShortCodeGenerator.generateShortCode("http://example.com/test", workspaceId, 0);

        // All should be different despite similarity
        assertNotEquals(code1, code2);
        assertNotEquals(code2, code3);
        assertNotEquals(code1, code3);
    }

    @Test
    @DisplayName("Should handle collision scenario with retry salt")
    void testCollisionHandlingWithSalt() {
        String url = "http://example.com/collision-test";
        Long workspaceId = 123456L;

        // Simulate collision handling by using different salts
        Set<String> codes = new HashSet<>();

        for (int salt = 0; salt < 10; salt++) {
            String code = ShortCodeGenerator.generateShortCode(url, workspaceId, salt);
            codes.add(code);
        }

        // All 10 codes should be unique
        assertEquals(10, codes.size(),
            "Different salts should produce different codes for collision handling");
    }
}
