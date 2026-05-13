package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.config.AppProperties;
import com.example.resumeanalyzer.dto.AnalyzeResumeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OpenAiResumeAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiResumeAnalyzer.class);

    private final AppProperties appProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, AnalyzeResumeResponse> responseCache = new ConcurrentHashMap<>();

    public OpenAiResumeAnalyzer(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(appProperties.openai().timeoutMillis()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(appProperties.openai().timeoutMillis()));
        this.restClient = RestClient.builder()
                .baseUrl(appProperties.openai().baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Optional<AnalyzeResumeResponse> analyze(String resumeText, String jobDescription) {
        String apiKey = appProperties.openai().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        String cacheKey = cacheKey(resumeText, jobDescription);
        AnalyzeResumeResponse cached = responseCache.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        try {
            Map<String, Object> request = Map.of(
                    "model", appProperties.openai().model(),
                    "instructions", """
                            You are a fast ATS resume reviewer. Return compact valid JSON only.
                            Keep each array to 3-5 practical items. Score keyword alignment,
                            clarity, measurable impact, formatting, and role fit.
                            """,
                    "input", """
                            Resume:
                            %s

                            Job description:
                            %s
                            """.formatted(
                            truncate(resumeText, appProperties.openai().maxResumeChars()),
                            truncate(jobDescription, appProperties.openai().maxJobDescriptionChars())
                    ),
                    "text", Map.of("format", responseSchema()),
                    "max_output_tokens", appProperties.openai().maxOutputTokens()
            );

            ResponseEntity<JsonNode> entity = restClient.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .toEntity(JsonNode.class);

            HttpStatusCode statusCode = entity.getStatusCode();
            if (!statusCode.is2xxSuccessful()) {
                LOGGER.warn("OpenAI resume analysis returned status {}", statusCode.value());
                return Optional.empty();
            }

            JsonNode response = entity.getBody();

            String outputText = response == null ? null : response.path("output_text").asText(null);
            if (outputText == null || outputText.isBlank()) {
                outputText = findOutputText(response);
            }
            if (outputText == null || outputText.isBlank()) {
                return Optional.empty();
            }

            JsonNode json = objectMapper.readTree(outputText);
            AnalyzeResumeResponse analysis = new AnalyzeResumeResponse(
                    json.path("atsScore").asInt(0),
                    json.path("verdict").asText("Analysis complete"),
                    strings(json.path("strengths")),
                    strings(json.path("improvements")),
                    strings(json.path("missingKeywords")),
                    strings(json.path("suggestedKeywords")),
                    strings(json.path("formattingTips")),
                    json.path("summary").asText("OpenAI analysis completed."),
                    true,
                    "openai:" + appProperties.openai().model(),
                    null
            );
            responseCache.put(cacheKey, analysis);
            return Optional.of(analysis);
        } catch (Exception exception) {
            LOGGER.warn("OpenAI resume analysis failed; falling back to local analyzer: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    public boolean isConfigured() {
        String apiKey = appProperties.openai().apiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    private String findOutputText(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode output = response.path("output");
        for (JsonNode item : output) {
            for (JsonNode content : item.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText();
                }
            }
        }
        return null;
    }

    private List<String> strings(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return node.findValuesAsText("").stream().filter(value -> !value.isBlank()).toList();
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> stringArray = Map.of("type", "array", "items", Map.of("type", "string"));
        return Map.of(
                "type", "json_schema",
                "name", "resume_analysis",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of(
                                "atsScore", "verdict", "strengths", "improvements",
                                "missingKeywords", "suggestedKeywords", "formattingTips", "summary"
                        ),
                        "properties", Map.of(
                                "atsScore", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                                "verdict", Map.of("type", "string"),
                                "strengths", stringArray,
                                "improvements", stringArray,
                                "missingKeywords", stringArray,
                                "suggestedKeywords", stringArray,
                                "formattingTips", stringArray,
                                "summary", Map.of("type", "string")
                        )
                )
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String cacheKey(String resumeText, String jobDescription) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String rawKey = appProperties.openai().model()
                    + "\n" + truncate(resumeText, appProperties.openai().maxResumeChars())
                    + "\n" + truncate(jobDescription, appProperties.openai().maxJobDescriptionChars());
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return String.valueOf((resumeText + jobDescription).hashCode());
        }
    }
}
