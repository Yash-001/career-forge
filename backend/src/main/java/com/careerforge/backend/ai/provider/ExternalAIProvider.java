package com.careerforge.backend.ai.provider;

import com.careerforge.backend.ai.dto.*;

import java.util.List;

/**
 * Stub for a future external AI provider (e.g. OpenAI, Gemini, Claude).
 *
 * This class is intentionally NOT registered as a Spring bean.
 * It will be wired in a future phase when an external API key is available.
 * No network calls, SDK dependencies, or API keys are used here.
 */
public class ExternalAIProvider implements AIProvider {

    @Override
    public String providerName() {
        return "External AI (not configured)";
    }

    @Override
    public JobAnalysisResponse analyzeJobDescription(JobAnalysisRequest request) {
        throw new UnsupportedOperationException(
                "ExternalAIProvider is not yet implemented. Configure an API key and register this bean.");
    }

    @Override
    public TailoringResponse tailorResume(TailoringRequest request) {
        throw new UnsupportedOperationException(
                "ExternalAIProvider is not yet implemented. Configure an API key and register this bean.");
    }
}
