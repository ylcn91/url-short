package com.urlshort.service;

import com.urlshort.domain.LinkVariant;
import com.urlshort.domain.ShortLink;
import com.urlshort.domain.User;
import com.urlshort.domain.UserRole;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.variant.LinkVariantRequest;
import com.urlshort.dto.variant.LinkVariantResponse;
import com.urlshort.dto.variant.VariantStatsResponse;
import com.urlshort.exception.VariantWeightExceededException;
import com.urlshort.repository.LinkVariantRepository;
import com.urlshort.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkVariantService Unit Tests")
class LinkVariantServiceTest {

    @Mock
    private LinkVariantRepository variantRepository;

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @InjectMocks
    private LinkVariantService linkVariantService;

    private ShortLink shortLink;
    private LinkVariant variantA;
    private LinkVariant variantB;

    @BeforeEach
    void setUp() {
        Workspace workspace = Workspace.builder()
                .id(1L)
                .name("Test")
                .slug("test")
                .isDeleted(false)
                .settings(new HashMap<>())
                .build();

        User user = User.builder()
                .id(10L)
                .workspace(workspace)
                .email("user@test.com")
                .fullName("User")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .isDeleted(false)
                .build();

        shortLink = ShortLink.builder()
                .id(100L)
                .workspace(workspace)
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .normalizedUrl("https://example.com")
                .createdBy(user)
                .clickCount(0L)
                .isActive(true)
                .isDeleted(false)
                .metadata(new HashMap<>())
                .build();

        variantA = LinkVariant.builder()
                .id(1L)
                .shortLink(shortLink)
                .name("Control")
                .destinationUrl("https://example.com/page-a")
                .weight(50)
                .clickCount(100L)
                .conversionCount(10L)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        variantB = LinkVariant.builder()
                .id(2L)
                .shortLink(shortLink)
                .name("Treatment")
                .destinationUrl("https://example.com/page-b")
                .weight(50)
                .clickCount(100L)
                .conversionCount(15L)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createVariant creates a new variant successfully")
    void createVariant_success_createsVariant() {
        LinkVariantRequest request = LinkVariantRequest.builder()
                .name("Control")
                .destinationUrl("https://example.com/page-a")
                .weight(50)
                .build();

        when(shortLinkRepository.findById(100L)).thenReturn(Optional.of(shortLink));
        when(variantRepository.sumWeightByShortLinkId(100L)).thenReturn(0);
        when(variantRepository.save(any(LinkVariant.class))).thenReturn(variantA);

        LinkVariantResponse response = linkVariantService.createVariant(100L, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Control");
        assertThat(response.destinationUrl()).isEqualTo("https://example.com/page-a");
        assertThat(response.weight()).isEqualTo(50);
        verify(variantRepository).save(any(LinkVariant.class));
    }

    @Test
    @DisplayName("createVariant throws VariantWeightExceededException when total weight exceeds 100")
    void createVariant_exceedsWeight_throwsVariantWeightExceededException() {
        LinkVariantRequest request = LinkVariantRequest.builder()
                .name("Extra")
                .destinationUrl("https://example.com/extra")
                .weight(60)
                .build();

        when(shortLinkRepository.findById(100L)).thenReturn(Optional.of(shortLink));
        when(variantRepository.sumWeightByShortLinkId(100L)).thenReturn(50);

        assertThatThrownBy(() -> linkVariantService.createVariant(100L, request))
                .isInstanceOf(VariantWeightExceededException.class)
                .hasMessageContaining("exceed 100%");
    }

    @Test
    @DisplayName("selectVariant returns destination URL from weighted random selection")
    void selectVariant_returnsDestinationUrl() {
        when(variantRepository.findByShortLinkIdAndIsActive(100L, true))
                .thenReturn(List.of(variantA));
        when(variantRepository.findById(variantA.getId())).thenReturn(Optional.of(variantA));
        when(variantRepository.save(any(LinkVariant.class))).thenReturn(variantA);

        String destinationUrl = linkVariantService.selectVariant(100L);

        assertThat(destinationUrl).isNotNull();
        // With only one variant, the result should always be variantA's URL
        assertThat(destinationUrl).isEqualTo("https://example.com/page-a");
    }

    @Test
    @DisplayName("getVariants returns list of variant responses")
    void getVariants_returnsVariantList() {
        when(variantRepository.findByShortLinkId(100L)).thenReturn(List.of(variantA, variantB));

        List<LinkVariantResponse> responses = linkVariantService.getVariants(100L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Control");
        assertThat(responses.get(1).name()).isEqualTo("Treatment");
    }

    @Test
    @DisplayName("getVariantStats returns aggregated statistics")
    void getVariantStats_returnsAggregatedStats() {
        when(variantRepository.findByShortLinkId(100L)).thenReturn(List.of(variantA, variantB));

        VariantStatsResponse stats = linkVariantService.getVariantStats(100L);

        assertThat(stats).isNotNull();
        assertThat(stats.totalClicks()).isEqualTo(200L);
        assertThat(stats.totalConversions()).isEqualTo(25L);
        assertThat(stats.overallConversionRate()).isEqualTo(12.5);
        assertThat(stats.variants()).hasSize(2);
    }
}
