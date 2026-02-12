package com.knowgauge.repo.jpa.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
public class MainFlywayConfig {

	@Bean(name = "flyway", initMethod = "migrate")
	public Flyway flyway(@Qualifier("dataSource") DataSource dataSource, FlywayProperties props) {

		return Flyway.configure().dataSource(dataSource).locations(props.getLocations().toArray(String[]::new))
				.schemas(props.getSchemas().toArray(String[]::new)).baselineOnMigrate(props.isBaselineOnMigrate())
				.load();
	}
}
