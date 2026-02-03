package com.knowgauge.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String objectKeyTemplate;

    public String getObjectKeyTemplate() {
        return objectKeyTemplate;
    }

    public void setObjectKeyTemplate(String objectKeyTemplate) {
        this.objectKeyTemplate = objectKeyTemplate;
    }
}
