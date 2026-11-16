package com.knowgauge.core.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A personal invitation to take a published test (ADR009): one email, one
 * random link, usable for one attempt. Sent by the server; a failed send is
 * kept with its error so it can be resent.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestInvitation extends AuditableObject {
	private Long tenantId;

	private Long testId;

	/** Lower-cased email address the invitation was sent to. */
	private String email;

	/** Random token in the personal link. */
	private String publicToken;

	/** Last successful send; {@code null} if never sent successfully. */
	private Instant sentAt;

	/** How often a send was attempted. */
	@Builder.Default
	private Integer sendCount = 0;

	/** Error of the last send, {@code null} after a successful one. */
	private String sendError;
}
