package com.knowgauge.core.service.testgeneration.schema;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ClasspathSchemaProvider implements SchemaProvider {

	private final String resourcePath;

	public ClasspathSchemaProvider(String basePath, String outputSchemaFile) {
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
