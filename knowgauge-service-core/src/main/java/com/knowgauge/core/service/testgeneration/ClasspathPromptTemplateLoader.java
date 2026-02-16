package com.knowgauge.core.service.testgeneration;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClasspathPromptTemplateLoader implements PromptTemplateLoader {

	private final String basePath;

	public ClasspathPromptTemplateLoader(
			@Value("${kg.testgeneration.prompt.templates.classpathBase}") String basePath) {
		this.basePath = basePath.endsWith("/") ? basePath : basePath + "/";
	}

	@Override
	public String loadTemplate(String templateId) {

		String resourcePath = basePath + templateId + ".txt";

		InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);

		if (is == null) {
			throw new IllegalStateException("Prompt template not found on classpath: " + resourcePath);
		}

		try (is) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to read template: " + resourcePath, e);
		}
	}

	@Override
	public boolean exists(String templateId) {
		String resourcePath = basePath + templateId + ".txt";

		InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);

		return is != null;
	}
}
