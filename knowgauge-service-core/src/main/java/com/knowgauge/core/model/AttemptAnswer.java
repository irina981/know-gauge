package com.knowgauge.core.model;

import java.util.List;

import com.knowgauge.core.model.enums.OptionLetter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The answer given to one question in an attempt, scored when the attempt is
 * submitted.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AttemptAnswer extends AuditableObject {
	private Long tenantId;

	private Long attemptId;

	private Long questionId;

	/** Exact question version shown to the user (see ADR001). */
	private Long questionVersionId;

	/** Options the user selected; empty when the question was left unanswered. */
	private List<OptionLetter> chosenOptions;

	/** Whether the chosen options exactly match the answer key. */
	private Boolean correct;
}
