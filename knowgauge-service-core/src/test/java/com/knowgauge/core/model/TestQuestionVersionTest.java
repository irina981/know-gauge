package com.knowgauge.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.QuestionStatus;
import com.knowgauge.core.support.QuestionFixtures;

class TestQuestionVersionTest {

	@Test
	void correctOptionsComeFromAnswerKeyAndIgnoreFlags() {
		TestQuestionVersion v = QuestionFixtures.defaultVersion();
		v.getOption(OptionLetter.B).flag("ArrayDeque", "HashMap order is unspecified but stable");

		assertThat(v.getCorrectOptions()).containsExactly(OptionLetter.A);
		assertThat(v.getFlaggedOptions()).containsExactly(OptionLetter.B);
		assertThat(v.hasFlags()).isTrue();
	}

	@Test
	void fixedDraftReplacesFlaggedTextClearsFlagsAndKeepsAnswerKey() {
		TestQuestionVersion v = QuestionFixtures.defaultVersion();
		v.setId(5L);
		v.setGenerationRunId(9L);
		v.getOption(OptionLetter.B).flag("  ArrayDeque  ", "reason");

		TestQuestionVersion fixed = v.toFixedDraft();

		assertThat(fixed.getId()).isNull();
		assertThat(fixed.getOption(OptionLetter.B).getText()).isEqualTo("ArrayDeque");
		assertThat(fixed.getOption(OptionLetter.B).isFlagged()).isFalse();
		assertThat(fixed.getOption(OptionLetter.B).getSuggestedText()).isNull();
		assertThat(fixed.getCorrectOptions()).containsExactly(OptionLetter.A);
		assertThat(fixed.getOption(OptionLetter.A).getText()).isEqualTo("LinkedHashMap");
		assertThat(fixed.hasFlags()).isFalse();
		// the original stays untouched (immutability of persisted versions)
		assertThat(v.getOption(OptionLetter.B).getText()).isEqualTo("HashMap");
		assertThat(v.getOption(OptionLetter.B).isFlagged()).isTrue();
	}

	@Test
	void fixIsApplicableOnlyWhenEveryFlagHasASuggestion() {
		TestQuestionVersion v = QuestionFixtures.defaultVersion();
		assertThat(v.isFixApplicable()).isFalse();

		v.getOption(OptionLetter.B).flag("ArrayDeque", "r");
		assertThat(v.isFixApplicable()).isTrue();

		v.getOption(OptionLetter.C).flag("  ", "r");
		assertThat(v.isFixApplicable()).isFalse();
	}

	@Test
	void questionStatusIsDerivedFromCurrentVersionFlags() {
		TestQuestionVersion v = QuestionFixtures.defaultVersion();
		TestQuestion q = QuestionFixtures.draft(v);
		assertThat(q.deriveStatus()).isEqualTo(QuestionStatus.OK);

		v.getOption(OptionLetter.D).flag("Stack", "r");
		assertThat(q.deriveStatus()).isEqualTo(QuestionStatus.NEEDS_REVIEW);
	}

	@Test
	void optionsViewIsReadOnlyAndKeysMustMatchLetters() {
		TestQuestionVersion v = QuestionFixtures.defaultVersion();
		assertThatThrownBy(() -> v.getOptions().clear()).isInstanceOf(UnsupportedOperationException.class);

		EnumMap<OptionLetter, TestQuestionOption> wrong = new EnumMap<>(OptionLetter.class);
		wrong.put(OptionLetter.A, TestQuestionOption.builder().letter(OptionLetter.B).text("x").build());
		assertThatThrownBy(() -> v.setOptions(wrong)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void setCorrectOptionsUpdatesAnswerKey() {
		TestQuestionVersion v = QuestionFixtures.defaultVersion();
		v.setCorrectOptions(List.of(OptionLetter.B, OptionLetter.C));
		assertThat(v.getCorrectOptions()).containsExactly(OptionLetter.B, OptionLetter.C);
	}
}
