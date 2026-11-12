package com.knowgauge.core.model;

import java.time.Instant;
import java.util.EnumSet;

import com.knowgauge.core.model.enums.OptionLetter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AttemptAnswer extends AuditableObject {
	private Long tenantId;

	private Long id;

	private Long attemptId;

	private Long questionId;

	/** Exact question version shown to the user (see ADR001). */
	private Long questionVersionId;

	private EnumSet<OptionLetter> chosenOptions;

	private Boolean correct;

	private Instant answeredAt;
}
