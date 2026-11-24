package com.knowgauge.restapi.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Turns on {@code @RolesAllowed} (Jakarta, JSR-250) on controller methods in security mode {@code oidc}: each
 * workspace endpoint names the roles that may call it (ADR017), checked against the user's roles from
 * {@code user_roles} ({@code ROLE_*} authorities, {@link AccountResolutionFilter}). A refusal is a {@code 403}.
 * <p>
 * Off in mode {@code fixed}, where nobody signs in and the fixed user may do everything. Core checks the same rules
 * again ({@code RolePermissions}), plus what an annotation cannot see: a {@code TENANT_USER} changes only the tests
 * they created.
 */
@Configuration
@ConditionalOnProperty(name = "kg.security.mode", havingValue = "oidc")
@EnableMethodSecurity(jsr250Enabled = true)
public class MethodSecurityConfig {
}
