package com.urlshort.repository;

import com.urlshort.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Repository interface for ClickEvent entity.
 * Provides database operations for click tracking and analytics.
 */
@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    /**
     * Counts the total number of clicks for a specific short link.
     * Used for basic analytics and statistics.
     *
     * @param shortLinkId the short link ID
     * @return total count of clicks
     */
    long countByShortLinkId(Long shortLinkId);

    /**
     * Aggregates click data grouped by date for a specific short link.
     * Returns time-series data showing click distribution over time.
     *
     * Query extracts the date portion from clickedAt timestamp and groups by date.
     * Result format: List of maps containing 'date' and 'count' keys.
     *
     * Example result: [{"date": "2025-01-15", "count": 42}, {"date": "2025-01-16", "count": 35}]
     *
     * @param linkId the short link ID
     * @return list of maps with date and click count for each day
     */
    @Query("SELECT new map(DATE(ce.clickedAt) as date, COUNT(ce) as count) FROM ClickEvent ce WHERE ce.shortLink.id = :linkId GROUP BY DATE(ce.clickedAt)")
    List<Map<String, Object>> findClicksByDate(@Param("linkId") Long linkId);

    /**
     * Aggregates click data grouped by country for geographic analytics.
     * Shows where clicks are coming from geographically.
     *
     * Query groups click events by country field and counts occurrences.
     * Result format: List of maps containing 'country' and 'count' keys.
     *
     * Example result: [{"country": "US", "count": 150}, {"country": "UK", "count": 75}]
     *
     * @param linkId the short link ID
     * @return list of maps with country code and click count for each country
     */
    @Query("SELECT new map(ce.country as country, COUNT(ce) as count) FROM ClickEvent ce WHERE ce.shortLink.id = :linkId GROUP BY ce.country")
    List<Map<String, Object>> findClicksByCountry(@Param("linkId") Long linkId);
}
