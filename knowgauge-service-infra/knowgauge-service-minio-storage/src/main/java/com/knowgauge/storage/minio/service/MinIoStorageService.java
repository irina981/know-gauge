package com.knowgauge.storage.minio.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.knowgauge.core.exception.StorageAuthException;
import com.knowgauge.core.exception.StorageBadRequestException;
import com.knowgauge.core.exception.StorageNotFoundException;
import com.knowgauge.core.exception.StorageUnavailableException;
import com.knowgauge.core.exception.StorageUnexpectedException;
import com.knowgauge.core.port.storage.StorageService;
import com.knowgauge.storage.minio.mapper.MinioResponseMapper;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;

@Service
public class MinIoStorageService implements StorageService {

	private final MinioClient minioClient;
	private final String bucket;
	private final MinioResponseMapper responseMapper;

	public MinIoStorageService(MinioClient minioClient, MinioResponseMapper responseMapper,
			@Value("${app.storage.bucket}") String bucket) {
		this.minioClient = minioClient;
		this.responseMapper = responseMapper;
		this.bucket = bucket;
	}

	@Override
	@Retry(name = "minio") // TODO: retry with InputStream is not safe - implement correctly retries!
	@CircuitBreaker(name = "minio", fallbackMethod = "putFallback")
	@Bulkhead(name = "minio", type = Bulkhead.Type.SEMAPHORE)
	public StoredObject put(String objectKey, InputStream in, long size, String contentType) {
		try {
			ObjectWriteResponse response = minioClient.putObject(PutObjectArgs.builder().bucket(bucket)
					.object(objectKey).stream(in, size, -1).contentType(contentType).build());
			return responseMapper.toStoredObject(response, objectKey, size, contentType);
		} catch (Exception e) {
			throw translate(e);
		}
	}

	private StoredObject putFallback(String objectKey, InputStream in, long size, String contentType, Throwable t) {
		// Option A: map to a clean 503 + message
		// throw new StorageUnavailableException("MinIO is temporarily unavailable", t);
		throw new StorageUnavailableException("MinIO is temporarily unavailable", t);
	}

	private RuntimeException translate(Exception e) {

		if (e instanceof ErrorResponseException err) {

			int status = err.response().code();

			if (status >= 500) {
				return new StorageUnavailableException(e); // retry
			}

			if (status == 404) {
				return new StorageNotFoundException(e); // no retry
			}

			if (status == 401 || status == 403) {
				return new StorageAuthException(e); // no retry
			}

			return new StorageBadRequestException(e); // no retry
		}

		if (e instanceof IOException) {
			return new StorageUnavailableException(e); // retry
		}

		return new StorageUnexpectedException(e);
	}
}
