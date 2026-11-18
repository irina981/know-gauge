package com.knowgauge.restapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Lets Swagger UI send a Keycloak access token ("Authorize") when sign-in is on (security mode oidc). */
@Configuration
public class OpenApiConfig {

	private static final String BEARER = "bearer-jwt";

	@Bean
	OpenAPI knowGaugeOpenApi() {
		return new OpenAPI()
				.components(new Components().addSecuritySchemes(BEARER,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER));
	}
}
