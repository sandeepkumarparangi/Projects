package com.example.resumeanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendOrigin,
        OpenAi openai
) {
    public record OpenAi(
            String apiKey,
            String model,
            String baseUrl,
            int timeoutMillis,
            int maxResumeChars,
            int maxJobDescriptionChars,
            int maxOutputTokens
    ) {
    }
}
