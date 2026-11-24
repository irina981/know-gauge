package com.knowgauge.restapi.security;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.service.account.AccountService;

import lombok.extern.slf4j.Slf4j;

/**
 * Security modes ({@code kg.security.mode}, ADR010):
 * <ul>
 * <li>{@code fixed} (default; also the test profile): no sign-in, every request is allowed, and the tenant and
 * user are fixed by {@code kg.context.*}.</li>
 * <li>{@code oidc}: Keycloak bearer tokens. {@code /api/public/**} (participants), health and the landing page
 * stay open; {@code /api/admin/**} and the other actuator endpoints need the {@code ADMIN} role from
 * {@code user_roles} (ADR012); everything else needs a signed-in user, who works in their own tenant.</li>
 * </ul>
 * Stateless in both modes: no session, no CSRF (the API takes bearer tokens, not cookies).
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(OidcProperties.class)
public class SecurityConfig {

	@Bean
	@ConditionalOnProperty(name = "kg.security.mode", havingValue = "fixed", matchIfMissing = true)
	SecurityFilterChain fixedSecurityFilterChain(HttpSecurity http) throws Exception {
		log.warn("Security mode fixed: no sign-in, every request runs as the fixed tenant (kg.context.tenant-id)");
		return stateless(http).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
	}

	@Bean
	@ConditionalOnProperty(name = "kg.security.mode", havingValue = "oidc")
	SecurityFilterChain oidcSecurityFilterChain(HttpSecurity http, AccountService accountService,
			OidcProperties properties) throws Exception {
		// Keycloak only authenticates (ADR012): no authorities from the token; AccountResolutionFilter adds the user's
		// roles from user_roles.
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of());
		AccountResolver accountResolver = new AccountResolver(accountService,
				Duration.ofMinutes(properties.getAccountCacheMinutes()));
		AccountResolutionFilter accountFilter = new AccountResolutionFilter(accountResolver, accountService);

		return stateless(http).authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/public/**").permitAll()
				.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
				.requestMatchers("/", "/index.html", "/*.svg", "/*.png", "/*.ico", "/error").permitAll()
				// Off in prod (springdoc.*.enabled); locally Swagger UI takes a token through "Authorize".
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.requestMatchers("/api/admin/**", "/actuator/**").hasRole(Role.ADMIN.name())
				.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
				.addFilterAfter(accountFilter, BearerTokenAuthenticationFilter.class).build();
	}

	@Bean
	@ConditionalOnProperty(name = "kg.security.mode", havingValue = "oidc")
	JwtDecoder jwtDecoder(OidcProperties properties) {
		if (!StringUtils.hasText(properties.getIssuerUri())) {
			throw new IllegalStateException(
					"kg.security.mode=oidc needs kg.security.oidc.issuer-uri (KG_OIDC_ISSUER_URI)");
		}
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.effectiveJwkSetUri()).build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(properties.getIssuerUri()),
				audienceValidator(properties.getAudience())));
		log.info("Security mode oidc: issuer {}, keys from {}, audience {}", properties.getIssuerUri(),
				properties.effectiveJwkSetUri(), properties.getAudience());
		return decoder;
	}

	private static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
		OAuth2Error error = new OAuth2Error("invalid_token", "The token is not meant for " + audience, null);
		return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(error);
	}

	private static HttpSecurity stateless(HttpSecurity http) throws Exception {
		return http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
	}
}
