package com.urlshort.service;

import com.urlshort.domain.CustomDomain;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.domain.CustomDomainRequest;
import com.urlshort.dto.domain.CustomDomainResponse;
import com.urlshort.exception.DomainAlreadyRegisteredException;
import com.urlshort.exception.DomainNotVerifiedException;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.CustomDomainRepository;
import com.urlshort.repository.WorkspaceRepository;
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
@DisplayName("CustomDomainService Unit Tests")
class CustomDomainServiceTest {

    @Mock
    private CustomDomainRepository domainRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private CustomDomainService customDomainService;

    private Workspace workspace;
    private CustomDomain customDomain;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder()
                .id(1L)
                .name("Test Workspace")
                .slug("test-ws")
                .isDeleted(false)
                .settings(new HashMap<>())
                .build();

        customDomain = CustomDomain.builder()
                .id(10L)
                .workspace(workspace)
                .domain("go.acme.com")
                .status(CustomDomain.DomainStatus.PENDING)
                .verificationToken("verify-token-123")
                .useHttps(true)
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("registerDomain creates a new custom domain successfully")
    void registerDomain_success_createsDomain() {
        CustomDomainRequest request = CustomDomainRequest.builder()
                .domain("go.acme.com")
                .useHttps(true)
                .build();

        when(domainRepository.existsByDomain("go.acme.com")).thenReturn(false);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(domainRepository.save(any(CustomDomain.class))).thenReturn(customDomain);

        CustomDomainResponse response = customDomainService.registerDomain(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.domain()).isEqualTo("go.acme.com");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.verificationToken()).isNotNull();
        verify(domainRepository).save(any(CustomDomain.class));
    }

    @Test
    @DisplayName("registerDomain throws DomainAlreadyRegisteredException for duplicate domain")
    void registerDomain_duplicate_throwsDomainAlreadyRegisteredException() {
        CustomDomainRequest request = CustomDomainRequest.builder()
                .domain("go.acme.com")
                .build();

        when(domainRepository.existsByDomain("go.acme.com")).thenReturn(true);

        assertThatThrownBy(() -> customDomainService.registerDomain(1L, request))
                .isInstanceOf(DomainAlreadyRegisteredException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("setAsDefault throws DomainNotVerifiedException for unverified domain")
    void setAsDefault_unverified_throwsDomainNotVerifiedException() {
        CustomDomain pendingDomain = CustomDomain.builder()
                .id(10L)
                .workspace(workspace)
                .domain("go.acme.com")
                .status(CustomDomain.DomainStatus.PENDING)
                .verificationToken("token")
                .useHttps(true)
                .isDefault(false)
                .build();

        when(domainRepository.findById(10L)).thenReturn(Optional.of(pendingDomain));

        assertThatThrownBy(() -> customDomainService.setAsDefault(10L))
                .isInstanceOf(DomainNotVerifiedException.class)
                .hasMessageContaining("verified");
    }

    @Test
    @DisplayName("getWorkspaceDomains returns list of domain responses")
    void getWorkspaceDomains_returnsDomainList() {
        when(domainRepository.findByWorkspaceId(1L)).thenReturn(List.of(customDomain));

        List<CustomDomainResponse> responses = customDomainService.getWorkspaceDomains(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).domain()).isEqualTo("go.acme.com");
    }

    @Test
    @DisplayName("deleteDomain calls repository deleteById")
    void deleteDomain_callsDeleteById() {
        doNothing().when(domainRepository).deleteById(10L);

        customDomainService.deleteDomain(10L);

        verify(domainRepository).deleteById(10L);
    }
}
