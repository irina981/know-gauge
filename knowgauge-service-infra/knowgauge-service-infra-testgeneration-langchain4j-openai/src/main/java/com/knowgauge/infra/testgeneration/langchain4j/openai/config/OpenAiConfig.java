package com.knowgauge.infra.testgeneration.langchain4j.openai.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.knowgauge.core.service.testgeneration.schema.SchemaProvider;
import com.knowgauge.infra.testgeneration.langchain4j.openai.service.OpenAiClient;
import com.knowgauge.infra.testgeneration.langchain4j.openai.service.OpenAiClientImpl;

@Configuration
public class OpenAiConfig {

	@Bean(name = "testGenerationOpenAiClient")
	public OpenAiClient testGenerationOpenAiClient(
			@Qualifier("testGenerationOutputSchemaProvider") SchemaProvider schemaProvider,
			@Value("${kg.testgen.chat-model.openai.structured-output-model-prefixes:gpt-4.1,gpt-4o,o1,o3,o4}") List<String> structuredOutputModelPrefixes) {
		return new OpenAiClientImpl(schemaProvider, structuredOutputModelPrefixes);
	}

	@Bean(name = "verificationOpenAiClient")
	public OpenAiClient verificationOpenAiClient(
			@Qualifier("verificationOutputSchemaProvider") SchemaProvider schemaProvider,
			@Value("${kg.testgen.chat-model.openai.structured-output-model-prefixes:gpt-4.1,gpt-4o,o1,o3,o4}") List<String> structuredOutputModelPrefixes) {
		return new OpenAiClientImpl(schemaProvider, structuredOutputModelPrefixes);
	}
}
