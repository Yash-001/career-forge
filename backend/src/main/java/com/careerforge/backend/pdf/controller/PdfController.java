package com.careerforge.backend.pdf.controller;

import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.pdf.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

        // SEC: Use RFC 5987 filename* encoding to prevent header injection.
        // The ASCII fallback (filename=) uses the sanitized name from buildFilename.
        // The filename* parameter handles non-ASCII characters safely.
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + filename + "\"; "
                + "filename*=UTF-8''" + encodedFilename;

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(pdf);
    }
}
