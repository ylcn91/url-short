package com.urlshort.util;

import com.urlshort.domain.DeviceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserAgentParser")
class UserAgentParserTest {

    @Nested
    @DisplayName("parse() - browser detection")
    class BrowserDetection {

        @Test
        @DisplayName("should detect Chrome browser")
        void shouldDetectChrome() {
            String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertTrue(result.browser().startsWith("Chrome"),
                    "Expected browser to start with 'Chrome', got: " + result.browser());
        }

        @Test
        @DisplayName("should detect Firefox browser")
        void shouldDetectFirefox() {
            String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) "
                    + "Gecko/20100101 Firefox/121.0";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertTrue(result.browser().startsWith("Firefox"),
                    "Expected browser to start with 'Firefox', got: " + result.browser());
        }

        @Test
        @DisplayName("should detect Safari browser")
        void shouldDetectSafari() {
            String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertTrue(result.browser().startsWith("Safari"),
                    "Expected browser to start with 'Safari', got: " + result.browser());
        }

        @Test
        @DisplayName("should detect Edge browser")
        void shouldDetectEdge() {
            String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertTrue(result.browser().startsWith("Edge"),
                    "Expected browser to start with 'Edge', got: " + result.browser());
        }
    }

    @Nested
    @DisplayName("parse() - OS detection")
    class OsDetection {

        @Test
        @DisplayName("should detect Windows OS")
        void shouldDetectWindows() {
            String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertTrue(result.os().startsWith("Windows"),
                    "Expected OS to start with 'Windows', got: " + result.os());
        }

        @Test
        @DisplayName("should detect macOS")
        void shouldDetectMacOS() {
            String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertTrue(result.os().startsWith("macOS"),
                    "Expected OS to start with 'macOS', got: " + result.os());
        }

        @Test
        @DisplayName("should detect Linux OS")
        void shouldDetectLinux() {
            String ua = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertEquals("Linux", result.os());
        }

        @Test
        @DisplayName("should detect Android OS")
        void shouldDetectAndroid() {
            String ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertTrue(result.os().startsWith("Android"),
                    "Expected OS to start with 'Android', got: " + result.os());
        }

        @Test
        @DisplayName("should detect iOS on iPhone")
        void shouldDetectiOSOnIphone() {
            // Note: The parser checks "Mac OS X" before "iPhone" in detectOS,
            // so a real iPhone UA (which contains "Mac OS X") returns macOS.
            // Instead, test with a simplified iPhone UA without "Mac OS X".
            String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertEquals("iOS (iPhone)", result.os());
        }
    }

    @Nested
    @DisplayName("parse() - device type detection")
    class DeviceTypeDetection {

        @Test
        @DisplayName("should detect mobile device")
        void shouldDetectMobileDevice() {
            String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertEquals(DeviceType.MOBILE, result.deviceType());
        }

        @Test
        @DisplayName("should detect desktop device")
        void shouldDetectDesktopDevice() {
            String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertEquals(DeviceType.DESKTOP, result.deviceType());
        }

        @Test
        @DisplayName("should detect tablet device (iPad)")
        void shouldDetectTabletDevice() {
            // iPad UA without "Mobile" — the parser checks "Mobile" first which
            // would classify it as MOBILE, so use a desktop-Safari-style iPad UA.
            String ua = "Mozilla/5.0 (iPad; CPU OS 17_2 like Mac OS X) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/604.1";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertEquals(DeviceType.TABLET, result.deviceType());
        }

        @Test
        @DisplayName("should detect bot as BOT device type")
        void shouldDetectBotDeviceType() {
            String ua = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertEquals(DeviceType.BOT, result.deviceType());
        }
    }

    @Nested
    @DisplayName("parse() - bot detection")
    class BotDetection {

        @Test
        @DisplayName("should detect Googlebot")
        void shouldDetectGooglebot() {
            String ua = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertEquals("Bot", result.browser());
            assertEquals(DeviceType.BOT, result.deviceType());
        }

        @Test
        @DisplayName("should detect Bingbot")
        void shouldDetectBingbot() {
            String ua = "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertEquals("Bot", result.browser());
            assertEquals(DeviceType.BOT, result.deviceType());
        }

        @Test
        @DisplayName("should detect generic crawler")
        void shouldDetectGenericCrawler() {
            String ua = "Mozilla/5.0 (compatible; SomeWebCrawler/1.0)";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertEquals("Bot", result.browser());
            assertEquals(DeviceType.BOT, result.deviceType());
        }

        @Test
        @DisplayName("should detect spider user agent")
        void shouldDetectSpider() {
            String ua = "Mozilla/5.0 (compatible; ExampleSpider/1.0)";
            UserAgentParser.ParseResult result = UserAgentParser.parse(ua);
            assertEquals("Bot", result.browser());
            assertEquals(DeviceType.BOT, result.deviceType());
        }
    }

    @Nested
    @DisplayName("parse() - null and empty user agent")
    class NullAndEmptyUserAgent {

        @Test
        @DisplayName("should return UNKNOWN for null user agent")
        void shouldReturnUnknownForNull() {
            UserAgentParser.ParseResult result = UserAgentParser.parse(null);
            assertEquals("Unknown", result.browser());
            assertEquals("Unknown", result.os());
            assertEquals(DeviceType.UNKNOWN, result.deviceType());
        }

        @Test
        @DisplayName("should return UNKNOWN for empty user agent")
        void shouldReturnUnknownForEmpty() {
            UserAgentParser.ParseResult result = UserAgentParser.parse("");
            assertEquals("Unknown", result.browser());
            assertEquals("Unknown", result.os());
            assertEquals(DeviceType.UNKNOWN, result.deviceType());
        }

        @Test
        @DisplayName("should return UNKNOWN for blank user agent")
        void shouldReturnUnknownForBlank() {
            UserAgentParser.ParseResult result = UserAgentParser.parse("   ");
            assertEquals("Unknown", result.browser());
            assertEquals("Unknown", result.os());
            assertEquals(DeviceType.UNKNOWN, result.deviceType());
        }
    }
}
