package com.urlshort.service;

import com.urlshort.dto.LinkVariantRequest;
import com.urlshort.dto.LinkVariantResponse;
import com.urlshort.dto.VariantStatsResponse;

import java.util.List;

/**
 * Service interface for A/B testing variants.
 * <p>
 * Provides operations for creating, managing, and analyzing A/B test variants
 * for short links. Handles traffic distribution and conversion tracking.
 * </p>
 */
public interface LinkVariantService {

    /**
     * Creates a new variant for A/B testing.
     * <p>
     * Validates that total weight of all active variants doesn't exceed 100.
     * </p>
     *
     * @param shortLinkId the short link ID
     * @param request the variant request
     * @return the created variant
     * @throws IllegalArgumentException if total weight exceeds 100
     */
    LinkVariantResponse createVariant(Long shortLinkId, LinkVariantRequest request);

    /**
     * Updates a variant's configuration.
     *
     * @param variantId the variant ID
     * @param request the update request
     * @return the updated variant
     */
    LinkVariantResponse updateVariant(Long variantId, LinkVariantRequest request);

    /**
     * Selects a variant based on weighted random distribution.
     * <p>
     * Uses weighted random selection to choose which variant URL to redirect to.
     * </p>
     *
     * @param shortLinkId the short link ID
     * @return the selected variant's destination URL
     */
    String selectVariant(Long shortLinkId);

    /**
     * Records a click for a variant.
     *
     * @param variantId the variant ID
     */
    void recordClick(Long variantId);

    /**
     * Records a conversion for a variant.
     *
     * @param variantId the variant ID
     */
    void recordConversion(Long variantId);

    /**
     * Retrieves all variants for a short link.
     *
     * @param shortLinkId the short link ID
     * @return list of variants
     */
    List<LinkVariantResponse> getVariants(Long shortLinkId);

    /**
     * Retrieves A/B test statistics for a short link.
     *
     * @param shortLinkId the short link ID
     * @return variant statistics including conversion rates
     */
    VariantStatsResponse getVariantStats(Long shortLinkId);

    /**
     * Deletes a variant.
     *
     * @param variantId the variant ID
     */
    void deleteVariant(Long variantId);

    /**
     * Deactivates all variants for a short link.
     *
     * @param shortLinkId the short link ID
     */
    void deactivateAllVariants(Long shortLinkId);
}
