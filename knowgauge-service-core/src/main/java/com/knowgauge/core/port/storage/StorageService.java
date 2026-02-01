package com.knowgauge.core.port.storage;

import java.io.InputStream;

public interface StorageService {
    StoredObject put(String objectKey, InputStream in, long size, String contentType);

    record StoredObject(String objectKey, String etag, String versionId, long size, String contentType) {}
   
}
