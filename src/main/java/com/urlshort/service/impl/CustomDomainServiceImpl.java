package com.urlshort.service.impl;

import com.urlshort.domain.CustomDomain;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.CustomDomainRequest;
import com.urlshort.dto.CustomDomainResponse;
import com.urlshort.dto.DomainVerificationResponse;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.CustomDomainRepository;
import com.urlshort.repository.WorkspaceRepository;
import com.urlshort.service.CustomDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of custom domain management service.
 */
@Service
public class CustomDomainServiceImpl implements CustomDomainService {

    private static final Logger log = LoggerFactory.getLogger(CustomDomainServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private CustomDomainRepository domainRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Override
    @Transactional
    public CustomDomainResponse registerDomain(Long workspaceId, CustomDomainRequest request) {
        log.info("Registering custom domain {} for workspace {}", request.getDomain(), workspaceId);

        // Check if domain already exists
        if (domainRepository.existsByDomain(request.getDomain())) {
            throw new IllegalArgumentException("Domain already registered");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        // Generate verification token
        String verificationToken = generateVerificationToken();

        CustomDomain domain = CustomDomain.builder()
                .workspace(workspace)
                .domain(request.getDomain())
                .status(CustomDomain.DomainStatus.PENDING)
                .verificationToken(verificationToken)
                .useHttps(request.getUseHttps() != null ? request.getUseHttps() : true)
                .isDefault(false)
                .build();

        CustomDomain saved = domainRepository.save(domain);
        log.info("Domain {} registered with verification token", request.getDomain());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public DomainVerificationResponse verifyDomain(Long domainId) {
        log.info("Verifying domain {}", domainId);

        CustomDomain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));

        // TODO: Implement actual DNS TXT record check
        // For now, simulate verification
        boolean verified = performDnsVerification(domain.getDomain(), domain.getVerificationToken());

        if (verified) {
            domain.setStatus(CustomDomain.DomainStatus.VERIFIED);
            domain.setVerifiedAt(LocalDateTime.now());
            domainRepository.save(domain);
            log.info("Domain {} verified successfully", domain.getDomain());
        } else {
            domain.setStatus(CustomDomain.DomainStatus.FAILED);
            domainRepository.save(domain);
            log.warn("Domain {} verification failed", domain.getDomain());
        }

        return DomainVerificationResponse.builder()
                .verified(verified)
                .domain(domain.getDomain())
                .status(domain.getStatus().name())
                .message(verified ? "Domain verified successfully" : "Verification failed - TXT record not found")
                .build();
    }

    @Override
    @Transactional
    public CustomDomainResponse setAsDefault(Long domainId) {
        log.info("Setting domain {} as default", domainId);

        CustomDomain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));

        if (domain.getStatus() != CustomDomain.DomainStatus.VERIFIED) {
            throw new IllegalStateException("Only verified domains can be set as default");
        }

        // Unset current default
        domainRepository.findByWorkspaceIdAndIsDefault(domain.getWorkspace().getId(), true)
                .ifPresent(currentDefault -> {
                    currentDefault.setIsDefault(false);
                    domainRepository.save(currentDefault);
                });

        // Set new default
        domain.setIsDefault(true);
        CustomDomain saved = domainRepository.save(domain);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomDomainResponse> getWorkspaceDomains(Long workspaceId) {
        return domainRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomDomainResponse getDomainByName(String domain) {
        CustomDomain customDomain = domainRepository.findByDomain(domain)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));
        return toResponse(customDomain);
    }

    @Override
    @Transactional
    public void deleteDomain(Long domainId) {
        log.info("Deleting domain {}", domainId);
        domainRepository.deleteById(domainId);
    }

    private String generateVerificationToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean performDnsVerification(String domain, String token) {
        // TODO: Implement actual DNS TXT record lookup
        // This would use a DNS library to check for TXT record: _url-short-verification.{domain}
        // For now, return true for development
        log.info("DNS verification for domain {} with token {} (not implemented)", domain, token);
        return true;
    }

    private CustomDomainResponse toResponse(CustomDomain domain) {
        return CustomDomainResponse.builder()
                .id(domain.getId())
                .domain(domain.getDomain())
                .status(domain.getStatus().name())
                .verificationToken(domain.getVerificationToken())
                .verifiedAt(domain.getVerifiedAt())
                .useHttps(domain.getUseHttps())
                .isDefault(domain.getIsDefault())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
