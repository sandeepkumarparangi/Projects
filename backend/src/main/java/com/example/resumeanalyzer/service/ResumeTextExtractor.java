package com.example.resumeanalyzer.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class ResumeTextExtractor {
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    public String extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeAnalysisException("Upload a resume file to analyze.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResumeAnalysisException("Resume file must be 5 MB or smaller.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            byte[] bytes = file.getBytes();
            if (filename.endsWith(".pdf")) {
                return extractPdf(bytes);
            }
            if (filename.endsWith(".docx")) {
                return extractDocx(bytes);
            }
            if (filename.endsWith(".txt")) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new ResumeAnalysisException("Could not read the uploaded resume.", exception);
        }

        throw new ResumeAnalysisException("Unsupported file type. Upload PDF, DOCX, or TXT.");
    }

    private String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .reduce("", (left, right) -> left + System.lineSeparator() + right);
        }
    }
}
