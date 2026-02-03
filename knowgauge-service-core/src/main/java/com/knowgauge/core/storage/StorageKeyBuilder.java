package com.knowgauge.core.storage;

import org.springframework.stereotype.Component;

@Component
public class StorageKeyBuilder {

    private final String pattern;

    public StorageKeyBuilder(StorageProperties props) {
        this.pattern = props.getStorageKeyTemplate();
    }

    public String build(long tenantId, long documentId, int version) {
        String shard3 = String.format("%03d", documentId % 1000);

        return pattern
                .replace("{tenantId}", String.valueOf(tenantId))
                .replace("{shard3}", shard3)
                .replace("{documentId}", String.valueOf(documentId))
                .replace("{version}", String.valueOf(version));
    }
}

