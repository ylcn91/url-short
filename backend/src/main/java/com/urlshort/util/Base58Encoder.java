package com.urlshort.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Static utility class for Base58 encoding operations.
 * <p>
 * This encoder uses a modified Base58 alphabet that excludes visually ambiguous characters
 * (0, O, I, l) to improve readability and reduce user confusion. The alphabet is:
 * "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
 * </p>
 * <p>
 * Base58 encoding is case-sensitive and URL-safe, making it ideal for URL shortening.
 * It provides higher entropy than Base62 while maintaining readability.
 * </p>
 * <p>
 * This class is thread-safe as all methods are static and work with immutable or locally-scoped data.
 * </p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Encode a long value
 * String encoded = Base58Encoder.encode(12345678901234L);
 * // Result: "3yQ3Ke1S5"
 *
 * // Encode from hash bytes with specific length
 * byte[] hash = MessageDigest.getInstance("SHA-256").digest("test".getBytes());
 * String shortCode = Base58Encoder.encode(hash, 10);
 * // Result: 10-character Base58 encoded string
 * }</pre>
 *
 * <h2>Algorithm Details:</h2>
 * <ul>
 *   <li>Alphabet: 58 characters (excludes 0, O, I, l)</li>
 *   <li>Encoding: Base58 positional notation</li>
 *   <li>Padding: Left-padded with '1' to reach target length</li>
 *   <li>Byte order: Big-endian for hash-to-integer conversion</li>
 * </ul>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Base58">Base58 Encoding</a>
 * @since 1.0
 */
public final class Base58Encoder {

    private static final Logger log = LoggerFactory.getLogger(Base58Encoder.class);

    /**
     * Base58 alphabet excluding visually ambiguous characters.
     * Excludes: 0 (zero), O (capital O), I (capital I), l (lowercase L)
     */
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    /**
     * Base value for Base58 encoding.
     */
    private static final int BASE = 58;

