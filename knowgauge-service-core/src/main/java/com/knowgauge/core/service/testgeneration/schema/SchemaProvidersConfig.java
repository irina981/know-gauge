package com.knowgauge.core.service.testgeneration.schema;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchemaProvidersConfig {

	@Bean(name = "testGenerationOutputSchemaProvider")
	public SchemaProvider testGenerationOutputSchemaProvider(
			@Value("${kg.testgen.prompt.templates.classpathBase:prompttemplates/}") String basePath,
			@Value("${kg.testgen.prompt.templates.output-schema-file:mcq-output-template.json}") String outputSchemaFile) {
		return new TestGenerationOutputSchemaProvider(basePath, outputSchemaFile);
	}

	@Bean(name = "verificationOutputSchemaProvider")
	public SchemaProvider verificationOutputSchemaProvider(
			@Value("${kg.testgen.verification.prompt.templates.classpathBase:prompttemplates/}") String basePath,
			@Value("${kg.testgen.verification.prompt.templates.output-schema-file:verification-output-template.json}") String outputSchemaFile) {
		return new VerificationOutputSchemaProvider(basePath, outputSchemaFile);
	}
}
