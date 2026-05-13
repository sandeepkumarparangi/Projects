package com.example.resumeanalyzer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns())
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }

    private String[] allowedOriginPatterns() {
        Set<String> origins = new LinkedHashSet<>();
        origins.add("http://localhost:*");
        origins.add("http://127.0.0.1:*");
        origins.add("https://sandeepkumarparangi.github.io");

        String configuredOrigins = appProperties.frontendOrigin();
        if (StringUtils.hasText(configuredOrigins)) {
            Arrays.stream(configuredOrigins.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(origins::add);
        }

        return origins.toArray(String[]::new);
    }
}
