package com.knowgauge.core.service.content;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.Document;


public interface DocumentService {

    /**
     * Uploads a document and stores metadata + objectKey.
     * Ingestion/chunking can be triggered later (separate endpoint/job).
     */
    public Document uploadDocument(Document document, InputStream contentStream);
    
    public Optional<Document> get(Long id);
    
    public Document download(Long id, OutputStream out);
    
    public void delete(Long topicId);
    
    public Page<Document> getAllDocuments(Long topicId, Pageable pageable);
}

