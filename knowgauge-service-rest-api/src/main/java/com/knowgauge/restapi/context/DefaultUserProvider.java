package com.knowgauge.restapi.context;

import java.util.OptionalLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentUserProvider;

/**
 * Fixed user from {@code kg.context.user-id} (env {@code KG_USER_ID} in prod),
 * or no user when it is empty. Stands in until authentication provides the user.
 */
@Component
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
