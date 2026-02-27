package com.urlshort.service;

import com.urlshort.domain.CustomDomain;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.domain.CustomDomainRequest;
import com.urlshort.dto.domain.CustomDomainResponse;
import com.urlshort.dto.domain.DomainVerificationResponse;
import com.urlshort.exception.DomainAlreadyRegisteredException;
import com.urlshort.exception.DomainNotVerifiedException;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.CustomDomainRepository;
import com.urlshort.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of custom domain management service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomDomainService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomDomainRepository domainRepository;

    private final WorkspaceRepository workspaceRepository;

        @Transactional
    public CustomDomainResponse registerDomain(Long workspaceId, CustomDomainRequest request) {
        log.info("Registering custom domain {} for workspace {}", request.getDomain(), workspaceId);

        // Check if domain already exists
        if (domainRepository.existsByDomain(request.getDomain())) {
            throw new DomainAlreadyRegisteredException("Domain already registered: " + request.getDomain());
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

        @Transactional
    public CustomDomainResponse setAsDefault(Long domainId) {
        log.info("Setting domain {} as default", domainId);

        CustomDomain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));

        if (domain.getStatus() != CustomDomain.DomainStatus.VERIFIED) {
            throw new DomainNotVerifiedException("Only verified domains can be set as default");
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

        @Transactional(readOnly = true)
    public List<CustomDomainResponse> getWorkspaceDomains(Long workspaceId) {
        return domainRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

        @Transactional(readOnly = true)
    public CustomDomainResponse getDomainByName(String domain) {
        CustomDomain customDomain = domainRepository.findByDomain(domain)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));
        return toResponse(customDomain);
    }

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

    private boolean performDnsVerification(String domain, String expectedToken) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);

            Attributes attrs = ctx.getAttributes(
                    "_url-short-verification." + domain, new String[]{"TXT"});
            Attribute txtRecords = attrs.get("TXT");

            if (txtRecords != null) {
                for (int i = 0; i < txtRecords.size(); i++) {
                    String record = txtRecords.get(i).toString().replace("\"", "");
                    if (record.equals(expectedToken)) {
                        log.info("DNS TXT record verified for domain {}", domain);
                        return true;
                    }
                }
            }

            log.warn("DNS TXT record not found for domain {} (expected token in _url-short-verification.{})",
                    domain, domain);
            return false;
        } catch (NamingException e) {
            log.warn("DNS lookup failed for {}: {}", domain, e.getMessage());
            return false;
        }
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
