package com.urlshort.util;

import com.urlshort.domain.DeviceType;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing User-Agent strings to extract browser, OS, and device type.
 * This is a lightweight implementation without external dependencies.
 * For production use with high accuracy, consider using ua-parser-js or similar libraries.
 */
@Slf4j
public class UserAgentParser {

    private UserAgentParser() {
        // Utility class - prevent instantiation
    }

    /**
     * Parse result containing browser, OS, and device type information.
     */
    public record ParseResult(String browser, String os, DeviceType deviceType) {}

    /**
     * Parse a User-Agent string and extract browser, OS, and device type.
     *
     * @param userAgent the User-Agent string
     * @return ParseResult containing extracted information
     */
    public static ParseResult parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new ParseResult("Unknown", "Unknown", DeviceType.UNKNOWN);
        }

        String browser = detectBrowser(userAgent);
        String os = detectOS(userAgent);
        DeviceType deviceType = detectDeviceType(userAgent);

        return new ParseResult(browser, os, deviceType);
    }

    /**
     * Detect browser from User-Agent string.
     */
    private static String detectBrowser(String ua) {
        // Check for common bots first
        if (isBot(ua)) {
            return "Bot";
        }

        // Edge (must be checked before Chrome)
        if (ua.contains("Edg/")) {
            return extractVersion(ua, "Edg/", "Edge");
        }

        // Chrome (must be checked before Safari)
        if (ua.contains("Chrome/") && !ua.contains("Edg/")) {
            return extractVersion(ua, "Chrome/", "Chrome");
        }

        // Firefox
        if (ua.contains("Firefox/")) {
            return extractVersion(ua, "Firefox/", "Firefox");
        }

        // Safari (must be checked after Chrome)
        if (ua.contains("Safari/") && !ua.contains("Chrome/") && !ua.contains("Edg/")) {
            return extractVersion(ua, "Version/", "Safari");
        }

        // Opera
        if (ua.contains("OPR/") || ua.contains("Opera/")) {
            String prefix = ua.contains("OPR/") ? "OPR/" : "Opera/";
            return extractVersion(ua, prefix, "Opera");
        }

        // Internet Explorer
        if (ua.contains("MSIE") || ua.contains("Trident/")) {
            if (ua.contains("MSIE")) {
                return extractVersion(ua, "MSIE ", "IE");
            }
            return "IE 11";
        }

        return "Unknown";
    }

    /**
     * Detect operating system from User-Agent string.
     */
    private static String detectOS(String ua) {
        // Windows
        if (ua.contains("Windows NT 10.0")) return "Windows 10";
        if (ua.contains("Windows NT 6.3")) return "Windows 8.1";
        if (ua.contains("Windows NT 6.2")) return "Windows 8";
        if (ua.contains("Windows NT 6.1")) return "Windows 7";
        if (ua.contains("Windows")) return "Windows";

        // macOS
        if (ua.contains("Mac OS X")) {
            Pattern pattern = Pattern.compile("Mac OS X (\\d+[._]\\d+)");
            Matcher matcher = pattern.matcher(ua);
            if (matcher.find()) {
                String version = matcher.group(1).replace('_', '.');
                return "macOS " + version;
            }
            return "macOS";
        }

        // iOS
        if (ua.contains("iPhone")) return "iOS (iPhone)";
        if (ua.contains("iPad")) return "iOS (iPad)";
        if (ua.contains("iPod")) return "iOS (iPod)";

        // Android
        if (ua.contains("Android")) {
            Pattern pattern = Pattern.compile("Android (\\d+\\.\\d+)");
            Matcher matcher = pattern.matcher(ua);
            if (matcher.find()) {
                return "Android " + matcher.group(1);
            }
            return "Android";
        }

        // Linux
        if (ua.contains("Linux")) return "Linux";

        // Chrome OS
        if (ua.contains("CrOS")) return "Chrome OS";

        return "Unknown";
    }

    /**
     * Detect device type from User-Agent string.
     */
    private static DeviceType detectDeviceType(String ua) {
        // Check for bots first
        if (isBot(ua)) {
            return DeviceType.BOT;
        }

        // Mobile devices
        if (ua.contains("Mobile") || ua.contains("iPhone") || ua.contains("iPod") ||
            ua.contains("Android") && ua.contains("Mobile")) {
            return DeviceType.MOBILE;
        }

        // Tablets
        if (ua.contains("iPad") || ua.contains("Tablet") ||
            ua.contains("Android") && !ua.contains("Mobile")) {
            return DeviceType.TABLET;
        }

        // Default to desktop
        return DeviceType.DESKTOP;
    }

    /**
     * Check if User-Agent is a bot/crawler.
     */
    private static boolean isBot(String ua) {
        String lowerUA = ua.toLowerCase();
        return lowerUA.contains("bot") ||
               lowerUA.contains("crawl") ||
               lowerUA.contains("spider") ||
               lowerUA.contains("slurp") ||
               lowerUA.contains("mediapartners") ||
               lowerUA.contains("googlebot") ||
               lowerUA.contains("bingbot") ||
               lowerUA.contains("facebookexternalhit");
    }

    /**
     * Extract version number from User-Agent.
     */
    private static String extractVersion(String ua, String prefix, String name) {
        int index = ua.indexOf(prefix);
        if (index == -1) {
            return name;
        }

        int start = index + prefix.length();
        int end = start;

        // Find end of version number
        while (end < ua.length() && (Character.isDigit(ua.charAt(end)) || ua.charAt(end) == '.')) {
            end++;
        }

        if (end > start) {
            String version = ua.substring(start, end);
            // Take only major.minor version
            String[] parts = version.split("\\.");
            if (parts.length >= 2) {
                return name + " " + parts[0] + "." + parts[1];
            } else if (parts.length == 1) {
                return name + " " + parts[0];
            }
        }

        return name;
    }
}
