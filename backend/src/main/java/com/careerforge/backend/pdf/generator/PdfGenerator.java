package com.careerforge.backend.pdf.generator;

import com.careerforge.backend.pdf.dto.ResumeVersionData;

/**
 * Generates a PDF document from a fully resolved ResumeVersionData.
 * Implementations must not access the database, enforce ownership, or apply billing limits.
 */
public interface PdfGenerator {
    byte[] generate(ResumeVersionData data);
}
