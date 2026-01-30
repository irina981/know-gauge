package com.knowgauge.storage.minio.service;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.knowgauge.core.port.service.storage.StorageService;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

@Service
public class MinIoStorageService implements StorageService {

	private final MinioClient minioClient;
	private final String bucket;

	public MinIoStorageService(MinioClient minioClient, @Value("${app.storage.bucket}") String bucket) {
		this.minioClient = minioClient;
		this.bucket = bucket;
	}

	@Override
	public StoredObject put(String objectKey, InputStream in, long size, String contentType) {
		try {
			minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(objectKey).stream(in, size, -1)
					.contentType(contentType).build());
			return new StoredObject(objectKey, size, contentType);
		} catch (Exception e) {
			throw new RuntimeException("Failed to upload to MinIO. key=" + objectKey, e);
		}
	}
}
