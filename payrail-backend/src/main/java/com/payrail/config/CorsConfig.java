package com.payrail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${APP_CORS_ORIGINS:http://localhost:5173}")
    private String corsOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Example: APP_CORS_ORIGINS=https://payrail.vercel.app,https://payrail-xyz.vercel.app
        List<String> origins = new ArrayList<>();
        Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(origins::add);

        cfg.setAllowedOrigins(origins);

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        cfg.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        cfg.setExposedHeaders(List.of("Authorization"));

        // If later you move JWT to cookies, change to true and configure cookies properly
        cfg.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
