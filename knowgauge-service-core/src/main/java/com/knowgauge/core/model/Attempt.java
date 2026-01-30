package com.knowgauge.core.model;

import java.time.Instant;

import com.knowgauge.core.model.enums.AttemptStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attempt {

	private Long id;

	private Long testId;

	private String userId;

	private AttemptStatus status;

	private Integer totalQuestions;

	@Builder.Default
	private Integer correctCount = 0;

	@Builder.Default
	private Integer wrongCount = 0;

	@Builder.Default
	private Double scorePercent = 0.0;

	private Instant startedAt;

	private Instant submittedAt;

	private Instant scoredAt;

}
