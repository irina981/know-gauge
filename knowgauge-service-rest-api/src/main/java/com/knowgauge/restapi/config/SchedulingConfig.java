package com.knowgauge.restapi.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled} processing when the scheduled ingestion job is enabled.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "kg.ingestion.job.enabled", havingValue = "true")
public class SchedulingConfig {
}
