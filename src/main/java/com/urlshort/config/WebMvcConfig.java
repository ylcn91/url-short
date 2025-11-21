package com.urlshort.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for the URL shortener application.
 * Configures interceptors, CORS, and other web-related settings.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * Registers interceptors for request processing.
     * Currently registers rate limiting interceptor for all endpoints.
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/**")  // Apply to all paths
            .excludePathPatterns(
                "/actuator/**",      // Exclude actuator endpoints
                "/swagger-ui/**",    // Exclude Swagger UI
                "/v3/api-docs/**",   // Exclude OpenAPI docs
                "/error"             // Exclude error endpoint
            );
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings.
     * In production, this should be configured via application.yml
     * with specific allowed origins.
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")  // Configure specific origins in production
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
