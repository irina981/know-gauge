package com.knowgauge.core.service.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * The system values of the tenant limits ({@link TenantLimits}). An {@code ADMIN} may override them on the System
 * settings page, and per tenant on the Workspaces page (ADR014). {@code null} or 0 means no limit.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kg.settings.tenant")
public class TenantLimitProperties {

	/** LLM tokens (input + output) each tenant may use per calendar month (UTC). */
	private Long monthlyTokenLimit;

	/** Megabytes of uploaded documents each tenant may keep. The library has no limit. */
	private Long storageLimitMb;

	/** Tests one user may generate at the same time. */
	private Integer concurrentGenerationsPerUser;
}
