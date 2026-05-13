package com.example.resumeanalyzer.dto;

import java.util.List;

public record AnalyzeResumeResponse(
        int atsScore,
        String verdict,
        List<String> strengths,
        List<String> improvements,
        List<String> missingKeywords,
        List<String> suggestedKeywords,
        List<String> formattingTips,
        String summary,
        boolean aiPowered,
        String analysisSource,
        String diagnostic
) {
}
