package com.urlshort.util;

import com.urlshort.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Base58Encoder")
class Base58EncoderTest {

    @Nested
    @DisplayName("encode(long)")
    class EncodeLong {

        @Test
        @DisplayName("should return '1' when encoding zero")
        void shouldReturnOneForZero() {
            assertEquals("1", Base58Encoder.encode(0));
        }

        @Test
        @DisplayName("should return non-empty string for positive value")
        void shouldReturnNonEmptyForPositiveValue() {
            String result = Base58Encoder.encode(12345L);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("should return string containing only Base58 characters for positive value")
        void shouldReturnBase58CharactersOnly() {
            String result = Base58Encoder.encode(999999999L);
            String alphabet = Base58Encoder.getAlphabet();
            for (char c : result.toCharArray()) {
                assertTrue(alphabet.indexOf(c) >= 0,
                        "Character '" + c + "' is not in Base58 alphabet");
            }
        }

        @Test
        @DisplayName("should throw InvalidInputException for negative value")
        void shouldThrowForNegativeValue() {
            assertThrows(InvalidInputException.class, () -> Base58Encoder.encode(-1));
        }

        @Test
        @DisplayName("should throw InvalidInputException for Long.MIN_VALUE")
        void shouldThrowForLongMinValue() {
            assertThrows(InvalidInputException.class, () -> Base58Encoder.encode(Long.MIN_VALUE));
        }

        @Test
        @DisplayName("should encode Long.MAX_VALUE without error")
        void shouldEncodeLongMaxValue() {
            String result = Base58Encoder.encode(Long.MAX_VALUE);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("encode(byte[], int)")
    class EncodeHashWithLength {

        @Test
        @DisplayName("should throw InvalidInputException when hash is null")
        void shouldThrowWhenHashIsNull() {
            assertThrows(InvalidInputException.class, () -> Base58Encoder.encode(null, 10));
        }

        @Test
        @DisplayName("should throw InvalidInputException when hash is empty")
        void shouldThrowWhenHashIsEmpty() {
            assertThrows(InvalidInputException.class, () -> Base58Encoder.encode(new byte[0], 10));
        }

        @Test
        @DisplayName("should throw InvalidInputException when length is zero")
        void shouldThrowWhenLengthIsZero() {
            assertThrows(InvalidInputException.class,
                    () -> Base58Encoder.encode(new byte[]{1, 2, 3}, 0));
        }

        @Test
        @DisplayName("should throw InvalidInputException when length is negative")
        void shouldThrowWhenLengthIsNegative() {
            assertThrows(InvalidInputException.class,
                    () -> Base58Encoder.encode(new byte[]{1, 2, 3}, -1));
        }

        @Test
        @DisplayName("should return string of exact specified length")
        void shouldReturnStringOfExactLength() {
            byte[] hash = {0x7F, 0x3A, (byte) 0xBC, (byte) 0xDE, 0x12, 0x34, 0x56, 0x78,
                           (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0, 0x11, 0x22, 0x33, 0x44};
            assertEquals(10, Base58Encoder.encode(hash, 10).length());
            assertEquals(6, Base58Encoder.encode(hash, 6).length());
            assertEquals(12, Base58Encoder.encode(hash, 12).length());
        }

        @Test
        @DisplayName("should return string containing only Base58 characters")
        void shouldReturnBase58CharactersOnlyForHash() {
            byte[] hash = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
            String result = Base58Encoder.encode(hash, 10);
            String alphabet = Base58Encoder.getAlphabet();
            for (char c : result.toCharArray()) {
                assertTrue(alphabet.indexOf(c) >= 0,
                        "Character '" + c + "' is not in Base58 alphabet");
            }
        }

        @Test
        @DisplayName("should produce deterministic results for same input")
        void shouldProduceDeterministicResults() {
            byte[] hash = {0x7F, 0x3A, (byte) 0xBC, (byte) 0xDE, 0x12, 0x34, 0x56, 0x78};
            String first = Base58Encoder.encode(hash, 10);
            String second = Base58Encoder.encode(hash, 10);
            assertEquals(first, second);
        }
    }

    @Nested
    @DisplayName("encodeLarge(byte[], int)")
    class EncodeLarge {

        @Test
        @DisplayName("should throw InvalidInputException when hash is null")
        void shouldThrowWhenHashIsNull() {
            assertThrows(InvalidInputException.class, () -> Base58Encoder.encodeLarge(null, 10));
        }

        @Test
        @DisplayName("should throw InvalidInputException when hash is empty")
        void shouldThrowWhenHashIsEmpty() {
            assertThrows(InvalidInputException.class,
                    () -> Base58Encoder.encodeLarge(new byte[0], 10));
        }

        @Test
        @DisplayName("should throw InvalidInputException when length is zero")
        void shouldThrowWhenLengthIsZero() {
            assertThrows(InvalidInputException.class,
                    () -> Base58Encoder.encodeLarge(new byte[]{1, 2, 3}, 0));
        }

        @Test
        @DisplayName("should return string of exact specified length")
        void shouldReturnStringOfExactLength() {
            byte[] hash = {0x7F, 0x3A, (byte) 0xBC, (byte) 0xDE, 0x12, 0x34, 0x56, 0x78,
                           (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0, 0x11, 0x22, 0x33, 0x44};
            assertEquals(12, Base58Encoder.encodeLarge(hash, 12).length());
        }
    }

    @Nested
    @DisplayName("getAlphabet()")
    class GetAlphabet {

        @Test
        @DisplayName("should return alphabet with exactly 58 characters")
        void shouldReturnAlphabetWith58Characters() {
            assertEquals(58, Base58Encoder.getAlphabet().length());
        }

        @Test
        @DisplayName("should not contain ambiguous characters (0, O, I, l)")
        void shouldNotContainAmbiguousCharacters() {
            String alphabet = Base58Encoder.getAlphabet();
            assertFalse(alphabet.contains("0"), "Alphabet should not contain '0'");
            assertFalse(alphabet.contains("O"), "Alphabet should not contain 'O'");
            assertFalse(alphabet.contains("I"), "Alphabet should not contain 'I'");
            assertFalse(alphabet.contains("l"), "Alphabet should not contain 'l'");
        }
    }
}
