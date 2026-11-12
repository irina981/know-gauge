package com.knowgauge.infra.testgeneration.langchain4j.openai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Local Ollama model used for distractor verification when
 * {@code kg.testgen.verification.provider=ollama}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kg.testgen.verification.chat-model.ollama")
public class OllamaVerificationModelProperties {

	private String baseUrl = "http://localhost:11434";
	private String model = "qwen2.5:7b-instruct";
	private Double temperature = 0.0;
	private Integer maxOutputTokens;
	private Integer timeoutSeconds = 120;
}
