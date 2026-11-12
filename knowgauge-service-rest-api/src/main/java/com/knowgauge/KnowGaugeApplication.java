package com.knowgauge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Main Spring Boot application class for KnowGauge Service. This is the entry
 * point for the REST API module.
 */
@SpringBootApplication(scanBasePackages = "com.knowgauge")
@Import({ com.knowgauge.infra.repository.jpa.config.MainJpaConfig.class,
		com.knowgauge.infra.vectorstore.pgvector.jpa.config.PgVectorJpaConfig.class })
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class KnowGaugeApplication {

	public static void main(String[] args) {
		SpringApplication.run(KnowGaugeApplication.class, args);
	}
}
