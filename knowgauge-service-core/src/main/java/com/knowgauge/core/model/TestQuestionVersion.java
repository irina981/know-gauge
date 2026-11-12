package com.knowgauge.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.VersionOrigin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Immutable content snapshot of a question (see ADR001).
 *
 * <p>
 * A version is mutable only while it is a draft (not yet persisted, {@code id == null}).
 * Once persisted it must never change. A change to a question is a new version.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestQuestionVersion extends PersistedObject {

	private Long tenantId;

	private Long questionId;

	private Integer versionNo;

	private VersionOrigin origin;

	private Long parentVersionId;

	/** LLM call that produced this content. Null for FIX_ACCEPTED and MANUAL. */
	private Long generationRunId;

	/** Verification call that checked this content (and raised any flags). */
	private Long verificationRunId;

	private String questionText;

	private EnumMap<OptionLetter, TestQuestionOption> options;

	private String explanation;

	private List<Long> sourceChunkIds;

	private Instant createdAt;

	/** Author. Null means system (LLM generation). */
	private Long createdBy;

	/**
	 * @return read-only view of the options. Use {@link #addOption} or
	 *         {@link #setOptions} to change a draft.
	 */
	public Map<OptionLetter, TestQuestionOption> getOptions() {
		if (options == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(options);
	}

	/**
	 * Replaces the options. Every key must equal its option's letter.
	 */
	public void setOptions(EnumMap<OptionLetter, TestQuestionOption> newOptions) {
		if (newOptions != null) {
			for (Map.Entry<OptionLetter, TestQuestionOption> entry : newOptions.entrySet()) {
				TestQuestionOption opt = entry.getValue();
				if (opt != null && !entry.getKey().equals(opt.getLetter())) {
					throw new IllegalArgumentException("Option letter mismatch: EnumMap key=" + entry.getKey()
							+ " but option.letter=" + opt.getLetter());
				}
			}
		}
		this.options = newOptions;
	}

	/**
	 * Adds an option keyed by its own letter.
	 */
	public void addOption(TestQuestionOption option) {
		if (option == null || option.getLetter() == null) {
			throw new IllegalArgumentException("Option and its letter cannot be null");
		}
		if (options == null) {
			options = new EnumMap<>(OptionLetter.class);
		}
		options.put(option.getLetter(), option);
	}

	public TestQuestionOption getOption(OptionLetter letter) {
		return options != null ? options.get(letter) : null;
	}

	/**
	 * @return letters whose option is part of the answer key, in A–D order.
	 */
	public List<OptionLetter> getCorrectOptions() {
		List<OptionLetter> correct = new ArrayList<>();
		if (options == null) {
			return correct;
		}
		for (Map.Entry<OptionLetter, TestQuestionOption> entry : options.entrySet()) {
			if (entry.getValue() != null && entry.getValue().isCorrect()) {
				correct.add(entry.getKey());
			}
		}
		return correct;
	}

	/**
	 * Sets the answer key on the existing options.
	 */
	public void setCorrectOptions(List<OptionLetter> correctLetters) {
		if (options == null) {
			return;
		}
		for (Map.Entry<OptionLetter, TestQuestionOption> entry : options.entrySet()) {
			if (entry.getValue() != null) {
				entry.getValue().setCorrect(correctLetters != null && correctLetters.contains(entry.getKey()));
			}
		}
	}

	/**
	 * @return letters of options flagged by the verifier, in A–D order.
	 */
	public List<OptionLetter> getFlaggedOptions() {
		List<OptionLetter> flagged = new ArrayList<>();
		if (options == null) {
			return flagged;
		}
		for (Map.Entry<OptionLetter, TestQuestionOption> entry : options.entrySet()) {
			if (entry.getValue() != null && entry.getValue().isFlagged()) {
				flagged.add(entry.getKey());
			}
		}
		return flagged;
	}

	public boolean hasFlags() {
		return !getFlaggedOptions().isEmpty();
	}

	/**
	 * @return true when there is at least one flag and every flagged option has a
	 *         usable suggestion (precondition for accepting the fix).
	 */
	public boolean isFixApplicable() {
		if (!hasFlags()) {
			return false;
		}
		return options.values().stream().filter(o -> o != null && o.isFlagged())
				.allMatch(TestQuestionOption::hasUsableSuggestion);
	}

	/**
	 * Creates the content of a FIX_ACCEPTED successor draft: flagged options take
	 * their suggested text and lose the flag. Everything else is copied.
	 */
	public TestQuestionVersion toFixedDraft() {
		TestQuestionVersion draft = copyContentAsDraft();
		EnumMap<OptionLetter, TestQuestionOption> fixed = new EnumMap<>(OptionLetter.class);
		if (options != null) {
			options.forEach((letter, option) -> {
				if (option != null) {
					fixed.put(letter, option.isFlagged() ? option.withSuggestionApplied() : option.copyWithoutFlag());
				}
			});
		}
		draft.setOptions(fixed);
		return draft;
	}

	/**
	 * Deep-copies the content (text, options including flags, explanation,
	 * sources) into a new unsaved draft without identity or lineage.
	 */
	public TestQuestionVersion copyContentAsDraft() {
		TestQuestionVersion draft = new TestQuestionVersion();
		draft.setTenantId(tenantId);
		draft.setQuestionText(questionText);
		draft.setExplanation(explanation);
		draft.setSourceChunkIds(sourceChunkIds != null ? new ArrayList<>(sourceChunkIds) : null);
		EnumMap<OptionLetter, TestQuestionOption> copy = new EnumMap<>(OptionLetter.class);
		if (options != null) {
			options.forEach((letter, option) -> {
				if (option != null) {
					copy.put(letter, option.copy());
				}
			});
		}
		draft.setOptions(copy);
		return draft;
	}
}
