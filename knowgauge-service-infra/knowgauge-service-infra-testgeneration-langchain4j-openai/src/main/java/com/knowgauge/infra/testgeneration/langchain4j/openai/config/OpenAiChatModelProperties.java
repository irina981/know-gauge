package com.knowgauge.infra.testgeneration.langchain4j.openai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kg.testgen.chat-model.openai")
public class OpenAiChatModelProperties {

	private String apiKey;
	private String model;
	private Double temperature;
	private Integer maxOutputTokens;
	private Integer timeoutSeconds;
	private Boolean strictJson;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Integer getMaxOutputTokens() {
		return maxOutputTokens;
	}

	public void setMaxOutputTokens(Integer maxOutputTokens) {
		this.maxOutputTokens = maxOutputTokens;
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(Integer timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public Boolean getStrictJson() {
		return strictJson;
	}

	public void setStrictJson(Boolean strictJson) {
		this.strictJson = strictJson;
	}
}
