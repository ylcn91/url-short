package com.urlshort.service;

import com.urlshort.domain.ShortLink;
import com.urlshort.domain.User;
import com.urlshort.domain.UserRole;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.link.CreateShortLinkRequest;
import com.urlshort.dto.link.ShortLinkResponse;
import com.urlshort.dto.link.UpdateShortLinkRequest;
import com.urlshort.exception.LinkExpiredException;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.ClickEventRepository;
import com.urlshort.repository.ShortLinkRepository;
import com.urlshort.repository.UserRepository;
import com.urlshort.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.cache.CacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkService Unit Tests")
class ShortLinkServiceTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private ShortLinkService shortLinkService;

    private Workspace workspace;
    private User user;
    private ShortLink shortLink;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shortLinkService, "baseUrl", "http://localhost:8080");

        workspace = Workspace.builder()
                .id(1L)
                .name("Test Workspace")
                .slug("test-ws")
                .isDeleted(false)
                .settings(new HashMap<>())
                .users(new ArrayList<>())
                .build();

        user = User.builder()
                .id(10L)
                .workspace(workspace)
                .email("user@test.com")
                .fullName("Test User")
                .passwordHash("hashed")
                .role(UserRole.ADMIN)
                .isDeleted(false)
                .build();

        workspace.getUsers().add(user);

        shortLink = ShortLink.builder()
                .id(100L)
                .workspace(workspace)
                .shortCode("abc123")
                .originalUrl("https://www.example.com/long/url")
                .normalizedUrl("https://www.example.com/long/url")
                .createdBy(user)
                .createdAt(Instant.now())
                .clickCount(0L)
                .isActive(true)
                .isDeleted(false)
                .metadata(new HashMap<>())
                .build();
    }

    @Test
    @DisplayName("createShortLink creates new short link for a new URL")
    void createShortLink_newUrl_createsNewLink() {
        CreateShortLinkRequest request = CreateShortLinkRequest.builder()
                .originalUrl("https://www.example.com/long/url")
                .build();

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(shortLinkRepository.findByWorkspaceIdAndNormalizedUrlAndIsDeletedFalse(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(shortLinkRepository.findByWorkspaceIdAndShortCodeAndIsDeletedFalse(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> {
            ShortLink saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            ReflectionTestUtils.setField(saved, "createdAt", Instant.now());
            return saved;
        });

        ShortLinkResponse response = shortLinkService.createShortLink(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.originalUrl()).isEqualTo("https://www.example.com/long/url");
        assertThat(response.isActive()).isTrue();
        verify(shortLinkRepository).save(any(ShortLink.class));
    }

    @Test
    @DisplayName("createShortLink returns existing link for duplicate URL (deterministic reuse)")
    void createShortLink_existingUrl_returnsExistingLink() {
        CreateShortLinkRequest request = CreateShortLinkRequest.builder()
                .originalUrl("https://www.example.com/long/url")
                .build();

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(shortLinkRepository.findByWorkspaceIdAndNormalizedUrlAndIsDeletedFalse(anyLong(), anyString()))
                .thenReturn(Optional.of(shortLink));

        ShortLinkResponse response = shortLinkService.createShortLink(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.shortCode()).isEqualTo("abc123");
        verify(shortLinkRepository, never()).save(any(ShortLink.class));
    }

    @Test
    @DisplayName("getShortLink returns response for valid short code")
    void getShortLink_found_returnsResponse() {
        when(shortLinkRepository.findByWorkspaceIdAndShortCodeAndIsDeletedFalse(1L, "abc123"))
                .thenReturn(Optional.of(shortLink));

        ShortLinkResponse response = shortLinkService.getShortLink(1L, "abc123");

        assertThat(response).isNotNull();
        assertThat(response.shortCode()).isEqualTo("abc123");
        assertThat(response.originalUrl()).isEqualTo("https://www.example.com/long/url");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/abc123");
    }

    @Test
    @DisplayName("getShortLink throws ResourceNotFoundException when not found")
    void getShortLink_notFound_throwsResourceNotFoundException() {
        when(shortLinkRepository.findByWorkspaceIdAndShortCodeAndIsDeletedFalse(1L, "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> shortLinkService.getShortLink(1L, "missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Short link not found");
    }

    @Test
    @DisplayName("getShortLink throws LinkExpiredException when link is expired")
    void getShortLink_expired_throwsLinkExpiredException() {
        ShortLink expiredLink = ShortLink.builder()
                .id(101L)
                .workspace(workspace)
                .shortCode("expired1")
                .originalUrl("https://expired.com")
                .normalizedUrl("https://expired.com")
                .createdBy(user)
                .createdAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .clickCount(0L)
                .isActive(true)
                .isDeleted(false)
                .metadata(new HashMap<>())
                .build();

        when(shortLinkRepository.findByWorkspaceIdAndShortCodeAndIsDeletedFalse(1L, "expired1"))
                .thenReturn(Optional.of(expiredLink));

        assertThatThrownBy(() -> shortLinkService.getShortLink(1L, "expired1"))
                .isInstanceOf(LinkExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("getShortLink throws LinkExpiredException when max clicks exceeded")
    void getShortLink_maxClicksExceeded_throwsLinkExpiredException() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("maxClicks", 10);

        ShortLink maxedLink = ShortLink.builder()
                .id(102L)
                .workspace(workspace)
                .shortCode("maxed1")
                .originalUrl("https://maxed.com")
                .normalizedUrl("https://maxed.com")
                .createdBy(user)
                .createdAt(Instant.now())
                .clickCount(10L)
                .isActive(true)
                .isDeleted(false)
                .metadata(metadata)
                .build();

        when(shortLinkRepository.findByWorkspaceIdAndShortCodeAndIsDeletedFalse(1L, "maxed1"))
                .thenReturn(Optional.of(maxedLink));

        assertThatThrownBy(() -> shortLinkService.getShortLink(1L, "maxed1"))
                .isInstanceOf(LinkExpiredException.class)
                .hasMessageContaining("maximum clicks");
    }

    @Test
    @DisplayName("deleteShortLink soft deletes the link")
    void deleteShortLink_softDeletesLink() {
        when(shortLinkRepository.findById(100L)).thenReturn(Optional.of(shortLink));
        when(shortLinkRepository.save(any(ShortLink.class))).thenReturn(shortLink);

        shortLinkService.deleteShortLink(1L, 100L);

        verify(shortLinkRepository).save(any(ShortLink.class));
    }

    @Test
    @DisplayName("listShortLinks returns paginated results")
    void listShortLinks_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ShortLink> page = new PageImpl<>(List.of(shortLink));

        when(shortLinkRepository.findByWorkspaceIdAndIsDeletedFalse(1L, pageable)).thenReturn(page);

        Page<ShortLinkResponse> result = shortLinkService.listShortLinks(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).shortCode()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("getShortLinkById returns response for valid ID")
    void getShortLinkById_returnsResponse() {
        when(shortLinkRepository.findById(100L)).thenReturn(Optional.of(shortLink));

        ShortLinkResponse response = shortLinkService.getShortLinkById(1L, 100L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.shortCode()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("updateShortLink updates link properties")
    void updateShortLink_updatesProperties() {
        UpdateShortLinkRequest request = UpdateShortLinkRequest.builder()
                .maxClicks(500)
                .isActive(false)
                .build();

        when(shortLinkRepository.findById(100L)).thenReturn(Optional.of(shortLink));
        when(shortLinkRepository.save(any(ShortLink.class))).thenReturn(shortLink);

        ShortLinkResponse response = shortLinkService.updateShortLink(1L, 100L, request);

        assertThat(response).isNotNull();
        verify(shortLinkRepository).save(any(ShortLink.class));
        assertThat(shortLink.getMetadata().get("maxClicks")).isEqualTo(500);
        assertThat(shortLink.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("bulkCreateShortLinks creates multiple links")
    void bulkCreateShortLinks_createsMultipleLinks() {
        List<String> urls = List.of("https://example.com/1", "https://example.com/2");

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(shortLinkRepository.findByWorkspaceIdAndNormalizedUrlAndIsDeletedFalse(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(shortLinkRepository.findByWorkspaceIdAndShortCodeAndIsDeletedFalse(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> {
            ShortLink saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            ReflectionTestUtils.setField(saved, "createdAt", Instant.now());
            return saved;
        });

        List<ShortLinkResponse> responses = shortLinkService.bulkCreateShortLinks(1L, urls);

        assertThat(responses).hasSize(2);
        verify(shortLinkRepository, times(2)).save(any(ShortLink.class));
    }
}
