package com.example.payment_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * ═════════════════════════════════════════════════════════════════════════════
 * CHANGE COMMENT: NEW CLASS - CORS CONFIGURATION
 * ═════════════════════════════════════════════════════════════════════════════
 * 
 * Purpose: Enable Cross-Origin Resource Sharing (CORS) for the API
 * 
 * This configuration allows frontend applications running on different domains
 * to make HTTP requests to this backend API. Without CORS, browsers block 
 * cross-origin requests by default for security reasons.
 * 
 * Implementation:
 * 1. Implements WebMvcConfigurer to customize Spring MVC configuration
 * 2. Provides two CORS configuration approaches:
 *    a) addCorsMappings() - Annotation-based approach
 *    b) corsConfigurationSource() - Bean-based approach for filter-level config
 * 
 * Security Considerations:
 * - CORS sources are restricted to specific whitelisted origins
 * - Only necessary HTTP methods are allowed
 * - Credentials (cookies, auth headers) are explicitly enabled
 * - Preflight requests are cached to reduce overhead
 * 
 * Configured for:
 * - Development: localhost:3000, 4200, 5173, 8000 (React, Angular, Vite)
 * - Production: https://yourdomain.com (update with your domain)
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * CHANGE: Override addCorsMappings to configure CORS globally
     * 
     * This method is called during Spring startup to register CORS mappings
     * for all endpoints that match the pattern "/**"
     * 
     * @param registry - CorsRegistry to add CORS configuration to
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CHANGE: Register CORS mapping for all endpoints
        registry.addMapping("/**")
                // CHANGE: Allow requests from these specific origins (whitelisted)
                // Update these origins for your environment:
                //   - Development: http://localhost:3000, 4200, 5173
                //   - Production: https://yourdomain.com
                .allowedOrigins(
                        "http://localhost:3000",      // React development server (Create React App default)
                        "http://localhost:4200",      // Angular development server default
                        "http://localhost:8000",      // Alternative development port
                        "http://localhost:5173",      // Vite development server default
                        "https://localhost:3000",     // HTTPS local development
                        "https://localhost:4200",     // HTTPS local development
                        // CHANGE: Add production domains here
                        "https://yourdomain.com",
                        "https://www.yourdomain.com"
                )
                // CHANGE: Allow these HTTP methods from cross-origin requests
                // OPTIONS: Used for preflight requests, GET: Read data, POST: Submit data, 
                // PUT: Update data, DELETE: Remove data, PATCH: Partial update
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // CHANGE: Allow all headers to be sent in cross-origin requests
                // This allows custom headers like Authorization, Content-Type, etc.
                .allowedHeaders("*")
                // CHANGE: Allow credentials (cookies, authorization headers) in cross-origin requests
                // Important: When set to true, allowedOrigins cannot be "*" (must be specific)
                .allowCredentials(true)
                // CHANGE: Cache preflight response for 3600 seconds (1 hour)
                // This reduces overhead by caching OPTIONS request results
                .maxAge(3600)
                // CHANGE: Expose these headers to the client's JavaScript code
                // By default, browsers only expose basic headers; these must be explicitly exposed
                .exposedHeaders("Authorization", "X-Custom-Header");
    }

    /**
     * CHANGE: Alternative CORS configuration using CorsConfigurationSource bean
     * 
     * This bean-based approach provides CORS configuration at the filter level,
     * which is useful for Spring Security integration and more fine-grained control.
     * 
     * This method creates a bean that Spring uses to configure CORS globally
     * for the entire application.
     * 
     * @return CorsConfigurationSource - Configuration source for CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // CHANGE: Create new CORS configuration instance
        CorsConfiguration configuration = new CorsConfiguration();
        
        // CHANGE: Set allowed origins (same as addCorsMappings)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8000",
                "http://localhost:5173",
                "https://localhost:3000",
                "https://localhost:4200",
                // CHANGE: Update these for production
                "https://yourdomain.com",
                "https://www.yourdomain.com"
        ));
        
        // CHANGE: Set allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // CHANGE: Set allowed headers (allow all with "*")
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // CHANGE: Enable credentials support
        configuration.setAllowCredentials(true);
        
        // CHANGE: Set max age for preflight cache (3600 seconds = 1 hour)
        configuration.setMaxAge(3600L);
        
        // CHANGE: Set headers that should be exposed to client
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Custom-Header"));
        
        // CHANGE: Register configuration for all endpoints ("/**")
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

