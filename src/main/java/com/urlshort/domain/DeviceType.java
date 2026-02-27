package com.urlshort.domain;

/**
 * Device type enumeration for click event analytics.
 * Categorizes the type of device used to access a short link.
 */
public enum DeviceType {
    /**
     * Desktop computer (Windows, Mac, Linux).
     */
    DESKTOP,

    /**
     * Mobile phone (iOS, Android, etc.).
     */
    MOBILE,

    /**
     * Tablet device (iPad, Android tablets, etc.).
     */
    TABLET,

    /**
     * Automated bot or crawler.
     */
    BOT,

    /**
     * Unknown or unrecognized device type.
     */
    UNKNOWN
}
