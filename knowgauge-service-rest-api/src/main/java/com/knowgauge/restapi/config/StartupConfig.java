package com.knowgauge.restapi.config;

import java.util.Arrays;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

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
}