    /**
     * BigInteger representation of the base for efficient calculations.
     */
    private static final BigInteger BASE_BIG = BigInteger.valueOf(BASE);

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private Base58Encoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encodes a long value into a Base58 string.
     * <p>
     * This method converts the provided long value into its Base58 representation
     * using the modified alphabet. The result is not padded to any specific length.
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * long value = 3471844090L;
     * String encoded = Base58Encoder.encode(value);
     * // Returns: "1BQ9Bz"
     * }</pre>
     *
     * @param value the long value to encode (must be non-negative)
     * @return the Base58 encoded string, or "1" if value is 0
     * @throws IllegalArgumentException if value is negative
     */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative");
        }

        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder encoded = new StringBuilder();
        long remaining = value;

        while (remaining > 0) {
            int remainder = (int) (remaining % BASE);
            encoded.insert(0, ALPHABET.charAt(remainder));
            remaining = remaining / BASE;
        }

        log.debug("Encoded long value {} to Base58: {}", value, encoded);
        return encoded.toString();
    }

    /**
     * Encodes the first 8 bytes of a hash into a Base58 string of specified length.
     * <p>
     * This method is specifically designed for URL shortening. It takes a hash
     * (typically from SHA-256), extracts the first 8 bytes, converts them to
     * an unsigned long value in big-endian order, and encodes it in Base58.
     * The result is padded or truncated to the specified length.
     * </p>
     *
     * <h3>Algorithm:</h3>
     * <ol>
     *   <li>Extract first 8 bytes from hash (or fewer if hash is shorter)</li>
     *   <li>Convert to unsigned long using big-endian byte order</li>
     *   <li>Encode to Base58</li>
     *   <li>Left-pad with '1' if result is shorter than target length</li>
     *   <li>Truncate to target length if result is longer</li>
     * </ol>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * MessageDigest md = MessageDigest.getInstance("SHA-256");
     * byte[] hash = md.digest("http://example.com|ws_123".getBytes());
     * String shortCode = Base58Encoder.encode(hash, 10);
     * // Returns: 10-character Base58 string like "MaSgB7xKpQ"
     * }</pre>
     *
     * @param hash the hash bytes (typically 32 bytes from SHA-256)
     * @param length the desired length of the encoded string (typically 10)
     * @return Base58 encoded string of exactly the specified length
     * @throws IllegalArgumentException if hash is null, empty, or length is non-positive
     */
    public static String encode(byte[] hash, int length) {
        if (hash == null || hash.length == 0) {
            throw new IllegalArgumentException("Hash cannot be null or empty");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }

        // Take first 8 bytes (64 bits) from hash for standard encoding
        int bytesToUse = Math.min(8, hash.length);
        byte[] extracted = Arrays.copyOf(hash, bytesToUse);

        // Convert bytes to unsigned long (big-endian)
        long value = 0;
        for (int i = 0; i < extracted.length; i++) {
            value = (value << 8) | (extracted[i] & 0xFF);
        }

        // Encode to Base58
        String encoded = encode(value);

        // Pad to target length if necessary
        String result = padOrTruncate(encoded, length);

        log.debug("Encoded {} bytes of hash to Base58 with length {}: {}",
                  bytesToUse, length, result);

        return result;
    }

    /**
     * Pads the encoded string with leading '1's or truncates it to the target length.
     * <p>
     * If the encoded string is shorter than the target length, it is left-padded
     * with the first character of the alphabet ('1'). If it's longer, it is
     * truncated to the target length.
     * </p>
     *
     * @param encoded the Base58 encoded string
     * @param targetLength the desired length
     * @return the padded or truncated string
     */
    private static String padOrTruncate(String encoded, int targetLength) {
        if (encoded.length() == targetLength) {
            return encoded;
        }

        if (encoded.length() < targetLength) {
            // Pad with '1' (first character of alphabet)
            return String.valueOf(ALPHABET.charAt(0))
                    .repeat(targetLength - encoded.length()) + encoded;
        }

        // Truncate if longer
        return encoded.substring(0, targetLength);
    }

    /**
     * Alternative implementation using BigInteger for very large hash values.
     * <p>
     * This method can encode more than 8 bytes of hash data, useful for
     * generating longer codes or when higher collision resistance is needed.
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * byte[] hash = MessageDigest.getInstance("SHA-256").digest("test".getBytes());
     * // Use first 16 bytes for extended collision resistance
     * String extendedCode = Base58Encoder.encodeLarge(
     *     Arrays.copyOf(hash, 16), 12
     * );
     * // Returns: 12-character Base58 string
     * }</pre>
     *
     * @param hash the hash bytes to encode (can use any number of bytes)
     * @param length the desired length of the encoded string
     * @return Base58 encoded string of exactly the specified length
     * @throws IllegalArgumentException if hash is null, empty, or length is non-positive
     */
    public static String encodeLarge(byte[] hash, int length) {
        if (hash == null || hash.length == 0) {
            throw new IllegalArgumentException("Hash cannot be null or empty");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }

        // Convert byte array to BigInteger (unsigned, big-endian)
        BigInteger value = new BigInteger(1, hash);

        // Handle zero case
        if (value.equals(BigInteger.ZERO)) {
            return String.valueOf(ALPHABET.charAt(0)).repeat(length);
        }

        // Encode to Base58
        StringBuilder encoded = new StringBuilder();
        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = value.divideAndRemainder(BASE_BIG);
            encoded.insert(0, ALPHABET.charAt(divmod[1].intValue()));
            value = divmod[0];
        }

        // Pad or truncate to target length
        String result = padOrTruncate(encoded.toString(), length);

        log.debug("Encoded {} bytes of hash to Base58 (large) with length {}: {}",
                  hash.length, length, result);

        return result;
    }

    /**
     * Returns the Base58 alphabet used by this encoder.
     * <p>
     * This method is primarily useful for testing and validation purposes.
     * </p>
     *
     * @return the 58-character alphabet string
     */
    public static String getAlphabet() {
        return ALPHABET;
    }
}
