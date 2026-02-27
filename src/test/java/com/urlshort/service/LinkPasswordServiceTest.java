package com.urlshort.service;

import com.urlshort.domain.LinkPassword;
import com.urlshort.domain.ShortLink;
import com.urlshort.domain.User;
import com.urlshort.domain.UserRole;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.password.LinkPasswordRequest;
import com.urlshort.dto.password.LinkPasswordResponse;
import com.urlshort.dto.password.PasswordValidationRequest;
import com.urlshort.dto.password.PasswordValidationResponse;
import com.urlshort.exception.LinkAlreadyPasswordProtectedException;
import com.urlshort.repository.LinkPasswordRepository;
import com.urlshort.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkPasswordService Unit Tests")
class LinkPasswordServiceTest {

    @Mock
    private LinkPasswordRepository passwordRepository;

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LinkPasswordService linkPasswordService;

    private ShortLink shortLink;
    private LinkPassword linkPassword;

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

        linkPassword = LinkPassword.builder()
                .id(50L)
                .shortLink(shortLink)
                .passwordHash("$2a$10$hashedpassword")
                .failedAttempts(0)
                .lockedUntil(null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("addPassword creates password protection for a link")
    void addPassword_success_createsPasswordProtection() {
        LinkPasswordRequest request = LinkPasswordRequest.builder()
                .password("secret123")
                .build();

        when(shortLinkRepository.findById(100L)).thenReturn(Optional.of(shortLink));
        when(passwordRepository.existsByShortLinkId(100L)).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$encoded");
        when(passwordRepository.save(any(LinkPassword.class))).thenReturn(linkPassword);

        LinkPasswordResponse response = linkPasswordService.addPassword(100L, request);

        assertThat(response).isNotNull();
        assertThat(response.shortLinkId()).isEqualTo(100L);
        assertThat(response.failedAttempts()).isEqualTo(0);
        assertThat(response.locked()).isFalse();
        verify(passwordRepository).save(any(LinkPassword.class));
    }

    @Test
    @DisplayName("addPassword throws LinkAlreadyPasswordProtectedException for duplicate")
    void addPassword_duplicate_throwsLinkAlreadyPasswordProtectedException() {
        LinkPasswordRequest request = LinkPasswordRequest.builder()
                .password("secret123")
                .build();

        when(shortLinkRepository.findById(100L)).thenReturn(Optional.of(shortLink));
        when(passwordRepository.existsByShortLinkId(100L)).thenReturn(true);

        assertThatThrownBy(() -> linkPasswordService.addPassword(100L, request))
                .isInstanceOf(LinkAlreadyPasswordProtectedException.class)
                .hasMessageContaining("already has password protection");
    }

    @Test
    @DisplayName("validatePassword with correct password returns valid response with access token")
    void validatePassword_correct_returnsValidResponse() {
        PasswordValidationRequest request = PasswordValidationRequest.builder()
                .password("secret123")
                .build();

        when(passwordRepository.findByShortLinkId(100L)).thenReturn(Optional.of(linkPassword));
        when(passwordEncoder.matches("secret123", "$2a$10$hashedpassword")).thenReturn(true);
        when(passwordRepository.save(any(LinkPassword.class))).thenReturn(linkPassword);

        PasswordValidationResponse response = linkPasswordService.validatePassword(100L, request);

        assertThat(response.valid()).isTrue();
        assertThat(response.locked()).isFalse();
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.message()).isEqualTo("Password correct");
    }

    @Test
    @DisplayName("validatePassword with incorrect password records failed attempt")
    void validatePassword_incorrect_recordsFailedAttempt() {
        PasswordValidationRequest request = PasswordValidationRequest.builder()
                .password("wrongpassword")
                .build();

        when(passwordRepository.findByShortLinkId(100L)).thenReturn(Optional.of(linkPassword));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);
        when(passwordRepository.save(any(LinkPassword.class))).thenReturn(linkPassword);

        PasswordValidationResponse response = linkPasswordService.validatePassword(100L, request);

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).isEqualTo("Invalid password");
        verify(passwordRepository).save(any(LinkPassword.class));
    }

    @Test
    @DisplayName("validatePassword for locked link returns locked response")
    void validatePassword_locked_returnsLockedResponse() {
        LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(15);
        LinkPassword lockedPassword = LinkPassword.builder()
                .id(50L)
                .shortLink(shortLink)
                .passwordHash("$2a$10$hashedpassword")
                .failedAttempts(5)
                .lockedUntil(lockedUntil)
                .createdAt(LocalDateTime.now())
                .build();

        PasswordValidationRequest request = PasswordValidationRequest.builder()
                .password("secret123")
                .build();

        when(passwordRepository.findByShortLinkId(100L)).thenReturn(Optional.of(lockedPassword));

        PasswordValidationResponse response = linkPasswordService.validatePassword(100L, request);

        assertThat(response.valid()).isFalse();
        assertThat(response.locked()).isTrue();
        assertThat(response.lockedUntil()).isNotNull();
        assertThat(response.message()).contains("locked");
    }

    @Test
    @DisplayName("isPasswordProtected returns true when password exists")
    void isPasswordProtected_exists_returnsTrue() {
        when(passwordRepository.existsByShortLinkId(100L)).thenReturn(true);

        boolean result = linkPasswordService.isPasswordProtected(100L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isPasswordProtected returns false when no password exists")
    void isPasswordProtected_notExists_returnsFalse() {
        when(passwordRepository.existsByShortLinkId(100L)).thenReturn(false);

        boolean result = linkPasswordService.isPasswordProtected(100L);

        assertThat(result).isFalse();
    }
}
