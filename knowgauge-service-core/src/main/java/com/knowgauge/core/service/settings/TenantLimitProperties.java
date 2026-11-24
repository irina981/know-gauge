package com.knowgauge.core.service.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/** Limits every tenant shares. An {@code ADMIN} may override them on the System settings page. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kg.settings.tenant")
public class TenantLimitProperties {

	/** LLM tokens (input + output) each tenant may use per calendar month (UTC); {@code null} for no limit. */
	private Long monthlyTokenLimit;
}
