package com.knowgauge.restapi.config;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.knowgauge.core.context.ExecutionContext;

/**
 * Fills the audit columns of both databases: created/updated at, and created/updated by the user of the
 * {@link ExecutionContext} (the signed-in user, the system user for jobs, or {@code null} for anonymous
 * participants).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "auditingDateTimeProvider")
public class AuditingConfig {

	@Bean
	AuditorAware<Long> auditorAware(ExecutionContext executionContext) {
		return () -> {
			OptionalLong userId = executionContext.userId();
			return userId.isPresent() ? Optional.of(userId.getAsLong()) : Optional.empty();
		};
	}

	@Bean
	DateTimeProvider auditingDateTimeProvider() {
		return () -> Optional.of(Instant.now());
	}
}
