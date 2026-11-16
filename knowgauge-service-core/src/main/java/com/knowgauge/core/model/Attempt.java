package com.knowgauge.core.model;

import java.time.Instant;

import com.knowgauge.core.model.enums.AttemptStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Attempt extends AuditableObject {
	private Long tenantId;

	private Long testId;

	/** Set only when a logged-in administrator started the attempt (ADR009). */
	private Long userId;

	/** Full name the participant entered on the start screen. */
	private String participantName;

	/** Email the participant entered; identifies the same person across attempts. */
	private String participantEmail;

	/** Random token the participant uses to read and submit the attempt (ADR009). */
	private String publicToken;

	/** The personal invitation this attempt was started from, if any. */
	private Long invitationId;

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
