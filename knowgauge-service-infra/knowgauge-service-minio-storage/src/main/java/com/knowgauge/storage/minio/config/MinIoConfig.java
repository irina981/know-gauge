package com.knowgauge.storage.minio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
public class MinIoConfig {

    @Bean
    MinioClient minioClient(
            @Value("${app.storage.endpoint}") String endpoint,
            @Value("${app.storage.accessKey}") String accessKey,
            @Value("${app.storage.secretKey}") String secretKey
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
