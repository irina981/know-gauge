package com.knowgauge.core.service.testgeneration.validation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.core.service.content.DocumentService;
import com.knowgauge.core.service.content.TopicService;

/**
 * Validates Test draft BEFORE generation begins.
 *
 * Responsibilities:
 * - Validate test draft structure (sources + questionCount)
 * - Ensure topics/documents exist and belong to tenant
 * - Resolve all source documents (explicit documentIds + documents derived from topicIds)
 * - Ensure all resolved source documents are in INGESTED state
 *
 * Returns the resolved documentIds that should be considered as generation sources.
 *
 * Thread-safe and stateless.
 */
@Component
public class TestDraftValidator {

    private final DocumentService documentService;
    private final TopicService topicService;

    public TestDraftValidator(DocumentService documentService, TopicService topicService) {
        this.documentService = documentService;
        this.topicService = topicService;
    }

    public Test validateAndExpandTopicsAndDocuments(Long tenantId, Test testDraft) {

        validateTestNotNull(testDraft);

        
        List<Long> topicIds = safeList(testDraft.getTopicIds());
        List<Long> explicitDocumentIds = safeList(testDraft.getDocumentIds());

        validateAtLeastOneSourceSelected(topicIds, explicitDocumentIds);
        validateQuestionCount(testDraft);

        List<Topic> topics = validateTopicsExistAndBelongToTenant(tenantId, topicIds);

        List<Document> explicitDocuments = validateDocumentsExistAndBelongToTenant(tenantId, explicitDocumentIds);

        List<Document> topicDocuments = resolveDocumentsFromTopics(tenantId, topics);

        List<Document> allSourceDocuments = mergeDocuments(explicitDocuments, topicDocuments);

        validateNonEmptySources(allSourceDocuments);
        validateAllDocumentsIngested(allSourceDocuments);
        testDraft.setDocumentIds(allSourceDocuments.stream().map(doc -> doc.getId()).toList());

        List<Long> expandedTopicIds = expandTopicIdsWithDescendants(tenantId, topicIds);
        testDraft.setTopicIds(expandedTopicIds);
        
        return testDraft;
    }
    
    // ---------------------------
    // Expand topic IDs with all the descendants' IDs
    // ---------------------------
    
    private List<Long> expandTopicIdsWithDescendants(Long tenantId, List<Long> topicIds) {

        if (topicIds == null || topicIds.isEmpty()) return List.of();

        Set<Long> expanded = new LinkedHashSet<>();

        topicIds.stream()
            .filter(Objects::nonNull)
            .forEach(rootId -> {

                expanded.add(rootId);

                topicService.getDescendants(rootId).stream()
                    .filter(t -> tenantId.equals(t.getTenantId()))
                    .map(Topic::getId)
                    .forEach(expanded::add);
            });

        return List.copyOf(expanded);
    }


    // ---------------------------
    // Individual validation steps
    // ---------------------------

    private void validateTestNotNull(Test test) {
        if (test == null) {
            throw new IllegalArgumentException("Test must not be null");
        }
    }

    private void validateAtLeastOneSourceSelected(List<Long> topicIds, List<Long> documentIds) {
        if (topicIds.isEmpty() && documentIds.isEmpty()) {
            throw new IllegalArgumentException("Test must specify at least one topicId or documentId.");
        }
    }

    private void validateQuestionCount(Test test) {
        if (test.getQuestionCount() == null || test.getQuestionCount() <= 0) {
            throw new IllegalArgumentException("Test.questionCount must be > 0");
        }
    }

    private List<Topic> validateTopicsExistAndBelongToTenant(Long tenantId, List<Long> topicIds) {

        List<Topic> topics = new ArrayList<>();

        for (Long topicId : topicIds) {

            Topic topic = topicService.get(topicId)
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicId));

            validateBelongsToTenant("Topic", topicId, tenantId, topic.getTenantId());

            topics.add(topic);
        }

        return topics;
    }

    private List<Document> validateDocumentsExistAndBelongToTenant(Long tenantId, List<Long> documentIds) {

        List<Document> documents = new ArrayList<>();

        for (Long documentId : documentIds) {

            Document document = documentService.get(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

            validateBelongsToTenant("Document", documentId, tenantId, document.getTenantId());

            documents.add(document);
        }

        return documents;
    }

    private List<Document> resolveDocumentsFromTopics(Long tenantId, List<Topic> topics) {

        if (topics.isEmpty()) {
            return List.of();
        }

        List<Long> topicIds = topics.stream().map(Topic::getId).toList();

        // Requires: DocumentService.findByTopicIds(tenantId, topicIds)
        List<Document> docs = documentService.findByTopicIds(tenantId, topicIds);

        for (Document d : docs) {
            validateBelongsToTenant("Document", d.getId(), tenantId, d.getTenantId());
        }

        return docs;
    }

    private void validateNonEmptySources(List<Document> sourceDocuments) {
        if (sourceDocuments.isEmpty()) {
            throw new IllegalArgumentException("No source documents found for selected topics/documents.");
        }
    }

    private void validateAllDocumentsIngested(List<Document> sourceDocuments) {
        for (Document document : sourceDocuments) {
            if (document.getStatus() != DocumentStatus.INGESTED) {
                throw new IllegalArgumentException(
                        "Document " + document.getId() + " is not INGESTED (status=" + document.getStatus() + ")");
            }
        }
    }

    private void validateBelongsToTenant(String entity, Long entityId, Long expectedTenantId, Long actualTenantId) {
        if (!expectedTenantId.equals(actualTenantId)) {
            throw new IllegalArgumentException(entity + " " + entityId + " does not belong to tenant " + expectedTenantId);
        }
    }

    // ---------------------------
    // Helpers
    // ---------------------------

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private List<Document> mergeDocuments(List<Document> a, List<Document> b) {

        Set<Long> seen = new LinkedHashSet<>();
        List<Document> result = new ArrayList<>();

        for (Document d : a) {
            if (d != null && d.getId() != null && seen.add(d.getId())) {
                result.add(d);
            }
        }

        for (Document d : b) {
            if (d != null && d.getId() != null && seen.add(d.getId())) {
                result.add(d);
            }
        }

        return result;
    }
}
