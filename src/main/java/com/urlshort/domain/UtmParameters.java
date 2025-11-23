package com.urlshort.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UTM (Urchin Tracking Module) parameters for campaign tracking.
 * Embedded in ShortLink entity.
 *
 * Standard UTM parameters:
 * - utm_source: Identifies which site sent the traffic
 * - utm_medium: Identifies what type of link was used
 * - utm_campaign: Identifies a specific campaign
 * - utm_term: Identifies search terms (for paid search)
 * - utm_content: Identifies what specifically was clicked
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtmParameters {

    /**
     * Campaign source (e.g., "google", "newsletter", "facebook").
     */
    @Column(name = "utm_source", length = 255)
    private String source;

    /**
     * Marketing medium (e.g., "cpc", "email", "social").
     */
    @Column(name = "utm_medium", length = 255)
    private String medium;

    /**
     * Campaign name (e.g., "summer_sale", "product_launch").
     */
    @Column(name = "utm_campaign", length = 255)
    private String campaign;

    /**
     * Paid search keywords (e.g., "running+shoes").
     */
    @Column(name = "utm_term", length = 255)
    private String term;

    /**
     * Used to differentiate ads or links (e.g., "textlink", "banner").
     */
    @Column(name = "utm_content", length = 255)
    private String content;

    /**
     * Check if any UTM parameter is set.
     */
    public boolean hasAnyParameter() {
        return source != null || medium != null || campaign != null ||
               term != null || content != null;
    }

    /**
     * Build query string from UTM parameters.
     */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();

        if (source != null) {
            appendParam(sb, "utm_source", source);
        }
        if (medium != null) {
            appendParam(sb, "utm_medium", medium);
        }
        if (campaign != null) {
            appendParam(sb, "utm_campaign", campaign);
        }
        if (term != null) {
            appendParam(sb, "utm_term", term);
        }
        if (content != null) {
            appendParam(sb, "utm_content", content);
        }

        return sb.toString();
    }

    private void appendParam(StringBuilder sb, String key, String value) {
        if (sb.length() > 0) {
            sb.append("&");
        }
        sb.append(key).append("=").append(value);
    }
}
