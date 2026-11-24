package com.knowgauge.core.model;

import java.time.Instant;

import com.knowgauge.core.model.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * An invitation to join a workspace (tenant) an ADMIN created (ADR015): one email, one random link. Accepting moves
 * the user into the workspace ({@code users.tenant_id}) with {@link #role}. A failed send is kept with its error so it
 * can be resent.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WorkspaceInvitation extends AuditableObject {

	private Long tenantId;

	/** Lower-cased email address the invitation was sent to. */
	private String email;

	/** The tenant role the user gets on accepting. */
	private Role role;

	/** Random token in the link. */
	private String publicToken;

	/** Last successful send; {@code null} if never sent successfully. */
	private Instant sentAt;

	/** How often a send was attempted. */
	@Builder.Default
	private Integer sendCount = 0;

	/** Error of the last send, {@code null} after a successful one. */
	private String sendError;

	/** When the invitation was accepted; {@code null} while pending. */
	private Instant acceptedAt;

	/** The user who accepted it. */
	private Long acceptedUserId;

	public boolean isAccepted() {
		return acceptedAt != null;
	}
}
