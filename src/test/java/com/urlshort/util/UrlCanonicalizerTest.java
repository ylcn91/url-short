package com.urlshort.util;

import com.urlshort.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UrlCanonicalizer")
class UrlCanonicalizerTest {

    @Nested
    @DisplayName("canonicalize() - null and empty input")
    class NullAndEmptyInput {

        @Test
        @DisplayName("should throw InvalidInputException when URL is null")
        void shouldThrowWhenUrlIsNull() {
            InvalidInputException ex = assertThrows(InvalidInputException.class,
                    () -> UrlCanonicalizer.canonicalize(null));
            assertTrue(ex.getMessage().contains("null"));
        }

        @Test
        @DisplayName("should throw InvalidInputException when URL is empty")
        void shouldThrowWhenUrlIsEmpty() {
            InvalidInputException ex = assertThrows(InvalidInputException.class,
                    () -> UrlCanonicalizer.canonicalize(""));
            assertTrue(ex.getMessage().contains("empty"));
        }

        @Test
        @DisplayName("should throw InvalidInputException when URL is blank (whitespace only)")
        void shouldThrowWhenUrlIsBlank() {
            assertThrows(InvalidInputException.class,
                    () -> UrlCanonicalizer.canonicalize("   "));
        }
    }

    @Nested
    @DisplayName("canonicalize() - scheme normalization")
    class SchemeNormalization {

        @Test
        @DisplayName("should lowercase HTTP scheme")
        void shouldLowercaseHttpScheme() {
            String result = UrlCanonicalizer.canonicalize("HTTP://example.com/path");
            assertTrue(result.startsWith("http://"));
        }

        @Test
        @DisplayName("should lowercase HTTPS scheme")
        void shouldLowercaseHttpsScheme() {
            String result = UrlCanonicalizer.canonicalize("HTTPS://example.com/path");
            assertTrue(result.startsWith("https://"));
        }

        @Test
        @DisplayName("should lowercase mixed-case scheme")
        void shouldLowercaseMixedCaseScheme() {
            String result = UrlCanonicalizer.canonicalize("HtTp://example.com/path");
            assertTrue(result.startsWith("http://"));
        }
    }

    @Nested
    @DisplayName("canonicalize() - host lowercasing")
    class HostLowercasing {

        @Test
        @DisplayName("should lowercase uppercase host")
        void shouldLowercaseUppercaseHost() {
            String result = UrlCanonicalizer.canonicalize("http://EXAMPLE.COM/path");
            assertEquals("http://example.com/path", result);
        }

        @Test
        @DisplayName("should lowercase mixed-case host")
        void shouldLowercaseMixedCaseHost() {
            String result = UrlCanonicalizer.canonicalize("http://Example.Com/path");
            assertEquals("http://example.com/path", result);
        }
    }

    @Nested
    @DisplayName("canonicalize() - default port removal")
    class DefaultPortRemoval {

        @Test
        @DisplayName("should remove port 80 for http")
        void shouldRemovePort80ForHttp() {
            String result = UrlCanonicalizer.canonicalize("http://example.com:80/path");
            assertEquals("http://example.com/path", result);
        }

        @Test
        @DisplayName("should remove port 443 for https")
        void shouldRemovePort443ForHttps() {
            String result = UrlCanonicalizer.canonicalize("https://example.com:443/path");
            assertEquals("https://example.com/path", result);
        }

        @Test
        @DisplayName("should keep non-default port for http")
        void shouldKeepNonDefaultPortForHttp() {
            String result = UrlCanonicalizer.canonicalize("http://example.com:8080/path");
            assertEquals("http://example.com:8080/path", result);
        }

        @Test
        @DisplayName("should keep non-default port for https")
        void shouldKeepNonDefaultPortForHttps() {
            String result = UrlCanonicalizer.canonicalize("https://example.com:8443/path");
            assertEquals("https://example.com:8443/path", result);
        }
    }

    @Nested
    @DisplayName("canonicalize() - trailing slash removal")
    class TrailingSlashRemoval {

        @Test
        @DisplayName("should remove trailing slash from path")
        void shouldRemoveTrailingSlashFromPath() {
            String result = UrlCanonicalizer.canonicalize("http://example.com/path/");
            assertEquals("http://example.com/path", result);
        }

        @Test
        @DisplayName("should keep root path slash")
        void shouldKeepRootPathSlash() {
            String result = UrlCanonicalizer.canonicalize("http://example.com/");
            assertEquals("http://example.com/", result);
        }
    }

    @Nested
    @DisplayName("canonicalize() - query parameter sorting")
    class QueryParameterSorting {

