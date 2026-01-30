package com.knowgauge.core.port.service.storage;

import java.io.InputStream;

public interface StorageService {
    StoredObject put(String objectKey, InputStream in, long size, String contentType);

    record StoredObject(String objectKey, long size, String contentType) {}
}
