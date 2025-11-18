package com.urlshort.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for API documentation.
 * <p>
 * This configuration sets up comprehensive API documentation using SpringDoc OpenAPI 3.
 * The documentation is available at:
 * </p>
 * <ul>
 *   <li><b>Swagger UI:</b> http://localhost:8080/swagger-ui.html</li>
 *   <li><b>OpenAPI JSON:</b> http://localhost:8080/v3/api-docs</li>
 *   <li><b>OpenAPI YAML:</b> http://localhost:8080/v3/api-docs.yaml</li>
 * </ul>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>JWT authentication support in Swagger UI</li>
 *   <li>Interactive API testing</li>
 *   <li>Complete endpoint documentation with examples</li>
 *   <li>Schema definitions for all DTOs</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Creates the OpenAPI documentation configuration.
     * <p>
     * Defines API metadata, security schemes, and server information.
     * </p>
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // Define JWT security scheme
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("Linkforge URL Shortener API")
                .version("1.0.0")
                .description("""
                    # Linkforge URL Shortener API

                    A powerful, deterministic URL shortener platform with advanced analytics and multi-workspace support.

                    ## Key Features

                    - **Deterministic URL Shortening:** Same URL always returns the same short code within a workspace
                    - **Multi-Workspace Support:** Isolated environments for teams and organizations
                    - **Advanced Analytics:** Click tracking, geographic distribution, device analysis
                    - **Custom Domains:** Support for custom branded short URLs
                    - **Bulk Operations:** Create multiple links in a single request
                    - **Role-Based Access:** Admin, Member, and Viewer roles

                    ## Authentication

                    This API uses JWT (JSON Web Tokens) for authentication. To use authenticated endpoints:

                    1. Register a new account: `POST /api/v1/auth/signup`
                    2. Login to get tokens: `POST /api/v1/auth/login`
                    3. Use the access token in the Authorization header: `Bearer <token>`
                    4. Refresh your token when expired: `POST /api/v1/auth/refresh`

                    ## Rate Limiting

                    - Anonymous redirect requests: 100/minute per IP
                    - Authenticated API requests: 1000/hour per user
                    - Bulk operations: 10/hour per workspace

                    ## Support

                    For questions or issues, contact support@linkforge.io
                    """)
                .contact(new Contact()
                    .name("Linkforge Support")
                    .email("support@linkforge.io")
                    .url("https://linkforge.io"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development Server"),
                new Server()
                    .url("https://api.linkforge.io")
                    .description("Production Server")
            ))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token obtained from /api/v1/auth/login or /api/v1/auth/signup")))
            .addSecurityItem(new SecurityRequirement()
                .addList(securitySchemeName));
    }
}
