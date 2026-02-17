package com.knowgauge.infra.embedding.openai.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

@Configuration
public class OpenAiEmbeddingConfig {

    @Bean
    @Qualifier("openAiEmbeddingModel")
    public EmbeddingModel openAiEmbeddingModel(
            @Value("${kg.embedding.openai.api-key}") String apiKey,
            @Value("${kg.embedding.openai.model:text-embedding-3-small}") String modelName,
            @Value("${kg.embedding.openai.base-url:}") String baseUrl,
            @Value("${kg.embedding.openai.timeout}") int modelTimeoutSeconds
    ) {
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(modelTimeoutSeconds));

        // baseUrl is optional (useful for proxies / OpenAI-compatible providers)
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }

        return builder.build();
    }
}

