package com.knowgauge.core.service.testgeneration.prompt;

public interface PromptTemplateLoader {
    /**
     * @return the raw template content (e.g. with placeholders like {{topic}} or ${topic})
     */
    String loadTemplate(String templateId);

    /**
     * Optional convenience: whether a template exists.
     */
    boolean exists(String templateId);
}

