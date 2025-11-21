package com.urlshort.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

/**
 * Click event entity for analytics and tracking data.
 *
 * Records each click on a short link with detailed information about the visitor.
 * This entity uses hard delete strategy (not soft delete) as it's high-volume
 * time-series data. Consider table partitioning by date for optimal performance.
 */
@Entity
@Table(
    name = "click_event",
    indexes = {
        @Index(name = "idx_click_event_short_link_clicked_at", columnList = "short_link_id, clicked_at"),
        @Index(name = "idx_click_event_clicked_at", columnList = "clicked_at"),
        @Index(name = "idx_click_event_country", columnList = "short_link_id, country"),
        @Index(name = "idx_click_event_device_type", columnList = "short_link_id, device_type")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Short link is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "short_link_id", nullable = false, foreignKey = @ForeignKey(name = "fk_click_event_short_link"))
    private ShortLink shortLink;

    @NotNull(message = "Click timestamp is required")
    @PastOrPresent(message = "Click timestamp cannot be in the future")
    @CreationTimestamp
    @Column(name = "clicked_at", nullable = false, updatable = false)
    private Instant clickedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String referer;

    @Size(max = 2, message = "Country code must be 2 characters (ISO 3166-1 alpha-2)")
    @Column(length = 2)
    private String country;

    @Size(max = 255)
    @Column(length = 255)
    private String city;

    @NotNull(message = "Device type is required")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType = DeviceType.UNKNOWN;

    @Size(max = 100)
    @Column(length = 100)
    private String browser;

    @Size(max = 100)
    @Column(length = 100)
    private String os;

    // Business logic methods

    /**
     * Check if this click event is from a mobile device.
     *
     * @return true if device is mobile or tablet
     */
    public boolean isMobileDevice() {
        return deviceType == DeviceType.MOBILE || deviceType == DeviceType.TABLET;
    }

    /**
     * Check if this click event is from a bot.
     *
     * @return true if device is a bot
     */
    public boolean isBot() {
        return deviceType == DeviceType.BOT;
    }

    /**
     * Check if country information is available.
     *
     * @return true if country is not null and not empty
     */
    public boolean hasCountryInfo() {
        return country != null && !country.isBlank();
    }

    /**
     * Check if referer information is available.
     *
     * @return true if referer is not null and not empty
     */
    public boolean hasReferer() {
        return referer != null && !referer.isBlank();
    }

    /**
     * Anonymize IP address for GDPR compliance by masking the last octet.
     * For example: 192.168.1.100 becomes 192.168.1.0
     */
    public void anonymizeIpAddress() {
        if (ipAddress != null && !ipAddress.isBlank()) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                // IPv4 - mask last octet
                ipAddress = parts[0] + "." + parts[1] + "." + parts[2] + ".0";
            } else if (ipAddress.contains(":")) {
                // IPv6 - mask last 64 bits
                int lastColon = ipAddress.lastIndexOf(':');
                if (lastColon > 0) {
                    ipAddress = ipAddress.substring(0, lastColon) + ":0000";
                }
            }
        }
    }

    // equals and hashCode based on id (since click events don't have natural business keys)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClickEvent that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ClickEvent{" +
                "id=" + id +
                ", shortLinkId=" + (shortLink != null ? shortLink.getId() : null) +
                ", clickedAt=" + clickedAt +
                ", country='" + country + '\'' +
                ", deviceType=" + deviceType +
                ", browser='" + browser + '\'' +
                ", os='" + os + '\'' +
                '}';
    }
}
