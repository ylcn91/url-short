package com.urlshort.service;

import com.urlshort.domain.User;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.workspace.AddMemberRequest;
import com.urlshort.dto.workspace.MemberResponse;
import com.urlshort.dto.workspace.UpdateWorkspaceRequest;
import com.urlshort.dto.workspace.WorkspaceResponse;
import com.urlshort.exception.DuplicateResourceException;
import com.urlshort.exception.ForbiddenAccessException;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.UserRepository;
import com.urlshort.repository.WorkspaceRepository;
import com.urlshort.security.WorkspaceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public WorkspaceResponse getCurrentWorkspace() {
        Long workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Workspace workspace = findActiveWorkspace(workspaceId);
        return toWorkspaceResponse(workspace);
    }

    @Transactional
    public WorkspaceResponse updateWorkspace(Long id, UpdateWorkspaceRequest request) {
        WorkspaceContextHolder.verifyWorkspaceAccess(id);

        Workspace workspace = findActiveWorkspace(id);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            workspace.setName(request.getName().trim());
        }

        if (request.getSettings() != null) {
            workspace.getSettings().putAll(request.getSettings());
        }

        Workspace saved = workspaceRepository.save(workspace);
        log.info("Workspace updated: id={}, name={}", id, saved.getName());
        return toWorkspaceResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long workspaceId, Pageable pageable) {
        WorkspaceContextHolder.verifyWorkspaceAccess(workspaceId);
        findActiveWorkspace(workspaceId);

        Page<User> users = userRepository.findByWorkspaceIdAndIsDeletedFalse(workspaceId, pageable);
        return users.getContent().stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MemberResponse addMember(Long workspaceId, AddMemberRequest request) {
        WorkspaceContextHolder.verifyWorkspaceAccess(workspaceId);
        Workspace workspace = findActiveWorkspace(workspaceId);

        if (userRepository.findByEmailAndIsDeletedFalse(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException(
                    "User with email " + request.getEmail() + " already exists");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        User newUser = User.builder()
                .workspace(workspace)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(request.getRole())
                .passwordHash(passwordHash)
                .isDeleted(false)
                .build();

        User saved = userRepository.save(newUser);
        log.info("Member added: id={}, email={}, role={}", saved.getId(), saved.getEmail(), saved.getRole());

        return toMemberResponse(saved);
    }

    private Workspace findActiveWorkspace(Long id) {
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));
        if (workspace.getIsDeleted()) {
            throw new ResourceNotFoundException("Workspace has been deleted");
        }
        return workspace;
    }

    private WorkspaceResponse toWorkspaceResponse(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .slug(workspace.getSlug())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .isActive(!workspace.getIsDeleted())
                .settings(workspace.getSettings())
                .build();
    }

    private MemberResponse toMemberResponse(User user) {
        return MemberResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .isActive(!user.getIsDeleted())
                .build();
    }
}
