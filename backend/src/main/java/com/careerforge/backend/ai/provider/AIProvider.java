package com.careerforge.backend.ai.provider;

import com.careerforge.backend.ai.dto.JobAnalysisRequest;
import com.careerforge.backend.ai.dto.JobAnalysisResponse;
import com.careerforge.backend.ai.dto.TailoringRequest;
import com.careerforge.backend.ai.dto.TailoringResponse;

/**
 * Provider abstraction for AI operations.
 * Implementations must not mutate any resume or profile entities.
 */
public interface AIProvider {

    /** Human-readable name for this provider (e.g. "Demo AI", "OpenAI"). */
    String providerName();

    /** Analyses a job description and returns extracted keywords, technologies, and role. */
    JobAnalysisResponse analyzeJobDescription(JobAnalysisRequest request);

    /** Produces tailored bullet suggestions for a resume version against a job description. */
    TailoringResponse tailorResume(TailoringRequest request);
}
