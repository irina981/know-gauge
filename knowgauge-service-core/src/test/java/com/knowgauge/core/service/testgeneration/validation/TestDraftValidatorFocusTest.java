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
import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.service.content.DocumentService;
import com.knowgauge.core.service.content.TopicService;

/** The focus text of FOCUSED coverage. */
class TestDraftValidatorFocusTest {

	private static final long TENANT = 1L;

	private TestDraftValidator validator;

	@BeforeEach
	void setUp() {
		DocumentService documentService = mock(DocumentService.class);
		Document doc = Document.builder().id(10L).tenantId(TENANT).status(DocumentStatus.INGESTED).build();
		when(documentService.get(10L)).thenReturn(Optional.of(doc));
		validator = new TestDraftValidator(documentService, mock(TopicService.class));
	}

	@Test
	void focusedKeepsTheTrimmedFocusText() {
		com.knowgauge.core.model.Test test = draft(TestCoverageMode.FOCUSED, "  thread pools  ");

		validator.validateAndExpandTopicsAndDocuments(TENANT, test);

		assertThat(test.getFocusQuery()).isEqualTo("thread pools");
	}

	@Test
	void focusedRequiresAFocusText() {
		assertThatThrownBy(() -> validator.validateAndExpandTopicsAndDocuments(TENANT,
				draft(TestCoverageMode.FOCUSED, "   "))).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("focusQuery is required");
	}

	@Test
	void focusedRejectsAnOverlongFocusText() {
		String tooLong = "x".repeat(TestDraftValidator.MAX_FOCUS_QUERY_LENGTH + 1);

		assertThatThrownBy(() -> validator.validateAndExpandTopicsAndDocuments(TENANT,
				draft(TestCoverageMode.FOCUSED, tooLong))).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at most");
	}

	@Test
	void balancedModesDropTheFocusText() {
		com.knowgauge.core.model.Test test = draft(TestCoverageMode.BALANCED_PER_DOCS, "thread pools");

		validator.validateAndExpandTopicsAndDocuments(TENANT, test);

		assertThat(test.getFocusQuery()).isNull();
	}

	private static com.knowgauge.core.model.Test draft(TestCoverageMode mode, String focus) {
		return com.knowgauge.core.model.Test.builder().name("Test").documentIds(List.of(10L)).questionCount(5)
				.coverageMode(mode).focusQuery(focus).build();
	}
}
