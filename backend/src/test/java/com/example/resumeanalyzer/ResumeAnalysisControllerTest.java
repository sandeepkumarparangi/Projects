package com.example.resumeanalyzer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeAnalysisControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void analyzesUploadedResumeWithLocalFallback() throws Exception {
        MockMultipartFile resume = new MockMultipartFile(
                "resume",
                "sample-resume.txt",
                "text/plain",
                """
                Summary
                Full stack Java developer with Spring Boot, React, REST API, SQL, Docker, and CI/CD experience.

                Experience
                Built Spring Boot microservices and React dashboards for internal operations.
                Improved API response time by 35% through query optimization and caching.
                Automated deployments with GitHub Actions and Docker.

                Skills
                Java, Spring Boot, React, TypeScript, SQL, REST, Docker, testing, agile
                """.getBytes()
        );

        mockMvc.perform(multipart("/api/resumes/analyze")
                        .file(resume)
                        .param("jobDescription", """
                                Looking for a Java Spring Boot React developer with Docker, SQL,
                                TypeScript, GitHub Actions, security, and cloud experience.
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atsScore", greaterThanOrEqualTo(70)))
                .andExpect(jsonPath("$.verdict").isNotEmpty())
                .andExpect(jsonPath("$.aiPowered").value(false))
                .andExpect(jsonPath("$.analysisSource").value("local"))
                .andExpect(jsonPath("$.suggestedKeywords", hasItem("spring boot")))
                .andExpect(jsonPath("$.missingKeywords", hasItem("security")));
    }
}
