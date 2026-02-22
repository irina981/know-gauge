package com.knowgauge.core.service.testgeneration.prompt;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

/**
 * Simple template renderer using {{variable}} placeholders.
 *
 * Example: Template: "Topic: {{topic}}\nContext:\n{{context}}"
 *
 * Variables: topic=Java context=JVM manages memory...
 *
 * Result: "Topic: Java\nContext:\nJVM manages memory..."
 */
@Component
public class DefaultPromptTemplateRenderer implements PromptTemplateRenderer {

	@Override
	public String render(String template, Map<String, Object> variables) {

		Objects.requireNonNull(template, "template must not be null");

		if (variables == null || variables.isEmpty()) {
			return template;
		}

		String result = template;

		for (Map.Entry<String, Object> entry : variables.entrySet()) {

			String key = entry.getKey();

			if (key == null || key.isBlank()) {
				continue;
			}

			String placeholder = "{{" + key + "}}";

			String value = stringify(entry.getValue());

			result = result.replace(placeholder, value);
		}

		return result;
	}

	private String stringify(Object value) {

		if (value == null) {
			return "";
		}

		if (value instanceof String s) {
			return s;
		}

		return String.valueOf(value);
	}
}
