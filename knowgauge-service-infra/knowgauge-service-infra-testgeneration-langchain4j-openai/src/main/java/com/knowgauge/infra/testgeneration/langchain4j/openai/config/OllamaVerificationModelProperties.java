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
	private Integer timeoutSeconds = 600;
	/**
	 * Context window (prompt + reply) in tokens. Ollama's default of 4096 can be
	 * too small once the source chunks are in the prompt, and Ollama then drops
	 * the start of the prompt silently.
	 */
	private Integer contextLength = 8192;
}
