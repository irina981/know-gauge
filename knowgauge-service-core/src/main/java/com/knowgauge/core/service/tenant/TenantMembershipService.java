package com.knowgauge.core.service.tenant;

import com.knowgauge.core.model.enums.TenantType;

/** The invitee's side of a workspace invitation (ADR015). */
public interface TenantMembershipService {

	/** What the link leads to, for the join page before and after signing in (public: no ids). */
	record InvitationPreview(String tenantName, TenantType tenantType, String email, boolean accepted) {
	}

	/** What accepting did: the workspace joined, and the one the user left ({@code null} if none or the same). */
	record Joined(Long tenantId, String tenantName, Long previousTenantId) {
	}

	/** @throws com.knowgauge.core.exception.ResourceNotFoundException for an unknown or revoked link */
	InvitationPreview preview(String token);

	/**
	 * Moves the signed-in user into the invited workspace. Accepting again is a no-op.
	 *
	 * @param emailVerified whether the identity provider verified the user's email
	 * @throws com.knowgauge.core.exception.TenantInvitationForbiddenException when nobody is signed in, the email is
	 *                                                                           not verified or is not the invited one
	 * @throws com.knowgauge.core.exception.TenantInvitationConflictException when someone else accepted it or the
	 *                                                                          personal workspace has another owner
	 */
	Joined accept(String token, boolean emailVerified);
}
