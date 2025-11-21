package com.urlshort.repository;

import com.urlshort.domain.LinkVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for LinkVariant entity.
 * Provides database operations for A/B testing variants.
 */
@Repository
public interface LinkVariantRepository extends JpaRepository<LinkVariant, Long> {

    /**
     * Retrieves all variants for a specific short link.
     * Used for A/B test configuration and management.
     *
     * @param shortLinkId the short link ID
     * @return list of all variants
     */
    List<LinkVariant> findByShortLinkId(Long shortLinkId);

    /**
     * Retrieves all active variants for a short link.
     * Used for traffic distribution during redirects.
     *
     * @param shortLinkId the short link ID
     * @param isActive true to get only active variants
     * @return list of active variants
     */
    List<LinkVariant> findByShortLinkIdAndIsActive(Long shortLinkId, Boolean isActive);

    /**
     * Counts active variants for a short link.
     * Used to validate A/B test configuration.
     *
     * @param shortLinkId the short link ID
     * @param isActive true to count only active variants
     * @return count of active variants
     */
    long countByShortLinkIdAndIsActive(Long shortLinkId, Boolean isActive);

    /**
     * Calculates the total weight of all active variants.
     * Used to validate that total weight equals 100%.
     *
     * @param shortLinkId the short link ID
     * @return sum of all active variant weights
     */
    @Query("SELECT COALESCE(SUM(v.weight), 0) FROM LinkVariant v WHERE v.shortLink.id = :shortLinkId AND v.isActive = true")
    Integer sumWeightByShortLinkId(@Param("shortLinkId") Long shortLinkId);

    /**
     * Deletes all variants for a short link.
     * Used when removing A/B testing from a link.
     *
     * @param shortLinkId the short link ID
     */
    void deleteByShortLinkId(Long shortLinkId);
}
