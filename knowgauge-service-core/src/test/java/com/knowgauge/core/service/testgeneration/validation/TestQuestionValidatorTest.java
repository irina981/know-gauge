package com.knowgauge.core.service.testgeneration.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionOption;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.support.QuestionFixtures;

class TestQuestionValidatorTest {

	private final TestQuestionValidator validator = new TestQuestionValidator();

	private final com.knowgauge.core.model.Test singleCorrectTest = QuestionFixtures
			.test(AnswerCardinality.SINGLE_CORRECT, TestStatus.CREATED);

	@Test
	void flaggedSingleCorrectQuestionIsKeptForReview() {
		// Regression: previously a flag turned the distractor into a correct answer and
		// the question failed the single-correct cardinality check (silently dropped).
		TestQuestionVersion v = QuestionFixtures.defaultVersion();
		v.getOption(OptionLetter.B).flag("ArrayDeque", "HashMap could be argued");
		TestQuestion q = QuestionFixtures.draft(v);

		List<TestQuestion> valid = validator.validateAndNormalize(List.of(q), singleCorrectTest, List.of(11L, 12L), 3,
				TestQuestionValidator.postLlmStrategies());

		assertThat(valid).hasSize(1);
		assertThat(valid.get(0).getQuestionIndex()).isEqualTo(3);
		assertThat(valid.get(0).getCorrectOptions()).hasSize(1);
		assertThat(valid.get(0).getFlaggedOptions()).hasSize(1);
	}

	@Test
	void shuffleMovesAnswerKeyAndFlagWithTheirText() {
		TestQuestionVersion v = QuestionFixtures.defaultVersion();
		v.getOption(OptionLetter.B).flag("ArrayDeque", "r");
		TestQuestion q = QuestionFixtures.draft(v);

		for (int i = 0; i < 20; i++) {
			validator.validateAndNormalize(List.of(q), singleCorrectTest, List.of(), 0,
					TestQuestionValidator.postLlmStrategies());
			TestQuestionOption correct = q.getOption(q.getCorrectOptions().get(0));
			TestQuestionOption flagged = q.getOption(q.getFlaggedOptions().get(0));
			assertThat(correct.getText()).isEqualTo("LinkedHashMap");
			assertThat(flagged.getText()).isEqualTo("HashMap");
			assertThat(flagged.getSuggestedText()).isEqualTo("ArrayDeque");
		}
	}

	@Test
	void malformedQuestionsAreDiscardedByPreValidation() {
		TestQuestion tooShort = QuestionFixtures
				.draft(QuestionFixtures.version("Short?", Set.of(OptionLetter.A), "a1", "b1", "c1", "d1"));
		TestQuestion noCorrect = QuestionFixtures.draft(QuestionFixtures.version("Which option is valid here?",
				Set.of(), "alpha", "beta", "gamma", "delta"));
		TestQuestion duplicateOptions = QuestionFixtures.draft(QuestionFixtures
				.version("Which option is valid here?", Set.of(OptionLetter.A), "same", "Same", "other", "fourth"));
		TestQuestion allOfTheAbove = QuestionFixtures.draft(QuestionFixtures.version("Which option is right here?",
				Set.of(OptionLetter.A), "first", "second", "third", "All of the above"));
		TestQuestion good = QuestionFixtures.draft(QuestionFixtures.defaultVersion());

		List<TestQuestion> valid = validator.validateAndNormalize(
				List.of(tooShort, noCorrect, duplicateOptions, allOfTheAbove, good), singleCorrectTest, List.of(), 0,
				TestQuestionValidator.preLlmStrategies());

		assertThat(valid).containsExactly(good);
	}

	@Test
	void manualValidationReportsEveryReason() {
		TestQuestion manual = QuestionFixtures.draft(QuestionFixtures.version("Which two of these are maps?",
				Set.of(OptionLetter.A, OptionLetter.B), "HashMap", "TreeMap", "ArrayList", "LinkedList"));

		List<String> reasons = validator.validateManual(manual, singleCorrectTest, List.of());

		assertThat(reasons).anyMatch(r -> r.contains("count-revealing"));
		assertThat(reasons).anyMatch(r -> r.contains("instead of 1"));
	}

	@Test
	void manualValidationRejectsDuplicateOfAnotherQuestion() {
		TestQuestion existing = QuestionFixtures.persisted(1L, 0, 10L, QuestionFixtures.defaultVersion());
		TestQuestionVersion copy = QuestionFixtures.version("  Which collection keeps insertion order in Java? ",
				Set.of(OptionLetter.A), "LinkedHashMap", "HashMap", "TreeSet", "PriorityQueue");
		TestQuestion manual = QuestionFixtures.draft(copy);

		List<String> reasons = validator.validateManual(manual, singleCorrectTest, List.of(existing));

		assertThat(reasons).containsExactly("duplicates another question of the test");
	}

	@Test
	void validManualQuestionHasNoReasonsAndIsNormalized() {
		TestQuestion manual = QuestionFixtures.draft(QuestionFixtures.version("Q1:  Which   map sorts its keys?",
				Set.of(OptionLetter.C), "HashMap", "LinkedHashMap", "TreeMap", "IdentityHashMap"));

		List<String> reasons = validator.validateManual(manual, singleCorrectTest, List.of());

		assertThat(reasons).isEmpty();
		assertThat(manual.getQuestionText()).isEqualTo("Which map sorts its keys?");
	}

	@Test
	void multiCorrectQuotaDiscardsBatchWhenNotMet() {
		com.knowgauge.core.model.Test multiTest = QuestionFixtures.test(AnswerCardinality.MULTIPLE_CORRECT,
				TestStatus.CREATED);
		multiTest.setMinMultipleCorrectQuestionsCount(1);
		TestQuestion single = QuestionFixtures.draft(QuestionFixtures.defaultVersion());

		assertThat(validator.validateAndNormalize(List.of(single), multiTest, List.of(), 0,
				TestQuestionValidator.postLlmStrategies())).isEmpty();
	}
}
