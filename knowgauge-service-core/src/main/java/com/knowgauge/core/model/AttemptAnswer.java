package com.knowgauge.core.model;

import java.time.Instant;

import com.knowgauge.core.model.enums.AnswerOption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptAnswer {

	private Long id;

	private Long attemptId;

	private Long questionId;

	private AnswerOption chosenOption;

	private Boolean correct;

	private Instant answeredAt;
}
