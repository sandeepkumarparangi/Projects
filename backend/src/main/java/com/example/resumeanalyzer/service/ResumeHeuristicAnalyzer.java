package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.dto.AnalyzeResumeResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ResumeHeuristicAnalyzer {
    private static final List<String> CORE_KEYWORDS = List.of(
            "java", "spring boot", "react", "sql", "api", "rest", "microservices",
            "aws", "docker", "kubernetes", "ci/cd", "testing", "agile"
    );
    private static final List<String> ACTION_VERBS = List.of(
            "built", "created", "delivered", "improved", "reduced", "increased", "automated", "designed"
    );
    private static final Pattern METRIC_PATTERN = Pattern.compile("(\\d+%|\\$\\d+|\\d+x|\\d+\\+)");

    public AnalyzeResumeResponse analyze(String resumeText, String jobDescription) {
        String normalizedResume = resumeText.toLowerCase(Locale.ROOT);
        String normalizedJob = jobDescription == null ? "" : jobDescription.toLowerCase(Locale.ROOT);

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        List<String> formattingTips = new ArrayList<>();

        int score = 45;
        if (containsAny(normalizedResume, CORE_KEYWORDS)) {
            score += 12;
            strengths.add("Includes technical keywords that ATS systems commonly parse.");
        }
        if (ACTION_VERBS.stream().anyMatch(normalizedResume::contains)) {
            score += 10;
            strengths.add("Uses action-oriented language in experience bullets.");
        } else {
            improvements.add("Start more bullets with action verbs such as built, automated, improved, or delivered.");
        }
        if (METRIC_PATTERN.matcher(normalizedResume).find()) {
            score += 12;
            strengths.add("Mentions measurable impact, which helps recruiters compare outcomes.");
        } else {
            improvements.add("Add metrics like latency reduced, revenue supported, users served, or defect reduction.");
        }
        if (normalizedResume.contains("experience") && normalizedResume.contains("skills")) {
            score += 8;
            strengths.add("Has recognizable sections for experience and skills.");
        } else {
            formattingTips.add("Use clear ATS-friendly headings: Summary, Skills, Experience, Projects, Education.");
        }
        if (resumeText.length() < 1800) {
            improvements.add("Add more role-specific detail; the resume text looks brief for an ATS scan.");
        }

        Set<String> suggestedKeywords = new LinkedHashSet<>(CORE_KEYWORDS);
        Set<String> missingKeywords = new LinkedHashSet<>();
        if (!normalizedJob.isBlank()) {
            extractImportantTerms(normalizedJob).forEach(term -> {
                suggestedKeywords.add(term);
                if (!normalizedResume.contains(term)) {
                    missingKeywords.add(term);
                }
            });
            score += Math.max(0, 13 - missingKeywords.size());
        }

        formattingTips.add("Keep layout simple: avoid text boxes, icons-as-labels, tables for core experience, and tiny fonts.");
        formattingTips.add("Mirror the job description wording naturally where your experience supports it.");

        int boundedScore = Math.max(0, Math.min(100, score));
        String verdict = boundedScore >= 80 ? "Strong ATS match"
                : boundedScore >= 65 ? "Good foundation"
                : "Needs targeted improvement";

        return new AnalyzeResumeResponse(
                boundedScore,
                verdict,
                strengths.isEmpty() ? List.of("Resume has readable text and can be parsed by the analyzer.") : strengths,
                improvements.isEmpty() ? List.of("Tune keywords for the exact job description before applying.") : improvements,
                List.copyOf(missingKeywords),
                suggestedKeywords.stream().limit(14).toList(),
                formattingTips,
                "Local analysis completed. Add OPENAI_API_KEY for deeper AI recommendations.",
                false,
                "local",
                null
        );
    }

    private boolean containsAny(String text, List<String> terms) {
        return terms.stream().anyMatch(text::contains);
    }

    private Set<String> extractImportantTerms(String text) {
        Set<String> terms = new LinkedHashSet<>();
        CORE_KEYWORDS.stream().filter(text::contains).forEach(terms::add);
        List.of("leadership", "cloud", "security", "observability", "postgresql", "mongodb", "typescript")
                .stream()
                .filter(text::contains)
                .forEach(terms::add);
        return terms;
    }
}
