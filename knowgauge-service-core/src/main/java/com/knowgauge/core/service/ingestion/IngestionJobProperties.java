package com.knowgauge.core.service.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings of the scheduled ingestion job ({@code kg.ingestion.job.*}).
 * {@code cron} is read directly by {@link ScheduledIngestionJob}'s
 * {@code @Scheduled} annotation; it is bound here only for logging.
 */
@Component
@ConfigurationProperties(prefix = "kg.ingestion.job")
public class IngestionJobProperties {

	/** Whether the job bean is created at all. */
	private boolean enabled;

	/** Spring cron expression (six fields: second minute hour day month weekday). */
	private String cron;

	/** Maximum number of UPLOADED documents ingested per run, oldest first. */
	private int batchSize = 5;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getCron() {
		return cron;
	}

	public void setCron(String cron) {
		this.cron = cron;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}
}
