package com.bibei.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final List<String> allowedOriginPatterns;

    public WebConfig(@Value("${bibei.cors.allowed-origins:}") String configuredOrigins) {
        this.allowedOriginPatterns = new ArrayList<>(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "capacitor://localhost"
        ));

        List<String> explicitOrigins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
        if (explicitOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException("BIBEI_CORS_ALLOWED_ORIGINS 不允许使用通配符");
        }
        this.allowedOriginPatterns.addAll(explicitOrigins);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Authorization");
    }
}
