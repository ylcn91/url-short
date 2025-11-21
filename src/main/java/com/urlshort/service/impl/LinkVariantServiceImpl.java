package com.urlshort.service.impl;

import com.urlshort.domain.LinkVariant;
import com.urlshort.domain.ShortLink;
import com.urlshort.dto.LinkVariantRequest;
import com.urlshort.dto.LinkVariantResponse;
import com.urlshort.dto.VariantStatsResponse;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.LinkVariantRepository;
import com.urlshort.repository.ShortLinkRepository;
import com.urlshort.service.LinkVariantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Implementation of A/B testing variants service.
 */
@Service
public class LinkVariantServiceImpl implements LinkVariantService {

    private static final Logger log = LoggerFactory.getLogger(LinkVariantServiceImpl.class);
    private final Random random = new Random();

    @Autowired
    private LinkVariantRepository variantRepository;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Override
    @Transactional
    public LinkVariantResponse createVariant(Long shortLinkId, LinkVariantRequest request) {
        log.info("Creating variant {} for link {}", request.getName(), shortLinkId);

        ShortLink shortLink = shortLinkRepository.findById(shortLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));

        // Validate total weight
        Integer currentWeight = variantRepository.sumWeightByShortLinkId(shortLinkId);
        if (currentWeight + request.getWeight() > 100) {
            throw new IllegalArgumentException("Total variant weight would exceed 100%");
        }

        LinkVariant variant = LinkVariant.builder()
                .shortLink(shortLink)
                .name(request.getName())
                .destinationUrl(request.getDestinationUrl())
                .weight(request.getWeight())
                .clickCount(0L)
                .conversionCount(0L)
                .isActive(true)
                .build();

        LinkVariant saved = variantRepository.save(variant);
        log.info("Variant {} created for link {}", request.getName(), shortLinkId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public LinkVariantResponse updateVariant(Long variantId, LinkVariantRequest request) {
        log.info("Updating variant {}", variantId);

        LinkVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        // Validate weight if changed
        if (request.getWeight() != null && !request.getWeight().equals(variant.getWeight())) {
            Integer currentWeight = variantRepository.sumWeightByShortLinkId(variant.getShortLink().getId());
            int newTotalWeight = currentWeight - variant.getWeight() + request.getWeight();
            if (newTotalWeight > 100) {
                throw new IllegalArgumentException("Total variant weight would exceed 100%");
            }
            variant.setWeight(request.getWeight());
        }

        if (request.getDestinationUrl() != null) {
            variant.setDestinationUrl(request.getDestinationUrl());
        }
        if (request.getName() != null) {
            variant.setName(request.getName());
        }

        LinkVariant saved = variantRepository.save(variant);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public String selectVariant(Long shortLinkId) {
        List<LinkVariant> activeVariants = variantRepository.findByShortLinkIdAndIsActive(shortLinkId, true);

        if (activeVariants.isEmpty()) {
            // No A/B test configured, return original URL
            ShortLink shortLink = shortLinkRepository.findById(shortLinkId)
                    .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));
            return shortLink.getOriginalUrl();
        }

        // Weighted random selection
        int totalWeight = activeVariants.stream().mapToInt(LinkVariant::getWeight).sum();
        int randomValue = random.nextInt(totalWeight);

        int cumulativeWeight = 0;
        for (LinkVariant variant : activeVariants) {
            cumulativeWeight += variant.getWeight();
            if (randomValue < cumulativeWeight) {
                log.debug("Selected variant {} for link {}", variant.getName(), shortLinkId);
                recordClick(variant.getId());
                return variant.getDestinationUrl();
            }
        }

        // Fallback (shouldn't reach here)
        return activeVariants.get(0).getDestinationUrl();
    }

    @Override
    @Transactional
    public void recordClick(Long variantId) {
        LinkVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        variant.incrementClicks();
        variantRepository.save(variant);
    }

    @Override
    @Transactional
    public void recordConversion(Long variantId) {
        LinkVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        variant.incrementConversions();
        variantRepository.save(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkVariantResponse> getVariants(Long shortLinkId) {
        return variantRepository.findByShortLinkId(shortLinkId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VariantStatsResponse getVariantStats(Long shortLinkId) {
        List<LinkVariant> variants = variantRepository.findByShortLinkId(shortLinkId);

        long totalClicks = variants.stream().mapToLong(LinkVariant::getClickCount).sum();
        long totalConversions = variants.stream().mapToLong(LinkVariant::getConversionCount).sum();

        List<LinkVariantResponse> variantResponses = variants.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return VariantStatsResponse.builder()
                .variants(variantResponses)
                .totalClicks(totalClicks)
                .totalConversions(totalConversions)
                .overallConversionRate(totalClicks > 0 ? (double) totalConversions / totalClicks * 100 : 0.0)
                .build();
    }

    @Override
    @Transactional
    public void deleteVariant(Long variantId) {
        log.info("Deleting variant {}", variantId);
        variantRepository.deleteById(variantId);
    }

    @Override
    @Transactional
    public void deactivateAllVariants(Long shortLinkId) {
        log.info("Deactivating all variants for link {}", shortLinkId);
        List<LinkVariant> variants = variantRepository.findByShortLinkId(shortLinkId);
        variants.forEach(variant -> {
            variant.setIsActive(false);
            variantRepository.save(variant);
        });
    }

    private LinkVariantResponse toResponse(LinkVariant variant) {
        return LinkVariantResponse.builder()
                .id(variant.getId())
                .shortLinkId(variant.getShortLink().getId())
                .name(variant.getName())
                .destinationUrl(variant.getDestinationUrl())
                .weight(variant.getWeight())
                .clickCount(variant.getClickCount())
                .conversionCount(variant.getConversionCount())
                .conversionRate(variant.getConversionRate())
                .isActive(variant.getIsActive())
                .createdAt(variant.getCreatedAt())
                .build();
    }
}
