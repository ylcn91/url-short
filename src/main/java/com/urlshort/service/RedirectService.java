package com.urlshort.service;

import com.urlshort.domain.CustomDomain;
import com.urlshort.dto.link.ShortLinkResponse;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.CustomDomainRepository;
import com.urlshort.repository.WorkspaceRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectService {

    private static final Long DEFAULT_WORKSPACE_ID = 1L;

    private final ShortLinkService shortLinkService;
    private final CustomDomainRepository customDomainRepository;
    private final WorkspaceRepository workspaceRepository;

    @Value("${app.short-url.base-domain:localhost}")
    private String baseDomain;

    public ShortLinkResponse resolveShortLink(String code, Long workspaceId) {
        ShortLinkResponse shortLink = shortLinkService.getShortLink(workspaceId, code);

        if (!shortLink.isActive()) {
            throw new ResourceNotFoundException("This short link is inactive");
        }

        return shortLink;
    }

    public Long resolveWorkspaceId(HttpServletRequest request) {
        String host = request.getServerName();
        if (host == null || host.isBlank()) {
            return DEFAULT_WORKSPACE_ID;
        }

        // 1. Check custom domain mapping (e.g., go.acme.com → workspace)
        var customDomain = customDomainRepository.findByDomain(host);
        if (customDomain.isPresent()) {
            CustomDomain domain = customDomain.get();
            if (domain.getStatus() == CustomDomain.DomainStatus.VERIFIED) {
                log.debug("Resolved workspace via custom domain: host={}, workspaceId={}",
                        host, domain.getWorkspace().getId());
                return domain.getWorkspace().getId();
            }
            log.warn("Custom domain found but not verified: host={}, status={}", host, domain.getStatus());
        }

        // 2. Check subdomain routing (e.g., acme.short.ly → slug "acme" → workspace)
        if (host.endsWith("." + baseDomain)) {
            String subdomain = host.substring(0, host.length() - baseDomain.length() - 1);
            if (!subdomain.isBlank()) {
                var workspace = workspaceRepository.findBySlugAndIsDeletedFalse(subdomain);
                if (workspace.isPresent()) {
                    log.debug("Resolved workspace via subdomain: subdomain={}, workspaceId={}",
                            subdomain, workspace.get().getId());
                    return workspace.get().getId();
                }
                log.warn("Subdomain does not match any workspace: {}", subdomain);
            }
        }

        // 3. Fallback to default workspace
        return DEFAULT_WORKSPACE_ID;
    }

    public String extractClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int firstComma = ip.indexOf(',');
            if (firstComma > 0) {
                ip = ip.substring(0, firstComma).trim();
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}
