package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.dto.AnalyzeResumeResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeAnalysisService {
    private final ResumeTextExtractor resumeTextExtractor;
    private final ResumeHeuristicAnalyzer heuristicAnalyzer;
    private final OpenAiResumeAnalyzer openAiResumeAnalyzer;

    public ResumeAnalysisService(
            ResumeTextExtractor resumeTextExtractor,
            ResumeHeuristicAnalyzer heuristicAnalyzer,
            OpenAiResumeAnalyzer openAiResumeAnalyzer
    ) {
        this.resumeTextExtractor = resumeTextExtractor;
        this.heuristicAnalyzer = heuristicAnalyzer;
        this.openAiResumeAnalyzer = openAiResumeAnalyzer;
    }

    public AnalyzeResumeResponse analyze(MultipartFile resume, String jobDescription) {
        String resumeText = resumeTextExtractor.extract(resume);
        if (resumeText.isBlank()) {
            throw new ResumeAnalysisException("The uploaded resume did not contain readable text.");
        }

        return openAiResumeAnalyzer.analyze(resumeText, jobDescription)
                .orElseGet(() -> {
                    AnalyzeResumeResponse local = heuristicAnalyzer.analyze(resumeText, jobDescription);
                    String diagnostic = openAiResumeAnalyzer.isConfigured()
                            ? "OpenAI request was unavailable or timed out; using local analyzer."
                            : "OPENAI_API_KEY is not configured; using local analyzer.";
                    return new AnalyzeResumeResponse(
                            local.atsScore(),
                            local.verdict(),
                            local.strengths(),
                            local.improvements(),
                            local.missingKeywords(),
                            local.suggestedKeywords(),
                            local.formattingTips(),
                            local.summary(),
                            local.aiPowered(),
                            local.analysisSource(),
                            diagnostic
                    );
                });
    }
}
