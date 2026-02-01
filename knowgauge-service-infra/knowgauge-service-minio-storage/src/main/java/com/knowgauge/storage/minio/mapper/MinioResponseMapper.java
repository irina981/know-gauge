package com.knowgauge.storage.minio.mapper;

import org.springframework.stereotype.Component;

import com.knowgauge.core.port.storage.StorageService.StoredObject;

import io.minio.ObjectWriteResponse;

@Component
public class MinioResponseMapper {

	public StoredObject toStoredObject(ObjectWriteResponse response, String objectKey, long size,
			String contentType) {

		return new StoredObject(objectKey, response.etag(), response.versionId(), size, contentType);
	}
}