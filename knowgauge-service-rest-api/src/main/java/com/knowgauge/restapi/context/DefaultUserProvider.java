package com.knowgauge.restapi.context;

import java.util.OptionalLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentUserProvider;

/**
 * Security mode {@code fixed} (no sign-in): the user from {@code kg.context.user-id} (env {@code KG_USER_ID} in
 * prod), or no user when it is empty.
 */
@Component
@ConditionalOnProperty(name = "kg.security.mode", havingValue = "fixed", matchIfMissing = true)
public class DefaultUserProvider implements CurrentUserProvider {

	private final OptionalLong userId;

	public DefaultUserProvider(@Value("${kg.context.user-id:}") String userId) {
		this.userId = userId == null || userId.isBlank() ? OptionalLong.empty()
				: OptionalLong.of(Long.parseLong(userId.trim()));
	}

	@Override
	public OptionalLong userId() {
		return userId;
	}
}
