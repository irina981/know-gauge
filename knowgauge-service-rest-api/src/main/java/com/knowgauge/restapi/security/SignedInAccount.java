package com.knowgauge.restapi.security;

import java.util.Optional;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.knowgauge.core.model.User;

/**
 * The local user behind the bearer token of the current request, stored as a request attribute by
 * {@link AccountResolutionFilter}. {@code viewedTenantId} is set when an {@code ADMIN} reads another user's
 * library ({@link AccountResolutionFilter#TENANT_HEADER}, ADR013); that tenant is then the current one, read-only.
 * {@code subject} and {@code emailVerified} come from the token.
 */
public record SignedInAccount(User user, boolean applicationAdmin, Long viewedTenantId, String subject,
		boolean emailVerified) {

	/** The tenant this request works in: the viewed tenant, else the user's own. */
	public long tenantId() {
		return viewedTenantId != null ? viewedTenantId : user.getTenantId();
	}

	static final String REQUEST_ATTRIBUTE = SignedInAccount.class.getName();

	/** @return the account of the current request, empty when the request is anonymous or not a web request */
	public static Optional<SignedInAccount> current() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (attributes == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(
				(SignedInAccount) attributes.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
	}
}
