package com.knowgauge.core.service.testgeneration.prompt;

import java.util.Map;

public interface PromptTemplateRenderer {
    String render(String template, Map<String, Object> variables);
}
