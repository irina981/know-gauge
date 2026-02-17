package com.knowgauge.infra.testgeneration.openai.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Configuration
public class OpenAiTestGenerationConfig {

    @Bean
    @Qualifier("testGenerationChatModel")
    public ChatModel testGenerationChatModel(
            @Value("${kg.testgen.openai.api-key}") String apiKey,
            @Value("${kg.testgen.openai.model}") String model,
            @Value("${kg.testgen.openai.temperature:0.2}") Double temperature,
            @Value("${kg.testgen.openai.timeout-seconds:60}") Integer timeoutSeconds
    ) {

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
