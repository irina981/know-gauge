package com.knowgauge.restapi.security;

import java.util.OptionalLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentUserProvider;

/** Security mode {@code oidc}: the signed-in user, or none for anonymous requests (public test links). */
@Component
@ConditionalOnProperty(name = "kg.security.mode", havingValue = "oidc")
public class TokenUserProvider implements CurrentUserProvider {

	@Override
	public OptionalLong userId() {
		return SignedInAccount.current().map(account -> OptionalLong.of(account.user().getId()))
				.orElse(OptionalLong.empty());
	}
}
