package com.knowgauge.core.port.storage;

import java.io.InputStream;
import java.io.OutputStream;

public interface StorageService {
    StoredObject put(String objectKey, InputStream in, long size, String contentType);
    
    void download(String objectKey, OutputStream out);
    
    InputStream download(String objectKey);
    
    void delete(String objectKey);
    
    public void ensureBucketExists();

    record StoredObject(String objectKey, String etag, String versionId, long size, String contentType) {}
   
}
