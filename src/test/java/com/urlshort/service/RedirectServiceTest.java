package com.urlshort.service;

import com.urlshort.dto.link.ShortLinkResponse;
import com.urlshort.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedirectService Unit Tests")
class RedirectServiceTest {

    @Mock
    private ShortLinkService shortLinkService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private RedirectService redirectService;

    private ShortLinkResponse activeLink;
    private ShortLinkResponse inactiveLink;

    @BeforeEach
    void setUp() {
        activeLink = ShortLinkResponse.builder()
                .id(1L)
                .shortCode("abc123")
                .shortUrl("http://localhost:8080/abc123")
                .originalUrl("https://www.example.com")
                .normalizedUrl("https://www.example.com")
                .createdAt(LocalDateTime.now())
                .clickCount(5L)
                .isActive(true)
                .tags(Set.of())
                .build();

        inactiveLink = ShortLinkResponse.builder()
                .id(2L)
                .shortCode("xyz789")
                .shortUrl("http://localhost:8080/xyz789")
                .originalUrl("https://www.inactive.com")
                .normalizedUrl("https://www.inactive.com")
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .isActive(false)
                .tags(Set.of())
                .build();
    }

    @Test
    @DisplayName("resolveShortLink returns response for active link")
    void resolveShortLink_activeLink_returnsResponse() {
        when(shortLinkService.getShortLink(1L, "abc123")).thenReturn(activeLink);

        ShortLinkResponse response = redirectService.resolveShortLink("abc123", 1L);

        assertThat(response).isNotNull();
        assertThat(response.shortCode()).isEqualTo("abc123");
        assertThat(response.originalUrl()).isEqualTo("https://www.example.com");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("resolveShortLink throws ResourceNotFoundException for inactive link")
    void resolveShortLink_inactiveLink_throwsResourceNotFoundException() {
        when(shortLinkService.getShortLink(1L, "xyz789")).thenReturn(inactiveLink);

        assertThatThrownBy(() -> redirectService.resolveShortLink("xyz789", 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("resolveWorkspaceId returns default workspace ID 1")
    void resolveWorkspaceId_returnsDefault() {
        Long workspaceId = redirectService.resolveWorkspaceId(httpServletRequest);

        assertThat(workspaceId).isEqualTo(1L);
    }

    @Test
    @DisplayName("extractClientIpAddress returns IP from X-Forwarded-For header")
    void extractClientIpAddress_xForwardedFor_returnsFirstIp() {
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2, 10.0.0.3");

        String ip = redirectService.extractClientIpAddress(httpServletRequest);

        assertThat(ip).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("extractClientIpAddress returns IP from X-Real-IP header when X-Forwarded-For is absent")
    void extractClientIpAddress_xRealIp_returnsIp() {
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn("172.16.0.1");

        String ip = redirectService.extractClientIpAddress(httpServletRequest);

        assertThat(ip).isEqualTo("172.16.0.1");
    }

    @Test
    @DisplayName("extractClientIpAddress falls back to remoteAddr when no proxy headers present")
    void extractClientIpAddress_fallback_returnsRemoteAddr() {
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(httpServletRequest.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(httpServletRequest.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        String ip = redirectService.extractClientIpAddress(httpServletRequest);

        assertThat(ip).isEqualTo("127.0.0.1");
    }
}
