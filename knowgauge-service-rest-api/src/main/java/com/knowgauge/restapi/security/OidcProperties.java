package com.knowgauge.restapi.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code kg.security.oidc.*}: how bearer tokens from Keycloak are validated in security mode {@code oidc}
 * (ADR010).
 */
@Getter
@Setter
@ConfigurationProperties("kg.security.oidc")
public class OidcProperties {

	/** Must equal the token's {@code iss} claim, e.g. {@code https://auth.example.com/realms/knowgauge}. */
	private String issuerUri;

	/**
	 * Where the signing keys are fetched. Defaults to the issuer's certs endpoint. Set it when the app reaches
	 * Keycloak at another address than the browser does (e.g. inside Docker).
	 */
	private String jwkSetUri;

	/** Must be one of the token's {@code aud} values (the realm's audience mapper adds it). */
	private String audience = "knowgauge-api";

	/** Signed-in users are resolved again (email, name changes) after this many minutes. */
	private int accountCacheMinutes = 10;

	public String effectiveJwkSetUri() {
		if (StringUtils.hasText(jwkSetUri)) {
			return jwkSetUri;
		}
		return issuerUri.replaceAll("/+$", "") + "/protocol/openid-connect/certs";
	}
}
