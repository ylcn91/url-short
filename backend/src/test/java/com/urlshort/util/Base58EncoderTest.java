package com.urlshort.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for Base58Encoder utility class.
 * Tests all encoding methods with various inputs including edge cases.
 */
@DisplayName("Base58Encoder Tests")
class Base58EncoderTest {

    @Test
    @DisplayName("Should encode zero as first alphabet character")
    void testEncodeZero() {
        String result = Base58Encoder.encode(0L);
        assertEquals("1", result, "Zero should encode to '1'");
    }

    @Test
    @DisplayName("Should encode positive long values correctly")
    void testEncodePositiveLong() {
        // Test known value
        String result = Base58Encoder.encode(3471844090L);
        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertTrue(result.matches("[1-9A-HJ-NP-Za-km-z]+"),
            "Result should only contain Base58 characters");
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1",
        "57, z",
        "58, 21",
        "100, 1d",
        "1000, HD",
        "10000, 3yQ"
    })
    @DisplayName("Should encode known values correctly")
    void testEncodeKnownValues(long input, String expected) {
        String result = Base58Encoder.encode(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should throw exception for negative long value")
    void testEncodeNegativeLong() {
        assertThrows(IllegalArgumentException.class,
            () -> Base58Encoder.encode(-1L),
            "Should reject negative values");
    }

    @Test
    @DisplayName("Should encode hash bytes to specific length")
    void testEncodeHashWithLength() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest("test".getBytes());

        String result = Base58Encoder.encode(hash, 10);

        assertNotNull(result);
        assertEquals(10, result.length(), "Result should be exactly 10 characters");
        assertTrue(result.matches("[1-9A-HJ-NP-Za-km-z]+"),
            "Result should only contain Base58 characters");
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 8, 10, 12, 15, 20})
    @DisplayName("Should generate codes of specified lengths")
    void testEncodeDifferentLengths(int length) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest("example".getBytes());

        String result = Base58Encoder.encode(hash, length);

        assertEquals(length, result.length());
        assertTrue(result.matches("[1-9A-HJ-NP-Za-km-z]+"));
    }

    @Test
    @DisplayName("Should be deterministic for same input")
    void testDeterminism() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest("consistent".getBytes());

        String first = Base58Encoder.encode(hash, 10);
        String second = Base58Encoder.encode(hash, 10);

        assertEquals(first, second, "Same input should produce same output");
    }

    @Test
    @DisplayName("Should produce different codes for different inputs")
    void testUniqueness() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[] hash1 = md.digest("url1".getBytes());
        byte[] hash2 = md.digest("url2".getBytes());

        String code1 = Base58Encoder.encode(hash1, 10);
        String code2 = Base58Encoder.encode(hash2, 10);

        assertNotEquals(code1, code2, "Different inputs should produce different codes");
    }

    @Test
    @DisplayName("Should throw exception for null hash")
    void testEncodeNullHash() {
        assertThrows(IllegalArgumentException.class,
            () -> Base58Encoder.encode(null, 10),
            "Should reject null hash");
    }

    @Test
    @DisplayName("Should throw exception for empty hash")
    void testEncodeEmptyHash() {
        assertThrows(IllegalArgumentException.class,
            () -> Base58Encoder.encode(new byte[0], 10),
            "Should reject empty hash");
    }

    @Test
    @DisplayName("Should throw exception for non-positive length")
    void testEncodeInvalidLength() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest("test".getBytes());

        assertThrows(IllegalArgumentException.class,
            () -> Base58Encoder.encode(hash, 0));
        assertThrows(IllegalArgumentException.class,
            () -> Base58Encoder.encode(hash, -1));
    }

    @Test
    @DisplayName("Should handle short hash bytes")
    void testEncodeShortHash() {
        byte[] shortHash = new byte[]{1, 2, 3, 4};
        String result = Base58Encoder.encode(shortHash, 10);

        assertNotNull(result);
        assertEquals(10, result.length());
    }

    @Test
    @DisplayName("Should pad with leading 1s when result is short")
    void testPadding() {
        byte[] smallHash = new byte[]{0, 0, 0, 0, 0, 0, 0, 1};
        String result = Base58Encoder.encode(smallHash, 10);

        assertEquals(10, result.length());
        assertTrue(result.startsWith("1"), "Should pad with leading 1s");
    }

    @Test
    @DisplayName("Should not contain ambiguous characters")
    void testNoAmbiguousCharacters() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // Test multiple URLs to ensure no ambiguous characters appear
        for (int i = 0; i < 100; i++) {
            byte[] hash = md.digest(("url" + i).getBytes());
            String code = Base58Encoder.encode(hash, 10);

            assertFalse(code.contains("0"), "Should not contain zero");
            assertFalse(code.contains("O"), "Should not contain capital O");
            assertFalse(code.contains("I"), "Should not contain capital I");
            assertFalse(code.contains("l"), "Should not contain lowercase l");
        }
    }

    @Test
    @DisplayName("Should encode large hash values correctly")
    void testEncodeLarge() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest("large_test".getBytes());

        // Use first 16 bytes instead of 8
        byte[] largeHash = new byte[16];
        System.arraycopy(hash, 0, largeHash, 0, 16);

        String result = Base58Encoder.encodeLarge(largeHash, 12);

        assertNotNull(result);
        assertEquals(12, result.length());
        assertTrue(result.matches("[1-9A-HJ-NP-Za-km-z]+"));
    }

    @Test
    @DisplayName("Should return correct alphabet")
    void testGetAlphabet() {
        String alphabet = Base58Encoder.getAlphabet();

        assertNotNull(alphabet);
        assertEquals(58, alphabet.length());
        assertFalse(alphabet.contains("0"));
        assertFalse(alphabet.contains("O"));
        assertFalse(alphabet.contains("I"));
        assertFalse(alphabet.contains("l"));
        assertTrue(alphabet.startsWith("1"));
    }

    @Test
    @DisplayName("Should not allow instantiation")
    void testCannotInstantiate() {
        assertThrows(UnsupportedOperationException.class,
            () -> {
                var constructor = Base58Encoder.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            },
            "Utility class should not be instantiable");
    }

    @Test
    @DisplayName("Should handle maximum long value")
    void testMaxLongValue() {
        String result = Base58Encoder.encode(Long.MAX_VALUE);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    @DisplayName("Should be URL-safe")
    void testUrlSafe() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest("url_safe_test".getBytes());

        String code = Base58Encoder.encode(hash, 10);

        // Base58 should not contain characters that need URL encoding
        assertFalse(code.contains(" "));
        assertFalse(code.contains("/"));
        assertFalse(code.contains("?"));
        assertFalse(code.contains("&"));
        assertFalse(code.contains("="));
        assertFalse(code.contains("+"));
    }
}
