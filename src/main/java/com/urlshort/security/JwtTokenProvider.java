package com.urlshort.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT token provider for generating and validating JWT tokens.
 * This component handles all JWT operations including token generation,
 * validation, and claims extraction. It uses JJWT library with HS512
 * algorithm for signing tokens.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpirationMs;

    private SecretKey key;

    /**
     * Initialize the signing key after bean construction.
     * Validates that the secret key is sufficiently long for HS512.
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        );
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT token provider initialized with expiration: {} ms", jwtExpirationMs);
    }

    /**
     * Generates a JWT access token for the given user details.
     *
     * @param userDetails the authenticated user details
     * @return JWT token string
     */
    public String generateToken(CustomUserDetails userDetails) {
        return generateToken(userDetails, jwtExpirationMs);
    }

    /**
     * Generates a JWT refresh token for the given user details.
     *
     * @param userDetails the authenticated user details
     * @return JWT refresh token string
     */
    public String generateRefreshToken(CustomUserDetails userDetails) {
        return generateToken(userDetails, jwtRefreshExpirationMs);
    }

    /**
     * Generates a JWT token with custom expiration time.
     *
     * @param userDetails the authenticated user details
     * @param expirationMs expiration time in milliseconds
     * @return JWT token string
     */
    private String generateToken(CustomUserDetails userDetails, long expirationMs) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userDetails.getId());
        claims.put("workspace_id", userDetails.getWorkspaceId());
        claims.put("email", userDetails.getEmail());
        claims.put("full_name", userDetails.getFullName());
        claims.put("role", userDetails.getRole().name());

        String token = Jwts.builder()
            .subject(userDetails.getId().toString())
            .claims(claims)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryDate))
            .signWith(key, Jwts.SIG.HS512)
            .compact();

        log.debug("Generated JWT token for user: {} (workspace: {})",
            userDetails.getEmail(), userDetails.getWorkspaceId());

        return token;
    }

    /**
     * Extracts the user ID from the JWT token.
     *
     * @param token JWT token string
     * @return user ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("user_id", Long.class);
    }

    /**
     * Extracts the workspace ID from the JWT token.
     *
     * @param token JWT token string
     * @return workspace ID
     */
    public Long getWorkspaceIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("workspace_id", Long.class);
    }

    /**
     * Extracts the email from the JWT token.
     *
     * @param token JWT token string
     * @return user email
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Extracts the user role from the JWT token.
     *
     * @param token JWT token string
     * @return user role
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extracts all claims from the JWT token.
     *
     * @param token JWT token string
     * @return claims object
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Validates the JWT token.
     *
     * @param token JWT token string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Checks if the JWT token is expired.
     *
     * @param token JWT token string
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (Exception ex) {
            log.error("Error checking token expiration: {}", ex.getMessage());
            return true;
        }
    }

    /**
     * Gets the expiration date from the JWT token.
     *
     * @param token JWT token string
     * @return expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getExpiration();
    }
}
