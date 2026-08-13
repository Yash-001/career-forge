package com.careerforge.backend.ai.config;

import com.careerforge.backend.ai.provider.AIProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AIConfig {

    /**
     * Selects the active AIProvider based on the {@code ai.provider} property.
     * Supported values: {@code demo} (default).
     * Future values: {@code openai}, {@code gemini}, etc.
     * Marked @Primary so Spring resolves AIProvider injection unambiguously.
     */
    @Bean
    @Primary
    public AIProvider activeAIProvider(
            @Qualifier("demoAIProvider") AIProvider demoAIProvider,
            @Value("${ai.provider:demo}") String providerName) {

        return switch (providerName.toLowerCase()) {
            case "demo" -> demoAIProvider;
            default -> throw new IllegalStateException(
                    "Unknown ai.provider value: '" + providerName + "'. Supported: demo");
        };
    }
}
