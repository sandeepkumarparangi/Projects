package com.example.resumeanalyzer;

import com.example.resumeanalyzer.dto.AnalyzeResumeResponse;
import com.example.resumeanalyzer.service.ResumeHeuristicAnalyzer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeHeuristicAnalyzerTest {
    private final ResumeHeuristicAnalyzer analyzer = new ResumeHeuristicAnalyzer();

    @Test
    void returnsUsefulLocalAnalysisWithoutOpenAi() {
        AnalyzeResumeResponse response = analyzer.analyze(
                """
                Summary
                Java developer with Spring Boot and React experience.
                Experience
                Built REST APIs and improved deployment speed by 35%.
                Skills
                Java, Spring Boot, React, SQL, Docker
                """,
                "Looking for Java Spring Boot React Docker SQL API experience"
        );

        assertThat(response.atsScore()).isGreaterThan(60);
        assertThat(response.aiPowered()).isFalse();
        assertThat(response.analysisSource()).isEqualTo("local");
        assertThat(response.suggestedKeywords()).contains("java", "spring boot", "react");
    }
}
