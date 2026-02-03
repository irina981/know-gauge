package com.knowgauge.core.service.content;


import java.io.InputStream;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.Topic;


public interface DocumentService {

    /**
     * Uploads a document and stores metadata + objectKey.
     * Ingestion/chunking can be triggered later (separate endpoint/job).
     */
    public Document uploadDocument(Document document, InputStream contentStream);
}

