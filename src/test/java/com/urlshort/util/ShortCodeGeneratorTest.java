package com.urlshort.util;

import com.urlshort.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShortCodeGenerator")
class ShortCodeGeneratorTest {

    private static final String SAMPLE_URL = "http://example.com/path";
    private static final Long WORKSPACE_ID = 123456L;

    @Nested
    @DisplayName("generateShortCode() - determinism")
    class Determinism {

        @Test
        @DisplayName("should produce the same code for identical inputs")
        void shouldProduceSameCodeForIdenticalInputs() {
            String code1 = ShortCodeGenerator.generateShortCode(SAMPLE_URL, WORKSPACE_ID, 0);
            String code2 = ShortCodeGenerator.generateShortCode(SAMPLE_URL, WORKSPACE_ID, 0);
            assertEquals(code1, code2);
        }

        @Test
        @DisplayName("should produce the same code across multiple invocations")
        void shouldProduceSameCodeAcrossMultipleInvocations() {
            String first = ShortCodeGenerator.generateShortCode(SAMPLE_URL, WORKSPACE_ID, 0);
            for (int i = 0; i < 10; i++) {
                assertEquals(first,
                        ShortCodeGenerator.generateShortCode(SAMPLE_URL, WORKSPACE_ID, 0));
            }
        }
    }

    @Nested
    @DisplayName("generateShortCode() - different workspace IDs")
    class DifferentWorkspaceIds {

        @Test
        @DisplayName("should produce different codes for different workspace IDs")
        void shouldProduceDifferentCodesForDifferentWorkspaces() {
            String code1 = ShortCodeGenerator.generateShortCode(SAMPLE_URL, 1L, 0);
            String code2 = ShortCodeGenerator.generateShortCode(SAMPLE_URL, 2L, 0);
            assertNotEquals(code1, code2);
        }
    }

    @Nested
    @DisplayName("generateShortCode() - different retry salts")
    class DifferentRetrySalts {

        @Test
        @DisplayName("should produce different codes for different retry salts")
        void shouldProduceDifferentCodesForDifferentSalts() {
            String code0 = ShortCodeGenerator.generateShortCode(SAMPLE_URL, WORKSPACE_ID, 0);
            String code1 = ShortCodeGenerator.generateShortCode(SAMPLE_URL, WORKSPACE_ID, 1);
            String code2 = ShortCodeGenerator.generateShortCode(SAMPLE_URL, WORKSPACE_ID, 2);
            assertNotEquals(code0, code1);
            assertNotEquals(code1, code2);
            assertNotEquals(code0, code2);
        }
    }

    @Nested
    @DisplayName("generateShortCode() - code length")
    class CodeLength {

        @Test
        @DisplayName("should generate a code of default length 10")
        void shouldGenerateCodeOfDefaultLength() {
            String code = ShortCodeGenerator.generateShortCode(SAMPLE_URL, WORKSPACE_ID, 0);
            assertEquals(10, code.length());
        }

        @Test
        @DisplayName("should generate a code of custom length")
        void shouldGenerateCodeOfCustomLength() {
            String code = ShortCodeGenerator.generateShortCode(SAMPLE_URL, WORKSPACE_ID, 0, 12);
            assertEquals(12, code.length());
        }

        @Test
        @DisplayName("getDefaultCodeLength should return 10")
        void shouldReturnDefaultCodeLength() {
            assertEquals(10, ShortCodeGenerator.getDefaultCodeLength());
        }
    }

    @Nested
    @DisplayName("generateShortCode() - input validation")
    class InputValidation {

        @Test
        @DisplayName("should throw InvalidInputException when URL is null")
        void shouldThrowWhenUrlIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> ShortCodeGenerator.generateShortCode(null, WORKSPACE_ID, 0));
        }

        @Test
        @DisplayName("should throw InvalidInputException when URL is empty")
        void shouldThrowWhenUrlIsEmpty() {
            assertThrows(InvalidInputException.class,
                    () -> ShortCodeGenerator.generateShortCode("", WORKSPACE_ID, 0));
        }

        @Test
        @DisplayName("should throw InvalidInputException when workspaceId is null")
        void shouldThrowWhenWorkspaceIdIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> ShortCodeGenerator.generateShortCode(SAMPLE_URL, null, 0));
        }

        @Test
        @DisplayName("should throw InvalidInputException when retry salt is negative")
        void shouldThrowWhenRetrySaltIsNegative() {
            assertThrows(InvalidInputException.class,
                    () -> ShortCodeGenerator.generateShortCode(SAMPLE_URL, WORKSPACE_ID, -1));
        }
    }

    @Nested
    @DisplayName("generateShortCodesWithRetries()")
    class GenerateWithRetries {

        @Test
        @DisplayName("should return the correct number of codes")
        void shouldReturnCorrectNumberOfCodes() {
            String[] codes = ShortCodeGenerator.generateShortCodesWithRetries(
                    SAMPLE_URL, WORKSPACE_ID, 5);
            assertEquals(5, codes.length);
        }

        @Test
        @DisplayName("should return all unique codes")
        void shouldReturnAllUniqueCodes() {
            String[] codes = ShortCodeGenerator.generateShortCodesWithRetries(
                    SAMPLE_URL, WORKSPACE_ID, 5);
            long uniqueCount = java.util.Arrays.stream(codes).distinct().count();
            assertEquals(5, uniqueCount);
        }

        @Test
        @DisplayName("should throw InvalidInputException when maxRetries is zero")
        void shouldThrowWhenMaxRetriesIsZero() {
            assertThrows(InvalidInputException.class,
                    () -> ShortCodeGenerator.generateShortCodesWithRetries(
                            SAMPLE_URL, WORKSPACE_ID, 0));
        }

        @Test
        @DisplayName("should throw InvalidInputException when maxRetries is negative")
        void shouldThrowWhenMaxRetriesIsNegative() {
            assertThrows(InvalidInputException.class,
                    () -> ShortCodeGenerator.generateShortCodesWithRetries(
                            SAMPLE_URL, WORKSPACE_ID, -1));
        }
    }

    @Nested
    @DisplayName("generateShortCodeFromUrl()")
    class GenerateFromUrl {

        @Test
        @DisplayName("should generate code from raw URL after canonicalization")
        void shouldGenerateCodeFromRawUrl() {
            String code = ShortCodeGenerator.generateShortCodeFromUrl(
                    "HTTP://Example.COM/path", WORKSPACE_ID, 0);
            assertNotNull(code);
            assertEquals(10, code.length());
        }

        @Test
        @DisplayName("should produce same code for semantically equivalent URLs")
        void shouldProduceSameCodeForEquivalentUrls() {
            String code1 = ShortCodeGenerator.generateShortCodeFromUrl(
                    "HTTP://Example.COM:80/path/", WORKSPACE_ID, 0);
            String code2 = ShortCodeGenerator.generateShortCodeFromUrl(
                    "http://example.com/path", WORKSPACE_ID, 0);
            assertEquals(code1, code2);
        }
    }
}
