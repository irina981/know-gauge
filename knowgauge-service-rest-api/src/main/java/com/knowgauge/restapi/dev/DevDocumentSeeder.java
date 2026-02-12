package com.knowgauge.restapi.dev;

import java.io.InputStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.port.repository.DocumentRepository;
import com.knowgauge.core.port.storage.StorageService;
import com.knowgauge.core.service.content.DocumentService;
import com.knowgauge.core.util.HashingHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "kg.dev.seed.enabled", havingValue = "true")
@Profile({ "dev", "docker" })
public class DevDocumentSeeder {

	@Bean
	CommandLineRunner seedDocuments(DocumentService documentService, DocumentRepository documentRepository, StorageService storageService) {
		return args -> {
			
			try {
				storageService.ensureBucketExists();
			} catch(Exception e) {
				log.error("Ensuring bucker exists failed: {}", e.getMessage(), e);
			}
			
			seedIfMissing(documentService, documentRepository, 2100L, "Java Notes", "Java - Notes.pdf",
					"devdocs/Java - Notes.pdf");

			seedIfMissing(documentService, documentRepository, 4000L, "Spring - DI Types", "Spring - DI Types",
					"devdocs/Spring - DI Types.pdf");

			seedIfMissing(documentService, documentRepository, 4000L, "Spring - Bean Configuration Types",
					"Spring - Bean Configuration Types.pdf", "devdocs/Spring - Bean Configuration Types.pdf");

			seedIfMissing(documentService, documentRepository, 2200L, "Java Multithreading - Process vs Thread",
					"Java Multithreading - Process vs Thread.pdf",
					"devdocs/Java Multithreading - Process vs Thread.pdf");
		};
	}

	private void seedIfMissing(DocumentService documentService, DocumentRepository documentRepository, Long topicId,
			String title, String originalFileName, String classpathLocation) throws Exception {

		// ---------- idempotency check ----------
		boolean exists = documentRepository.existsByTopicIdAndOriginalFileName(topicId, originalFileName);

		if (exists) {
			log.info("Dev seed skipped (already exists): [{}] {}", topicId, title);
			return;
		}

		ClassPathResource resource = new ClassPathResource(classpathLocation);
		if (!resource.exists()) {
			log.warn("Dev seed file not found: {}", classpathLocation);
			return;
		}

		Document doc = new Document();
		doc.setTopicId(topicId);
		doc.setTitle(title);
		doc.setOriginalFileName(originalFileName);
		doc.setContentType("application/pdf");
		doc.setFileSizeBytes(resource.contentLength());

		try {
			String checksum = HashingHelper.sha256Hex(resource.getInputStream());
			doc.setChecksum(checksum);

			try (InputStream in = resource.getInputStream()) {
				documentService.uploadDocument(doc, in);
				log.info("Dev seed uploaded: [{}] {}", topicId, title);
			}
		} catch (Exception e) {
			log.error("Dev seed file upload failed for: {}", classpathLocation, e);
		}

	}
}
