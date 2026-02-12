package com.knowgauge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Main Spring Boot application class for KnowGauge Service.
 * This is the entry point for the REST API module.
 */
@SpringBootApplication(scanBasePackages = "com.knowgauge")
@Import({
	  com.knowgauge.repo.jpa.config.MainJpaConfig.class,
	  com.knowgauge.pgvector.repo.jpa.config.PgVectorJpaConfig.class
	})
public class KnowGaugeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowGaugeApplication.class, args);
    }
}
