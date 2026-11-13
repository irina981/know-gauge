package com.knowgauge.restapi.config;

import java.util.Arrays;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.knowgauge.core.port.storage.StorageService;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class StartupConfig {

	@Bean
	ApplicationRunner logActiveProfiles(Environment environment) {
		return args -> {
			String[] profiles = environment.getActiveProfiles();

			if (profiles.length == 0) {
				log.info("No active Spring profile set (using default)");
			} else {
				log.info("Active Spring profiles: " + Arrays.toString(profiles));
			}
		};
	}

	/**
	 * Creates the storage bucket at startup when it is missing (opt-in, e.g. deploy/docker-compose.yml on a fresh
	 * server). Fails startup if storage cannot be reached, rather than failing on the first upload. The local profile
	 * creates the bucket in DevDocumentSeeder instead.
	 */
	@Bean
	@ConditionalOnProperty(name = "kg.storage.create-bucket-on-startup", havingValue = "true")
	ApplicationRunner ensureStorageBucket(StorageService storageService) {
		return args -> {
			storageService.ensureBucketExists();
			log.info("Storage bucket is ready");
		};
	}
}
