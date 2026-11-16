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
import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.service.content.DocumentService;
import com.knowgauge.core.service.content.TopicService;

/**
 * Validates Test draft BEFORE generation begins.
 *
 * Responsibilities:
 * - Validate test draft structure (name, description, sources + questionCount)
 * - Ensure topics/documents exist and belong to tenant
 * - Resolve all source documents: explicit documentIds (which must be INGESTED) plus the INGESTED documents of
 *   the selected topics and all their sub-topics (others are skipped)
 * - Fail when no ingested source document remains
 *
 * Returns the resolved documentIds that should be considered as generation sources.
 *
 * Thread-safe and stateless.
 */
@Component
public class TestDraftValidator {

    static final int MAX_NAME_LENGTH = 200;
    static final int MAX_DESCRIPTION_LENGTH = 2000;
    static final int MAX_FOCUS_QUERY_LENGTH = 2000;

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

        normalizeAndValidateNameAndDescription(testDraft);
        normalizeAndValidateFocusQuery(testDraft);
        validateAtLeastOneSourceSelected(topicIds, explicitDocumentIds);
        validateQuestionCount(testDraft);

        validateTopicsExistAndBelongToTenant(tenantId, topicIds);

        // Explicitly chosen documents must be ingested: the user asked for exactly these.
        List<Document> explicitDocuments = validateDocumentsExistAndBelongToTenant(tenantId, explicitDocumentIds);
        validateAllDocumentsIngested(explicitDocuments);

        // A topic stands for its whole subtree, so a parent topic without documents of its own works too. Documents
        // under it that are not ingested (yet) are skipped instead of failing the whole test.
        List<Long> expandedTopicIds = expandTopicIdsWithDescendants(tenantId, topicIds);
        List<Document> topicDocuments = resolveIngestedDocumentsFromTopics(tenantId, expandedTopicIds);

        List<Document> allSourceDocuments = mergeDocuments(explicitDocuments, topicDocuments);

        validateNonEmptySources(allSourceDocuments);
        testDraft.setDocumentIds(allSourceDocuments.stream().map(doc -> doc.getId()).toList());
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

    /**
     * Trims name and description. The name is required (clients supply the
     * default); a blank description is stored as null.
     */
    private void normalizeAndValidateNameAndDescription(Test test) {
        String name = test.getName() == null ? "" : test.getName().strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Test.name must not be blank.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Test.name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        test.setName(name);

        String description = test.getDescription() == null ? null : test.getDescription().strip();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Test.description must be at most " + MAX_DESCRIPTION_LENGTH + " characters.");
        }
        test.setDescription(description == null || description.isEmpty() ? null : description);
    }

    /**
     * FOCUSED coverage needs the text to search for; the other modes ignore it,
     * so it is dropped for them.
     */
    private void normalizeAndValidateFocusQuery(Test test) {
        if (test.getCoverageMode() != TestCoverageMode.FOCUSED) {
            test.setFocusQuery(null);
            return;
        }
        String focus = test.getFocusQuery() == null ? "" : test.getFocusQuery().strip();
        if (focus.isEmpty()) {
            throw new IllegalArgumentException(
                    "Test.focusQuery is required for FOCUSED coverage: describe what the questions should be about.");
        }
        if (focus.length() > MAX_FOCUS_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "Test.focusQuery must be at most " + MAX_FOCUS_QUERY_LENGTH + " characters.");
        }
        test.setFocusQuery(focus);
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

    /** Ingested documents of the given topics (the selected topics plus all their descendants). */
    private List<Document> resolveIngestedDocumentsFromTopics(Long tenantId, List<Long> topicIds) {

        if (topicIds.isEmpty()) {
            return List.of();
        }

        List<Document> docs = documentService.findByTopicIds(tenantId, topicIds);

        for (Document d : docs) {
            validateBelongsToTenant("Document", d.getId(), tenantId, d.getTenantId());
        }

        return docs.stream().filter(d -> d.getStatus() == DocumentStatus.INGESTED).toList();
    }

    private void validateNonEmptySources(List<Document> sourceDocuments) {
        if (sourceDocuments.isEmpty()) {
            throw new IllegalArgumentException(
                    "The selected topics and their sub-topics have no ingested documents. Ingest documents first.");
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
