package com.knowgauge.restapi.security;

import java.util.Optional;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.knowgauge.core.model.User;

/**
 * The local user behind the bearer token of the current request, stored as a request attribute by
 * {@link AccountResolutionFilter}.
 */
public record SignedInAccount(User user, boolean applicationAdmin) {

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
