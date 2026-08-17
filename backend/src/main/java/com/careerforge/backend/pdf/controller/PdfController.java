package com.careerforge.backend.pdf.controller;

import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.pdf.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class PdfController {

    private final PdfExportService pdfExportService;

    @GetMapping("/{resumeId}/versions/{versionId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId) {

        byte[] pdf = pdfExportService.exportVersion(user, resumeId, versionId);
        String filename = pdfExportService.buildFilename(user, resumeId, versionId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
