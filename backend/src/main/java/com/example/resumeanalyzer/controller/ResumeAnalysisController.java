package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.dto.AnalyzeResumeResponse;
import com.example.resumeanalyzer.service.ResumeAnalysisService;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@Validated
public class ResumeAnalysisController {
    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeAnalysisController(ResumeAnalysisService resumeAnalysisService) {
        this.resumeAnalysisService = resumeAnalysisService;
    }

    @PostMapping(path = "/api/resumes/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalyzeResumeResponse analyze(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam(value = "jobDescription", required = false)
            @Size(max = 12000, message = "Job description is too long") String jobDescription
    ) {
        return resumeAnalysisService.analyze(resume, jobDescription);
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "ai-resume-analyzer");
    }
}
