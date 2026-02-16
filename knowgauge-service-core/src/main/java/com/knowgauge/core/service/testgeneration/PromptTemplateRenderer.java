package com.knowgauge.core.service.testgeneration;

import java.util.Map;

public interface PromptTemplateRenderer {
    String render(String template, Map<String, Object> variables);
}
