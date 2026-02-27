package com.urlshort.service;

import com.urlshort.domain.User;
import com.urlshort.domain.UserRole;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.workspace.AddMemberRequest;
import com.urlshort.dto.workspace.MemberResponse;
import com.urlshort.dto.workspace.UpdateWorkspaceRequest;
import com.urlshort.dto.workspace.WorkspaceResponse;
import com.urlshort.exception.DuplicateResourceException;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.UserRepository;
import com.urlshort.repository.WorkspaceRepository;
import com.urlshort.security.CustomUserDetails;
import com.urlshort.security.WorkspaceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceService Unit Tests")
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private WorkspaceService workspaceService;

    private Workspace workspace;
    private User user;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder()
                .id(1L)
                .name("Test Workspace")
                .slug("test-workspace")
                .isDeleted(false)
                .settings(new HashMap<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        user = User.builder()
                .id(10L)
                .workspace(workspace)
                .email("user@example.com")
                .fullName("Test User")
                .passwordHash("hashed")
                .role(UserRole.ADMIN)
                .isDeleted(false)
                .createdAt(Instant.now())
                .lastLoginAt(Instant.now())
                .build();

        // Set up SecurityContext for WorkspaceContextHolder calls
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentWorkspace returns workspace response for current user")
    void getCurrentWorkspace_returnsWorkspaceResponse() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));

        WorkspaceResponse response = workspaceService.getCurrentWorkspace();

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Test Workspace");
        assertThat(response.slug()).isEqualTo("test-workspace");
        assertThat(response.isActive()).isTrue();
        verify(workspaceRepository).findById(1L);
    }

    @Test
    @DisplayName("updateWorkspace updates name and settings successfully")
    void updateWorkspace_updatesNameAndSettings() {
        Map<String, Object> newSettings = Map.of("theme", "dark");
        UpdateWorkspaceRequest request = UpdateWorkspaceRequest.builder()
                .name("Updated Workspace")
                .settings(newSettings)
                .build();

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(workspace);

        WorkspaceResponse response = workspaceService.updateWorkspace(1L, request);

        assertThat(response).isNotNull();
        verify(workspaceRepository).save(any(Workspace.class));
        assertThat(workspace.getName()).isEqualTo("Updated Workspace");
        assertThat(workspace.getSettings()).containsKey("theme");
    }

    @Test
    @DisplayName("listMembers returns paginated member list")
    void listMembers_returnsMemberList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(userRepository.findByWorkspaceIdAndIsDeletedFalse(1L, pageable)).thenReturn(userPage);

        List<MemberResponse> members = workspaceService.listMembers(1L, pageable);

        assertThat(members).hasSize(1);
        assertThat(members.get(0).email()).isEqualTo("user@example.com");
        assertThat(members.get(0).fullName()).isEqualTo("Test User");
        assertThat(members.get(0).role()).isEqualTo(UserRole.ADMIN);
        assertThat(members.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("addMember with duplicate email throws DuplicateResourceException")
    void addMember_duplicateEmail_throwsDuplicateResourceException() {
        AddMemberRequest request = AddMemberRequest.builder()
                .email("user@example.com")
                .fullName("Duplicate User")
                .role(UserRole.MEMBER)
                .build();

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmailAndIsDeletedFalse("user@example.com"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> workspaceService.addMember(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("findActiveWorkspace throws ResourceNotFoundException when workspace not found")
    void findActiveWorkspace_notFound_throwsResourceNotFoundException() {
        // The SecurityContext has user with workspaceId=1, so getCurrentWorkspace calls findById(1L)
        when(workspaceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.getCurrentWorkspace())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Workspace not found");
    }

    @Test
    @DisplayName("findActiveWorkspace throws ResourceNotFoundException when workspace is deleted")
    void findActiveWorkspace_deleted_throwsResourceNotFoundException() {
        Workspace deletedWorkspace = Workspace.builder()
                .id(1L)
                .name("Deleted Workspace")
                .slug("deleted-ws")
                .isDeleted(true)
                .settings(new HashMap<>())
                .build();

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(deletedWorkspace));

        assertThatThrownBy(() -> workspaceService.getCurrentWorkspace())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("deleted");
    }
}
