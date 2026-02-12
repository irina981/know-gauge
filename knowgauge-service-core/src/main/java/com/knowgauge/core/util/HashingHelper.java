package com.knowgauge.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashingHelper {
	private HashingHelper() {
	}

	public static String sha256Hex(File file) throws IOException {
		try (var in = new FileInputStream(file)) {
			return sha256Hex(in);
		}
	}

	public static String sha256Hex(InputStream in) throws IOException {
		try {
			var md = MessageDigest.getInstance("SHA-256");
			var dis = new DigestInputStream(in, md);
			dis.transferTo(OutputStream.nullOutputStream());
			byte[] digest = md.digest();
			return toHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static String sha256Hex(String text) {
	    try {
	        MessageDigest md = MessageDigest.getInstance("SHA-256");
	        byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
	        return HexFormat.of().formatHex(digest);
	    } catch (NoSuchAlgorithmException e) {
	        throw new IllegalStateException(e);
	    }
	}

	private static String toHex(byte[] bytes) {
		var sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

}
