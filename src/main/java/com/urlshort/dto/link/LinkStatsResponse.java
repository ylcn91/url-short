package com.urlshort.dto.link;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Map;

/**
 * Response DTO containing analytics and statistics for a short link.
 * Implemented as an immutable Java record.
 */
@Schema(description = "Response object containing link analytics and statistics")
public record LinkStatsResponse(

        @Schema(description = "The short code/slug", example = "abc123")
        @JsonProperty("short_code")
        String shortCode,

        @Schema(description = "Total number of clicks", example = "1523")
        @JsonProperty("total_clicks")
        Long totalClicks,

        @Schema(description = "Clicks grouped by date",
                example = "{\"2025-11-15\": 45, \"2025-11-16\": 62, \"2025-11-17\": 38}")
        @JsonProperty("clicks_by_date")
        Map<LocalDate, Long> clicksByDate,

        @Schema(description = "Clicks grouped by country code",
                example = "{\"US\": 450, \"GB\": 320, \"DE\": 180}")
        @JsonProperty("clicks_by_country")
        Map<String, Long> clicksByCountry,

        @Schema(description = "Clicks grouped by referrer domain",
                example = "{\"google.com\": 230, \"facebook.com\": 180, \"direct\": 520}")
        @JsonProperty("clicks_by_referrer")
        Map<String, Long> clicksByReferrer,

        @Schema(description = "Clicks grouped by device type",
                example = "{\"mobile\": 890, \"desktop\": 580, \"tablet\": 53}")
        @JsonProperty("clicks_by_device")
        Map<String, Long> clicksByDevice
) {
    /**
     * Builder pattern for LinkStatsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String shortCode;
        private Long totalClicks;
        private Map<LocalDate, Long> clicksByDate;
        private Map<String, Long> clicksByCountry;
        private Map<String, Long> clicksByReferrer;
        private Map<String, Long> clicksByDevice;

        public Builder shortCode(String shortCode) {
            this.shortCode = shortCode;
            return this;
        }

        public Builder totalClicks(Long totalClicks) {
            this.totalClicks = totalClicks;
            return this;
        }

        public Builder clicksByDate(Map<LocalDate, Long> clicksByDate) {
            this.clicksByDate = clicksByDate;
            return this;
        }

        public Builder clicksByCountry(Map<String, Long> clicksByCountry) {
            this.clicksByCountry = clicksByCountry;
            return this;
        }

        public Builder clicksByReferrer(Map<String, Long> clicksByReferrer) {
            this.clicksByReferrer = clicksByReferrer;
            return this;
        }

        public Builder clicksByDevice(Map<String, Long> clicksByDevice) {
            this.clicksByDevice = clicksByDevice;
            return this;
        }

        public LinkStatsResponse build() {
            return new LinkStatsResponse(
                    shortCode, totalClicks, clicksByDate,
                    clicksByCountry, clicksByReferrer, clicksByDevice
            );
        }
    }
}
