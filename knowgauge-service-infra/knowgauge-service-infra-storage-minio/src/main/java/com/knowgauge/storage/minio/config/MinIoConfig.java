package com.knowgauge.storage.minio.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;
import okhttp3.OkHttpClient;

@Configuration
public class MinIoConfig {

	@Bean
	MinioClient minioClient(@Value("${kg.storage.endpoint}") String endpoint,
			@Value("${kg.storage.accessKey}") String accessKey, @Value("${kg.storage.secretKey}") String secretKey,
			@Value("${kg.storage.connectTimeout}") Long connectTimeout,
			@Value("${kg.storage.readTimeout}") Long readTimeout,
			@Value("${kg.storage.writeTimeout}") Long writeTimeout,
			@Value("${kg.storage.callTimeout}") Long callTimeout) {
		// Define custom timeouts on HTTP client level
		OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(connectTimeout))
				.readTimeout(Duration.ofSeconds(readTimeout)).writeTimeout(Duration.ofSeconds(writeTimeout))
				.callTimeout(Duration.ofSeconds(callTimeout)) // total per call
				.build();

		return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey)
				.httpClient(httpClient).build();
	}
}
