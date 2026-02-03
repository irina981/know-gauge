package com.knowgauge.core.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String storageKeyTemplate;

    public String getStorageKeyTemplate() {
        return storageKeyTemplate;
    }

    public void setStorageKeyTemplate(String storageKeyTemplate) {
        this.storageKeyTemplate = storageKeyTemplate;
    }
}
