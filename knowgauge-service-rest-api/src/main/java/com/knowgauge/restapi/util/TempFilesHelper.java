package com.knowgauge.restapi.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public final class TempFilesHelper {
	private TempFilesHelper() {
	}

	public static File toTempFile(MultipartFile file)
			throws IOException {
		File tempFile = File.createTempFile("kg-upload-", ".bin");
		try (var in = file.getInputStream(); var out = new FileOutputStream(tempFile)) {
			in.transferTo(out);
		}
		return tempFile;
	}
	
	public static void delete(File tempFile)
			throws IOException {
		tempFile.delete();
	}
}
