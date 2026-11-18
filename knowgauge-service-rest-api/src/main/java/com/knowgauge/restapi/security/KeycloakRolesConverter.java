package com.knowgauge.restapi.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Keycloak realm roles ({@code realm_access.roles}) as {@code ROLE_<name>} authorities, e.g.
 * {@code APPLICATION_ADMIN} becomes {@code ROLE_APPLICATION_ADMIN}. Keycloak's own default roles are ignored.
 */
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
		if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
			return List.of();
		}
		return roles.stream().map(String::valueOf)
				.filter(role -> !role.startsWith("default-roles-") && !role.equals("offline_access")
						&& !role.equals("uma_authorization"))
				.<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
	}
}
