package com.knowgauge.core.model;

import java.time.Instant;
import java.util.List;

import com.knowgauge.core.model.enums.AnswerOption;

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

	private List<AnswerOption> chosenOptions;

	private Boolean correct;

	private Instant answeredAt;
}
