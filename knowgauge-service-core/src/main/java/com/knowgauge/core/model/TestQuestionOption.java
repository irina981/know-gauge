package com.knowgauge.core.model;

import com.knowgauge.core.model.enums.OptionLetter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One answer option (A–D) of a question version.
 *
 * <p>
 * {@code correct} is the answer key and is never changed by verification. The
 * verifier only <em>flags</em> a distractor it believes is actually true and
 * proposes a clearly false {@code suggestedText}. See ADR001.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TestQuestionOption {

	private OptionLetter letter;

	private String text;

	private boolean correct;

	private boolean flagged;

	/** Verifier's clearly-false replacement text. Only set when flagged. */
	private String suggestedText;

	/** Verifier's explanation of why the option was flagged. */
	private String flagReason;

	/**
	 * Marks this option as flagged by the verifier.
	 */
	public void flag(String suggestedText, String reason) {
		this.flagged = true;
		this.suggestedText = suggestedText;
		this.flagReason = reason;
	}

	/**
	 * @return true when this option is flagged and has a non-blank suggestion.
	 */
	public boolean hasUsableSuggestion() {
		return flagged && suggestedText != null && !suggestedText.isBlank();
	}

	/**
	 * @return an unflagged copy carrying only letter, text and the answer key.
	 */
	public TestQuestionOption copyWithoutFlag() {
		return TestQuestionOption.builder().letter(letter).text(text).correct(correct).build();
	}

	/**
	 * @return a copy in which the suggested text replaces the current text, and the
	 *         flag is cleared. The answer key is unchanged.
	 */
	public TestQuestionOption withSuggestionApplied() {
		if (!hasUsableSuggestion()) {
			return copyWithoutFlag();
		}
		return TestQuestionOption.builder().letter(letter).text(suggestedText.trim()).correct(correct).build();
	}

	/**
	 * @return a deep copy (including flag data).
	 */
	public TestQuestionOption copy() {
		return toBuilder().build();
	}
}
