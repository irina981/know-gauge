package com.knowgauge.core.service.testgeneration;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.knowgauge.core.port.testgeneration.TestQuestionSchemaProvider;

@Component
public class ClasspathTestQuestionSchemaProvider implements TestQuestionSchemaProvider {

	private final String resourcePath;

	public ClasspathTestQuestionSchemaProvider(
			@Value("${kg.llm.testgen.prompt.templates.classpathBase}") String basePath,
			@Value("${kg.llm.testgen.prompt.templates.output-schema-file:output-schema.json}") String outputSchemaFile) {
		String normalizedBasePath = basePath.endsWith("/") ? basePath : basePath + "/";
		this.resourcePath = normalizedBasePath + outputSchemaFile;
	}

	@Override
	public String getOutputSchemaJson() {
		InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
		if (is == null) {
			throw new IllegalStateException("Output schema not found on classpath: " + resourcePath);
		}

		try (is) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to read output schema: " + resourcePath, e);
		}
	}
}
