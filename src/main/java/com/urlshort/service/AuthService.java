package com.urlshort.service;

import com.urlshort.domain.User;
import com.urlshort.domain.UserRole;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.auth.AuthResponse;
import com.urlshort.dto.auth.LoginRequest;
import com.urlshort.dto.auth.RefreshTokenRequest;
import com.urlshort.dto.auth.SignupRequest;
import com.urlshort.dto.auth.UserResponse;
import com.urlshort.exception.DuplicateResourceException;
import com.urlshort.exception.UnauthorizedException;
import com.urlshort.repository.UserRepository;
import com.urlshort.repository.WorkspaceRepository;
import com.urlshort.security.CustomUserDetails;
import com.urlshort.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling authentication operations.
 * This service manages user registration, login, token refresh, and
 * current user information retrieval.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * @param request login request containing email and password
     * @return authentication response with tokens and user info
     * @throws UnauthorizedException if authentication fails
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Update last login timestamp
            User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
            user.updateLastLogin();
            userRepository.save(user);

            // Generate tokens
            String accessToken = tokenProvider.generateToken(userDetails);
            String refreshToken = tokenProvider.generateRefreshToken(userDetails);

            log.info("Login successful for user: {} (workspace: {})",
                userDetails.getEmail(), userDetails.getWorkspaceId());

            return buildAuthResponse(accessToken, refreshToken, user);

        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for user: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    /**
     * Registers a new user and workspace.
     *
     * @param request signup request containing user and workspace details
     * @return authentication response with tokens and user info
     * @throws DuplicateResourceException if email or workspace slug already exists
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("Attempting signup for email: {} with workspace: {}",
            request.getEmail(), request.getWorkspaceSlug());

        // Check if email already exists
        if (userRepository.findByEmailAndIsDeletedFalse(request.getEmail()).isPresent()) {
            log.warn("Signup failed: email already exists: {}", request.getEmail());
            throw new DuplicateResourceException("User with this email already exists");
        }

        // Check if workspace slug already exists
        if (workspaceRepository.findBySlugAndIsDeletedFalse(request.getWorkspaceSlug()).isPresent()) {
            log.warn("Signup failed: workspace slug already exists: {}", request.getWorkspaceSlug());
            throw new DuplicateResourceException("Workspace with this slug already exists");
        }

        // Create workspace
        Workspace workspace = Workspace.builder()
            .name(request.getWorkspaceName())
            .slug(request.getWorkspaceSlug())
            .build();
        workspace = workspaceRepository.save(workspace);

        log.debug("Created workspace: {} ({})", workspace.getName(), workspace.getSlug());

        // Create user as workspace admin
        User user = User.builder()
            .workspace(workspace)
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .role(UserRole.ADMIN)
            .build();
        user = userRepository.save(user);

        log.info("User created successfully: {} (workspace: {})", user.getEmail(), workspace.getSlug());

        // Generate tokens
        CustomUserDetails userDetails = CustomUserDetails.from(user);
        String accessToken = tokenProvider.generateToken(userDetails);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails);

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param request refresh token request
     * @return authentication response with new tokens
     * @throws UnauthorizedException if refresh token is invalid
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Attempting to refresh token");

        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            log.warn("Invalid refresh token provided");
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
            .filter(u -> !u.getIsDeleted())
            .orElseThrow(() -> new UnauthorizedException("User not found or inactive"));

        CustomUserDetails userDetails = CustomUserDetails.from(user);
        String newAccessToken = tokenProvider.generateToken(userDetails);
        String newRefreshToken = tokenProvider.generateRefreshToken(userDetails);

        log.info("Token refreshed successfully for user: {}", user.getEmail());

        return buildAuthResponse(newAccessToken, newRefreshToken, user);
    }

    /**
     * Gets the current authenticated user's information.
     *
     * @return user response DTO
     * @throws UnauthorizedException if user is not authenticated
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("Invalid authentication");
        }
        User user = userRepository.findById(userDetails.getId())
            .orElseThrow(() -> new UnauthorizedException("User not found"));

        log.debug("Retrieved current user: {}", user.getEmail());

        return UserResponse.from(user);
    }

    /**
     * Builds an authentication response with tokens and user information.
     *
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @param user user entity
     * @return authentication response
     */
    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .workspaceId(user.getWorkspace().getId())
            .workspaceName(user.getWorkspace().getName())
            .workspaceSlug(user.getWorkspace().getSlug())
            .createdAt(user.getCreatedAt())
            .build();

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtExpirationMs / 1000) // Convert to seconds
            .user(userInfo)
            .build();
    }
}
