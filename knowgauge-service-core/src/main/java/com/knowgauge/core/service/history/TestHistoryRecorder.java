package com.knowgauge.core.service.history;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.TestEvent;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionOption;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.TestEventType;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.TestEventRepository;

/**
 * Appends entries to the test history trail (ADR001). Joins the caller's
 * transaction, so events commit together with the change they describe.
 */
@Component
public class TestHistoryRecorder {

	private final TestEventRepository eventRepository;

	public TestHistoryRecorder(TestEventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	/**
	 * Records a test-level event.
	 */
	public TestEvent testEvent(Long tenantId, Long testId, TestEventType type, Long actorUserId,
			Map<String, Object> payload) {
		return eventRepository.append(base(tenantId, testId, null, type, actorUserId).payload(payload).build());
	}

	/**
	 * Records the test status change matching {@code newStatus}, if it has an
	 * event type.
	 */
	public void statusChanged(Long tenantId, Long testId, TestStatus newStatus, Long actorUserId,
			Map<String, Object> payload) {
		TestEventType type = switch (newStatus) {
		case GENERATED -> TestEventType.TEST_GENERATED;
		case REQUIRES_REVIEW -> TestEventType.TEST_REQUIRES_REVIEW;
		case REVIEWED -> TestEventType.TEST_REVIEWED;
		case FAILED -> TestEventType.TEST_FAILED;
		case CREATED -> TestEventType.TEST_CREATED;
		};
		testEvent(tenantId, testId, type, actorUserId, payload);
	}

	/**
	 * Records QUESTION_GENERATED plus one OPTION_FLAGGED per flag, for questions
	 * whose version 1 was just persisted. System actor.
	 */
	public void questionsGenerated(List<TestQuestion> savedQuestions) {
		List<TestEvent> events = new ArrayList<>();
		for (TestQuestion q : savedQuestions) {
			TestQuestionVersion v = q.getCurrentVersion();
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("questionIndex", q.getQuestionIndex());
			payload.put("generationRunId", v.getGenerationRunId());
			events.add(base(q.getTenantId(), q.getTestId(), q.getId(), TestEventType.QUESTION_GENERATED, null)
					.toVersionId(v.getId()).payload(payload).build());
			events.addAll(flagEvents(q, v, null));
		}
		if (!events.isEmpty()) {
			eventRepository.appendAll(events);
		}
	}

	/**
	 * Records a review action that moved a question from one version to another,
	 * plus OPTION_FLAGGED events for flags carried by the new version.
	 */
	public void versionChanged(TestQuestion question, TestEventType type, Long fromVersionId,
			TestQuestionVersion newVersion, Long actorUserId, String comment, Map<String, Object> payload) {
		List<TestEvent> events = new ArrayList<>();
		events.add(base(question.getTenantId(), question.getTestId(), question.getId(), type, actorUserId)
				.fromVersionId(fromVersionId).toVersionId(newVersion.getId()).comment(comment).payload(payload)
				.build());
		events.addAll(flagEvents(question, newVersion, null));
		eventRepository.appendAll(events);
	}

	/**
	 * Records a question-level event that did not create a version (e.g. a
	 * failed regeneration).
	 */
	public TestEvent questionEvent(TestQuestion question, TestEventType type, Long actorUserId, String comment,
			Map<String, Object> payload) {
		return eventRepository.append(base(question.getTenantId(), question.getTestId(), question.getId(), type,
				actorUserId).fromVersionId(question.getCurrentVersionId()).comment(comment).payload(payload).build());
	}

	private List<TestEvent> flagEvents(TestQuestion q, TestQuestionVersion v, Long actorUserId) {
		List<TestEvent> events = new ArrayList<>();
		for (OptionLetter letter : v.getFlaggedOptions()) {
			TestQuestionOption option = v.getOption(letter);
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("option", letter.name());
			payload.put("text", option.getText());
			payload.put("suggestedText", option.getSuggestedText());
			payload.put("reason", option.getFlagReason());
			payload.put("verificationRunId", v.getVerificationRunId());
			events.add(base(q.getTenantId(), q.getTestId(), q.getId(), TestEventType.OPTION_FLAGGED, actorUserId)
					.toVersionId(v.getId()).payload(payload).build());
		}
		return events;
	}

	private TestEvent.TestEventBuilder<?, ?> base(Long tenantId, Long testId, Long questionId, TestEventType type,
			Long actorUserId) {
		return TestEvent.builder().tenantId(tenantId).testId(testId).questionId(questionId).eventType(type)
				.actorUserId(actorUserId).createdAt(Instant.now());
	}
}
