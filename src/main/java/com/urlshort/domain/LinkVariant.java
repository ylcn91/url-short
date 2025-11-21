package com.urlshort.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreatedDate;

import java.time.LocalDateTime;

/**
 * A/B Test variant for short links.
 * Allows testing different destinations for the same short link.
 *
 * Example:
 * - Short link: acme.com/promo
 * - Variant A (50%): Landing page version 1
 * - Variant B (50%): Landing page version 2
 */
@Entity
@Table(name = "link_variants", indexes = {
    @Index(name = "idx_variant_link", columnList = "short_link_id"),
    @Index(name = "idx_variant_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The short link this variant belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "short_link_id", nullable = false)
    private ShortLink shortLink;

    /**
     * Variant name (e.g., "A", "B", "Control", "Treatment").
     */
    @NotBlank(message = "Variant name cannot be empty")
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * Destination URL for this variant.
     */
    @NotBlank(message = "Destination URL cannot be empty")
    @Column(name = "destination_url", nullable = false, length = 2048)
    private String destinationUrl;

    /**
     * Traffic allocation percentage (0-100).
     * Sum of all active variants should equal 100.
     */
    @NotNull(message = "Weight cannot be null")
    @Column(nullable = false)
    private Integer weight;

    /**
     * Number of clicks this variant has received.
     */
    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    /**
     * Number of conversions this variant has received.
     * Tracked via conversion events.
     */
    @Column(name = "conversion_count", nullable = false)
    @Builder.Default
    private Long conversionCount = 0L;

    /**
     * Whether this variant is currently active.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * When the variant was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Calculate conversion rate.
     */
    public double getConversionRate() {
        if (clickCount == 0) {
            return 0.0;
        }
        return (double) conversionCount / clickCount * 100;
    }

    /**
     * Increment click count.
     */
    public void incrementClicks() {
        clickCount++;
    }

    /**
     * Increment conversion count.
     */
    public void incrementConversions() {
        conversionCount++;
    }
}
