package com.knowgauge.core.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.QuestionStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A question slot in a test (see ADR001).
 *
 * <p>
 * The slot is stable (id, position, status). Its content lives in the
 * {@link #currentVersion}. The content accessors below ({@code getQuestionText},
 * {@code getOptions}, …) delegate to the current version, so the generation
 * pipeline can treat a draft question as one object. Setters create a draft
 * version on demand; they must only be used on drafts, never on a persisted
 * version.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestQuestion extends AuditableObject {

	private Long tenantId;

	private Long testId;

	private Integer questionIndex;

	private QuestionStatus status;

	private Long currentVersionId;

	private TestQuestionVersion currentVersion;

	/**
	 * Status derived from the current version: NEEDS_REVIEW when any option is
	 * flagged, OK otherwise.
	 */
	public QuestionStatus deriveStatus() {
		return currentVersion != null && currentVersion.hasFlags() ? QuestionStatus.NEEDS_REVIEW : QuestionStatus.OK;
	}

	public boolean needsReview() {
		return deriveStatus() == QuestionStatus.NEEDS_REVIEW;
	}

	// ------------------------------------------------------------------
	// Content delegation to the current version
	// ------------------------------------------------------------------

	private TestQuestionVersion draft() {
		if (currentVersion == null) {
			currentVersion = new TestQuestionVersion();
			currentVersion.setTenantId(tenantId);
		}
		return currentVersion;
	}

	public String getQuestionText() {
		return currentVersion != null ? currentVersion.getQuestionText() : null;
	}

	public void setQuestionText(String questionText) {
		draft().setQuestionText(questionText);
	}

	public String getExplanation() {
		return currentVersion != null ? currentVersion.getExplanation() : null;
	}

	public void setExplanation(String explanation) {
		draft().setExplanation(explanation);
	}

	public List<Long> getSourceChunkIds() {
		return currentVersion != null ? currentVersion.getSourceChunkIds() : null;
	}

	public void setSourceChunkIds(List<Long> sourceChunkIds) {
		draft().setSourceChunkIds(sourceChunkIds);
	}

	public Map<OptionLetter, TestQuestionOption> getOptions() {
		return currentVersion != null ? currentVersion.getOptions() : Map.of();
	}

	public void setOptions(EnumMap<OptionLetter, TestQuestionOption> options) {
		draft().setOptions(options);
	}

	public void addOption(TestQuestionOption option) {
		draft().addOption(option);
	}

	public TestQuestionOption getOption(OptionLetter letter) {
		return currentVersion != null ? currentVersion.getOption(letter) : null;
	}

	public List<OptionLetter> getCorrectOptions() {
		return currentVersion != null ? currentVersion.getCorrectOptions() : List.of();
	}

	public void setCorrectOptions(List<OptionLetter> correctLetters) {
		draft().setCorrectOptions(correctLetters);
	}

	public List<OptionLetter> getFlaggedOptions() {
		return currentVersion != null ? currentVersion.getFlaggedOptions() : List.of();
	}
}
