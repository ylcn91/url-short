package com.urlshort.controller;

import com.urlshort.dto.link.ShortLinkResponse;
import com.urlshort.service.ClickEventService;
import com.urlshort.service.RedirectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Tag(name = "Public Redirect", description = "Public endpoint for URL redirection (no authentication required)")
@Slf4j
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService;
    private final ClickEventService clickEventService;

    @GetMapping("/{code}")
    @Operation(summary = "Redirect to original URL",
               description = "Public endpoint that redirects a short code to its original URL. No authentication required.")
    public ResponseEntity<Void> redirect(
            @Parameter(description = "Short code to redirect", example = "abc123")
            @PathVariable String code,
            HttpServletRequest request) {

        Long workspaceId = redirectService.resolveWorkspaceId(request);
        ShortLinkResponse shortLink = redirectService.resolveShortLink(code, workspaceId);

        String ipAddress = redirectService.extractClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");

        clickEventService.recordClickEvent(
                shortLink.id(), workspaceId, code, shortLink.originalUrl(),
                ipAddress, userAgent, referrer);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(shortLink.originalUrl()))
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build();
    }
}
