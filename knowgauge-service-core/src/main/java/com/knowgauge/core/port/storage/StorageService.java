package com.knowgauge.core.port.storage;

import java.io.InputStream;
import java.io.OutputStream;

public interface StorageService {
    StoredObject put(String objectKey, InputStream in, long size, String contentType);
    
    void download(String objectKey, OutputStream out);
    
    void delete(String objectKey);

    record StoredObject(String objectKey, String etag, String versionId, long size, String contentType) {}
   
}
