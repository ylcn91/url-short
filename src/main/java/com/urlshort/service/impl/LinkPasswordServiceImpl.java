package com.urlshort.service.impl;

import com.urlshort.domain.LinkPassword;
import com.urlshort.domain.ShortLink;
import com.urlshort.dto.LinkPasswordRequest;
import com.urlshort.dto.LinkPasswordResponse;
import com.urlshort.dto.PasswordValidationRequest;
import com.urlshort.dto.PasswordValidationResponse;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.LinkPasswordRepository;
import com.urlshort.repository.ShortLinkRepository;
import com.urlshort.service.LinkPasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of password-protected links service.
 */
@Service
public class LinkPasswordServiceImpl implements LinkPasswordService {

    private static final Logger log = LoggerFactory.getLogger(LinkPasswordServiceImpl.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private LinkPasswordRepository passwordRepository;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Override
    @Transactional
    public LinkPasswordResponse addPassword(Long shortLinkId, LinkPasswordRequest request) {
        log.info("Adding password protection to link {}", shortLinkId);

        ShortLink shortLink = shortLinkRepository.findById(shortLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));

        if (passwordRepository.existsByShortLinkId(shortLinkId)) {
            throw new IllegalArgumentException("Link already has password protection");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        LinkPassword linkPassword = LinkPassword.builder()
                .shortLink(shortLink)
                .passwordHash(hashedPassword)
                .failedAttempts(0)
                .build();

        LinkPassword saved = passwordRepository.save(linkPassword);
        log.info("Password protection added to link {}", shortLinkId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PasswordValidationResponse validatePassword(Long shortLinkId, PasswordValidationRequest request) {
        log.info("Validating password for link {}", shortLinkId);

        LinkPassword linkPassword = passwordRepository.findByShortLinkId(shortLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Password protection not found"));

        // Check if locked
        if (linkPassword.isLocked()) {
            log.warn("Link {} is locked until {}", shortLinkId, linkPassword.getLockedUntil());
            return PasswordValidationResponse.builder()
                    .valid(false)
                    .locked(true)
                    .lockedUntil(linkPassword.getLockedUntil())
                    .message("Too many failed attempts. Link is locked.")
                    .build();
        }

        // Validate password
        boolean valid = passwordEncoder.matches(request.getPassword(), linkPassword.getPasswordHash());

        if (valid) {
            linkPassword.resetFailedAttempts();
            passwordRepository.save(linkPassword);
            log.info("Password validated successfully for link {}", shortLinkId);

            // Generate access token
            String accessToken = UUID.randomUUID().toString();

            return PasswordValidationResponse.builder()
                    .valid(true)
                    .locked(false)
                    .accessToken(accessToken)
                    .message("Password correct")
                    .build();
        } else {
            linkPassword.recordFailedAttempt();
            passwordRepository.save(linkPassword);
            log.warn("Invalid password attempt for link {}. Failed attempts: {}",
                    shortLinkId, linkPassword.getFailedAttempts());

            return PasswordValidationResponse.builder()
                    .valid(false)
                    .locked(linkPassword.isLocked())
                    .lockedUntil(linkPassword.getLockedUntil())
                    .failedAttempts(linkPassword.getFailedAttempts())
                    .message("Invalid password")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPasswordProtected(Long shortLinkId) {
        return passwordRepository.existsByShortLinkId(shortLinkId);
    }

    @Override
    @Transactional
    public void removePassword(Long shortLinkId) {
        log.info("Removing password protection from link {}", shortLinkId);
        passwordRepository.deleteByShortLinkId(shortLinkId);
    }

    @Override
    @Transactional
    public void resetFailedAttempts(Long shortLinkId) {
        log.info("Resetting failed attempts for link {}", shortLinkId);
        LinkPassword linkPassword = passwordRepository.findByShortLinkId(shortLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Password protection not found"));

        linkPassword.resetFailedAttempts();
        passwordRepository.save(linkPassword);
    }

    private LinkPasswordResponse toResponse(LinkPassword linkPassword) {
        return LinkPasswordResponse.builder()
                .id(linkPassword.getId())
                .shortLinkId(linkPassword.getShortLink().getId())
                .failedAttempts(linkPassword.getFailedAttempts())
                .locked(linkPassword.isLocked())
                .lockedUntil(linkPassword.getLockedUntil())
                .createdAt(linkPassword.getCreatedAt())
                .build();
    }
}
