package com.knowgauge.core.support;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionOption;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.model.enums.VersionOrigin;

/**
 * Builders for test data shared by the core unit tests.
 */
public final class QuestionFixtures {

	public static final long TENANT = 1L;
	public static final long TEST_ID = 100L;

	private QuestionFixtures() {
	}

	public static Test test(AnswerCardinality cardinality, TestStatus status) {
		return Test.builder().id(TEST_ID).tenantId(TENANT).answerCardinality(cardinality).status(status)
				.questionCount(10).minMultipleCorrectQuestionsCount(0).build();
	}

	/**
	 * Draft version with four options; {@code correct} letters form the answer key.
	 */
	public static TestQuestionVersion version(String questionText, Set<OptionLetter> correct, String... texts) {
		TestQuestionVersion v = new TestQuestionVersion();
		v.setTenantId(TENANT);
		v.setQuestionText(questionText);
		v.setExplanation("Because the context says so.");
		v.setSourceChunkIds(List.of(11L, 12L));
		EnumMap<OptionLetter, TestQuestionOption> options = new EnumMap<>(OptionLetter.class);
		OptionLetter[] letters = OptionLetter.values();
		for (int i = 0; i < letters.length; i++) {
			options.put(letters[i], TestQuestionOption.builder().letter(letters[i]).text(texts[i])
					.correct(correct.contains(letters[i])).build());
		}
		v.setOptions(options);
		return v;
	}

	public static TestQuestionVersion defaultVersion() {
		return version("Which collection keeps insertion order in Java?", Set.of(OptionLetter.A), "LinkedHashMap",
				"HashMap", "TreeSet", "PriorityQueue");
	}

	/**
	 * Persisted-looking question (ids set) whose current version is {@code v}.
	 */
	public static TestQuestion persisted(long questionId, int index, long versionId, TestQuestionVersion v) {
		v.setId(versionId);
		v.setQuestionId(questionId);
		v.setVersionNo(1);
		v.setOrigin(VersionOrigin.GENERATED);
		TestQuestion q = TestQuestion.builder().id(questionId).tenantId(TENANT).testId(TEST_ID).questionIndex(index)
				.currentVersionId(versionId).currentVersion(v).build();
		q.setStatus(q.deriveStatus());
		return q;
	}

	public static TestQuestion draft(TestQuestionVersion v) {
		return TestQuestion.builder().tenantId(TENANT).testId(TEST_ID).currentVersion(v).build();
	}
}