        @Test
        @DisplayName("should sort query parameters alphabetically")
        void shouldSortQueryParametersAlphabetically() {
            String result = UrlCanonicalizer.canonicalize("http://example.com/path?z=1&a=2&m=3");
            assertEquals("http://example.com/path?a=2&m=3&z=1", result);
        }

        @Test
        @DisplayName("should preserve already-sorted query parameters")
        void shouldPreserveAlreadySortedQueryParameters() {
            String result = UrlCanonicalizer.canonicalize("http://example.com/path?a=1&b=2&c=3");
            assertEquals("http://example.com/path?a=1&b=2&c=3", result);
        }

        @Test
        @DisplayName("should handle single query parameter")
        void shouldHandleSingleQueryParameter() {
            String result = UrlCanonicalizer.canonicalize("http://example.com/path?key=value");
            assertEquals("http://example.com/path?key=value", result);
        }
    }

    @Nested
    @DisplayName("canonicalize() - fragment removal")
    class FragmentRemoval {

        @Test
        @DisplayName("should remove fragment from URL")
        void shouldRemoveFragment() {
            String result = UrlCanonicalizer.canonicalize("http://example.com/page#section");
            assertEquals("http://example.com/page", result);
        }

        @Test
        @DisplayName("should remove fragment and keep query parameters")
        void shouldRemoveFragmentAndKeepQuery() {
            String result = UrlCanonicalizer.canonicalize("http://example.com/page?a=1#section");
            assertEquals("http://example.com/page?a=1", result);
        }
    }

    @Nested
    @DisplayName("canonicalize() - multiple slash collapsing")
    class MultipleSlashCollapsing {

        @Test
        @DisplayName("should collapse double slashes in path")
        void shouldCollapseDoubleSlashes() {
            String result = UrlCanonicalizer.canonicalize("http://example.com//a//b");
            assertEquals("http://example.com/a/b", result);
        }

        @Test
        @DisplayName("should collapse triple slashes in path")
        void shouldCollapseTripleSlashes() {
            String result = UrlCanonicalizer.canonicalize("http://example.com///a///b");
            assertEquals("http://example.com/a/b", result);
        }
    }

    @Nested
    @DisplayName("canonicalize() - idempotency")
    class Idempotency {

        @Test
        @DisplayName("should be idempotent: canonicalize(canonicalize(x)) == canonicalize(x)")
        void shouldBeIdempotent() {
            String url = "HTTP://Example.COM:80/path///to///page/?z=1&a=2#frag";
            String once = UrlCanonicalizer.canonicalize(url);
            String twice = UrlCanonicalizer.canonicalize(once);
            assertEquals(once, twice);
        }

        @Test
        @DisplayName("should be idempotent for simple URL")
        void shouldBeIdempotentForSimpleUrl() {
            String url = "http://example.com/path";
            String once = UrlCanonicalizer.canonicalize(url);
            String twice = UrlCanonicalizer.canonicalize(once);
            assertEquals(once, twice);
        }
    }

    @Nested
    @DisplayName("isValid()")
    class IsValid {

        @Test
        @DisplayName("should return true for valid HTTP URL")
        void shouldReturnTrueForValidHttpUrl() {
            assertTrue(UrlCanonicalizer.isValid("http://example.com"));
        }

        @Test
        @DisplayName("should return true for valid HTTPS URL")
        void shouldReturnTrueForValidHttpsUrl() {
            assertTrue(UrlCanonicalizer.isValid("https://example.com/path"));
        }

        @Test
        @DisplayName("should return false for null URL")
        void shouldReturnFalseForNull() {
            assertFalse(UrlCanonicalizer.isValid(null));
        }

        @Test
        @DisplayName("should return false for empty URL")
        void shouldReturnFalseForEmpty() {
            assertFalse(UrlCanonicalizer.isValid(""));
        }

        @Test
        @DisplayName("should return false for invalid URL")
        void shouldReturnFalseForInvalidUrl() {
            assertFalse(UrlCanonicalizer.isValid("not a url ://[invalid"));
        }
    }

    @Nested
    @DisplayName("canonicalize() - unsupported scheme")
    class UnsupportedScheme {

        @Test
        @DisplayName("should throw InvalidInputException for ftp scheme")
        void shouldThrowForFtpScheme() {
            assertThrows(InvalidInputException.class,
                    () -> UrlCanonicalizer.canonicalize("ftp://example.com/file"));
        }

        @Test
        @DisplayName("should throw InvalidInputException for file scheme")
        void shouldThrowForFileScheme() {
            assertThrows(InvalidInputException.class,
                    () -> UrlCanonicalizer.canonicalize("file:///etc/passwd"));
        }
    }
}
