package com.knowgauge.core.service.testgeneration.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.core.service.content.DocumentService;
import com.knowgauge.core.service.content.TopicService;

/** Which documents a test is generated from: a topic stands for its whole subtree. */
class TestDraftValidatorSourcesTest {

	private static final long TENANT = 1L;
	private static final long ROOT = 9000L;
	private static final long CHILD_A = 9100L;
	private static final long CHILD_B = 9200L;

	private DocumentService documentService;
	private TopicService topicService;
	private TestDraftValidator validator;

	@BeforeEach
	void setUp() {
		documentService = mock(DocumentService.class);
		topicService = mock(TopicService.class);
		when(topicService.get(ROOT)).thenReturn(Optional.of(topic(ROOT, null)));
		when(topicService.getDescendants(ROOT)).thenReturn(List.of(topic(CHILD_A, ROOT), topic(CHILD_B, ROOT)));
		validator = new TestDraftValidator(documentService, topicService);
	}

	@Test
	void aParentTopicWithoutOwnDocumentsUsesTheDocumentsOfItsSubTopics() {
		when(documentService.findByTopicIds(TENANT, List.of(ROOT, CHILD_A, CHILD_B)))
				.thenReturn(List.of(doc(1L, CHILD_A, DocumentStatus.INGESTED), doc(2L, CHILD_B, DocumentStatus.INGESTED)));

		com.knowgauge.core.model.Test test = validator.validateAndExpandTopicsAndDocuments(TENANT, draft(List.of(ROOT)));

		assertThat(test.getDocumentIds()).containsExactly(1L, 2L);
		assertThat(test.getTopicIds()).containsExactly(ROOT, CHILD_A, CHILD_B);
	}

	@Test
	void documentsOfTopicsThatAreNotIngestedAreSkipped() {
		when(documentService.findByTopicIds(TENANT, List.of(ROOT, CHILD_A, CHILD_B)))
				.thenReturn(List.of(doc(1L, CHILD_A, DocumentStatus.INGESTED), doc(2L, CHILD_B, DocumentStatus.UPLOADED)));

		com.knowgauge.core.model.Test test = validator.validateAndExpandTopicsAndDocuments(TENANT, draft(List.of(ROOT)));

		assertThat(test.getDocumentIds()).containsExactly(1L);
	}

	@Test
	void failsWhenNoDocumentInTheSubtreeIsIngested() {
		when(documentService.findByTopicIds(TENANT, List.of(ROOT, CHILD_A, CHILD_B)))
				.thenReturn(List.of(doc(2L, CHILD_B, DocumentStatus.FAILED)));

		assertThatThrownBy(() -> validator.validateAndExpandTopicsAndDocuments(TENANT, draft(List.of(ROOT))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no ingested documents");
	}

	@Test
	void anExplicitlyChosenDocumentMustBeIngested() {
		when(documentService.get(5L)).thenReturn(Optional.of(doc(5L, CHILD_A, DocumentStatus.UPLOADED)));
		com.knowgauge.core.model.Test draft = draft(List.of());
		draft.setDocumentIds(List.of(5L));

		assertThatThrownBy(() -> validator.validateAndExpandTopicsAndDocuments(TENANT, draft))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not INGESTED");
	}

	private static com.knowgauge.core.model.Test draft(List<Long> topicIds) {
		return com.knowgauge.core.model.Test.builder().name("Test").topicIds(topicIds).questionCount(5).build();
	}

	private static Topic topic(long id, Long parentId) {
		return Topic.builder().id(id).tenantId(TENANT).parentId(parentId).name("T" + id).build();
	}

	private static Document doc(long id, long topicId, DocumentStatus status) {
		return Document.builder().id(id).tenantId(TENANT).topicId(topicId).status(status).build();
	}
}
