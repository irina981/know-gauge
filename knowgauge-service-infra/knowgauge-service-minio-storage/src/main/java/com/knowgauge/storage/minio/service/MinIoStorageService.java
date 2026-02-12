package com.knowgauge.storage.minio.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
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

	@Override
	@Retry(name = "minio") // TODO: retry with InputStream is not safe - implement correctly retries!
	@CircuitBreaker(name = "minio", fallbackMethod = "downloadToStreamFallback")
	@Bulkhead(name = "minio", type = Bulkhead.Type.SEMAPHORE)
	public void download(String objectKey, OutputStream out) {

		try (InputStream in = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {

			// Efficient streaming copy
			in.transferTo(out);

			// IMPORTANT: flush but DO NOT close OutputStream (caller owns it)
			out.flush();

		} catch (Exception e) {
			throw translate(e);
		}
	}

	@Override
	@Retry(name = "minio") // ⚠ retries with InputStream still need special handling (see note below)
	@CircuitBreaker(name = "minio", fallbackMethod = "downloadFallback")
	@Bulkhead(name = "minio", type = Bulkhead.Type.SEMAPHORE)
	public InputStream download(String objectKey) {

		try {
			return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());

			// DO NOT close here → caller must close InputStream

		} catch (Exception e) {
			throw translate(e);
		}
	}

	@Override
	@Retry(name = "minio") // TODO: retry with InputStream is not safe - implement correctly retries!
	@CircuitBreaker(name = "minio", fallbackMethod = "deleteFallback")
	@Bulkhead(name = "minio", type = Bulkhead.Type.SEMAPHORE)
	public void delete(String objectKey) {
		try {
			minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
		} catch (Exception e) {
			throw translate(e);
		}
	}

	@Override
	public void ensureBucketExists() {
		try {
			boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
			if (!exists) {
				minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
			}
		} catch (Exception e) {
			throw new StorageUnavailableException("Cannot ensure bucket exists: " + bucket, e);
		}
	}

	private StoredObject putFallback(String objectKey, InputStream in, long size, String contentType, Throwable t) {
		throw new StorageUnavailableException("MinIO PUT failed for key=" + objectKey, t);
	}

	private StoredObject deleteFallback(String objectKey, Throwable t) {
		throw new StorageUnavailableException("MinIO DELETE failed for key=" + objectKey, t);
	}
	
	private StoredObject downloadFallback(String objectKey, Throwable t) {
		throw new StorageUnavailableException("MinIO GET failed for key=" + objectKey, t);
	}
	
	private StoredObject downloadToStreamFallback(String objectKey, OutputStream out, Throwable t) {
		throw new StorageUnavailableException("MinIO GET failed for key=" + objectKey, t);
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
