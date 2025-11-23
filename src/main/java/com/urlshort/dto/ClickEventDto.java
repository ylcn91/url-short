package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for Click Event payloads sent to Kafka.
 *
 * This DTO represents the event structure for click tracking published to Kafka.
 * It contains all necessary information for analytics and is serialized to JSON.
 *
 * Event Schema matches the Kafka decision document specification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventDto {

    /**
     * Unique identifier for this click event (UUID).
     */
    @NotBlank(message = "Click ID is required")
    @JsonProperty("click_id")
    private String clickId;

    /**
     * ID of the short link that was clicked.
     */
    @NotNull(message = "Short link ID is required")
    @JsonProperty("short_link_id")
    private Long shortLinkId;

    /**
     * Workspace ID for multi-tenant isolation.
     */
    @NotNull(message = "Workspace ID is required")
    @JsonProperty("workspace_id")
    private Long workspaceId;

    /**
     * Timestamp when the click occurred (ISO-8601 format).
     */
    @NotNull(message = "Timestamp is required")
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Client IP address (may be anonymized for GDPR compliance).
     */
    @JsonProperty("ip")
    private String ip;

    /**
     * User agent string from the HTTP request.
     */
    @JsonProperty("user_agent")
    private String userAgent;

    /**
     * HTTP referer header (referring URL).
     */
    @JsonProperty("referrer")
    private String referrer;

    /**
     * ISO 3166-1 alpha-2 country code (e.g., "US", "UK").
     */
    @JsonProperty("country")
    private String country;

    /**
     * City name derived from IP geolocation.
     */
    @JsonProperty("city")
    private String city;

    /**
     * Device type (desktop, mobile, tablet, bot, unknown).
     */
    @JsonProperty("device_type")
    private String deviceType;

    /**
     * Browser name extracted from user agent.
     */
    @JsonProperty("browser")
    private String browser;

    /**
     * Operating system extracted from user agent.
     */
    @JsonProperty("os")
    private String os;

    /**
     * Original URL that the short link points to.
     */
    @JsonProperty("original_url")
    private String originalUrl;

    /**
     * Short code used for the redirect.
     */
    @JsonProperty("short_code")
    private String shortCode;

    /**
     * Event schema version for backward compatibility.
     */
    @Builder.Default
    @JsonProperty("version")
    private String version = "1";
}
