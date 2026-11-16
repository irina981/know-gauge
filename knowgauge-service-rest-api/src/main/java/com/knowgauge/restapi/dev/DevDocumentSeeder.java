package com.knowgauge.restapi.dev;

import java.io.InputStream;
import java.util.List;

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
@Profile("local")
public class DevDocumentSeeder {

	@Bean
	CommandLineRunner seedDocuments(DocumentService documentService, DocumentRepository documentRepository, StorageService storageService) {
		return args -> {
			
			try {
				storageService.ensureBucketExists();
			} catch(Exception e) {
				log.error("Ensuring bucker exists failed: {}", e.getMessage(), e);
			}
			
			for (SeedDocument seed : SEED_DOCUMENTS) {
				seedIfMissing(documentService, documentRepository, seed.topicId(), seed.title(),
						seed.originalFileName(), "devdocs/" + seed.resourceFileName());
			}
		};
	}

	/**
	 * A PDF from {@code resources/devdocs/} uploaded under a topic from {@code R__dev_seed_topics.sql}. Files are named
	 * {@code "<Area> - <title>.pdf"}. The file name is the idempotency key: when renaming a seeded document, also rename
	 * it in existing databases in {@code R__dev_seed_topics.sql}.
	 */
	private record SeedDocument(long topicId, String title, String originalFileName, String resourceFileName) {

		SeedDocument(long topicId, String title, String fileName) {
			this(topicId, title, fileName, fileName);
		}
	}

	private static final List<SeedDocument> SEED_DOCUMENTS = List.of(
			// Programming
			new SeedDocument(2100L, "Core Java Notes", "Java - Core Java Notes.pdf"),
			new SeedDocument(2200L, "Processes vs Threads", "Java - Processes vs Threads.pdf"),
			new SeedDocument(4000L, "Dependency Injection Types", "Spring - Dependency Injection Types.pdf"),
			new SeedDocument(4000L, "Bean Configuration Types", "Spring - Bean Configuration Types.pdf"),

			// System Design (sources in demo-content/system-design)
			new SeedDocument(3100L, "System Design Basics", "System Design - Basics.pdf"),
			new SeedDocument(3200L, "Advanced System Design Topics", "System Design - Advanced Topics.pdf"),
			new SeedDocument(3300L, "System Design Interview Walkthrough", "System Design - Interview Walkthrough.pdf"),

			// Employee Onboarding & Compliance (fictional company; sources in demo-content/employee-onboarding)
			new SeedDocument(7100L, "Code of Conduct", "Corvane - Code of Conduct.pdf"),
			new SeedDocument(7100L, "Gifts, Hospitality and Anti-Bribery Policy",
					"Corvane - Gifts Hospitality and Anti-Bribery Policy.pdf"),
			new SeedDocument(7200L, "Acceptable Use Policy", "Corvane - Acceptable Use Policy.pdf"),
			new SeedDocument(7200L, "Password and MFA Standard", "Corvane - Password and MFA Standard.pdf"),
			new SeedDocument(7200L, "Phishing and Social Engineering Awareness Guide",
					"Corvane - Phishing and Social Engineering Awareness Guide.pdf"),
			new SeedDocument(7200L, "Data Classification and Handling Standard",
					"Corvane - Data Classification and Handling Standard.pdf"),
			new SeedDocument(7300L, "Employee Privacy Handbook (GDPR Basics)",
					"Corvane - Employee Privacy Handbook.pdf"),
			new SeedDocument(7300L, "Personal Data Breach Response Procedure",
					"Corvane - Personal Data Breach Response Procedure.pdf"),
			new SeedDocument(7400L, "Anti-Harassment and Equal Opportunity Policy",
					"Corvane - Anti-Harassment and Equal Opportunity Policy.pdf"),
			new SeedDocument(7400L, "Speak-Up Policy", "Corvane - Speak-Up Policy.pdf"),
			new SeedDocument(7500L, "Office Safety and Fire Evacuation Procedure",
					"Corvane - Office Safety and Fire Evacuation Procedure.pdf"),
			new SeedDocument(7500L, "Ergonomics and Home Workstation Guide",
					"Corvane - Ergonomics and Home Workstation Guide.pdf"),
			new SeedDocument(7600L, "Remote and Hybrid Work Policy", "Corvane - Remote and Hybrid Work Policy.pdf"),
			new SeedDocument(7600L, "Travel and Expense Policy", "Corvane - Travel and Expense Policy.pdf"),
			new SeedDocument(7600L, "Leave and Time Off Policy", "Corvane - Leave and Time Off Policy.pdf"),

			// Office Guides (sources in demo-content/office-guides): same four subjects per office, for FOCUSED tests
			new SeedDocument(9100L, "Rotterdam Office Guide", "Corvane - Rotterdam Office Guide.pdf"),
			new SeedDocument(9200L, "Lisbon Office Guide", "Corvane - Lisbon Office Guide.pdf"),
			new SeedDocument(9300L, "Kraków Office Guide", "Corvane - Krakow Office Guide.pdf"),

			// Gradovi Srbije (Serbian, sources in demo-content/gradovi-srbije): same four sections per city
			new SeedDocument(10100L, "Beograd – vodič kroz grad", "Gradovi Srbije - Beograd.pdf"),
			new SeedDocument(10200L, "Novi Sad – vodič kroz grad", "Gradovi Srbije - Novi Sad.pdf"),
			new SeedDocument(10300L, "Niš – vodič kroz grad", "Gradovi Srbije - Nis.pdf"),
			new SeedDocument(10400L, "Kragujevac – vodič kroz grad", "Gradovi Srbije - Kragujevac.pdf"),

			// AI in Day-to-Day Work (sources in demo-content/ai-day-to-day): same four sections per guide
			new SeedDocument(11100L, "AI Coding Assistants", "AI at Work - Coding Assistants.pdf"),
			new SeedDocument(11200L, "AI for Writing and Translation", "AI at Work - Writing and Translation.pdf"),
			new SeedDocument(11300L, "AI Chatbots for Customer Service", "AI at Work - Chatbots.pdf"),
			new SeedDocument(11400L, "AI for Images, Audio and Video", "AI at Work - Images and Media.pdf"),

			// English Lessons (sources in demo-content/english-lessons): same four sections per lesson
			new SeedDocument(12100L, "Lesson 1 – Travel and Holidays", "English Lessons - Lesson 1 Travel.pdf"),
			new SeedDocument(12200L, "Lesson 2 – Work and Careers", "English Lessons - Lesson 2 Work.pdf"),
			new SeedDocument(12300L, "Lesson 3 – Health and Lifestyle", "English Lessons - Lesson 3 Health.pdf"),
			new SeedDocument(12400L, "Lesson 4 – Technology and the Internet",
					"English Lessons - Lesson 4 Technology.pdf"));

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
