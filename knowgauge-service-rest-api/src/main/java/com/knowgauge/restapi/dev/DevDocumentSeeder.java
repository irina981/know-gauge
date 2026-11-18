package com.knowgauge.restapi.dev;

import java.io.InputStream;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import com.knowgauge.core.context.ExecutionContext;
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

	/** The tenant of the seeded topic tree (R__dev_seed_topics.sql). */
	private static final long DEV_TENANT_ID = 1L;

	@Bean
	CommandLineRunner seedDocuments(DocumentService documentService, DocumentRepository documentRepository,
			StorageService storageService, ExecutionContext executionContext) {
		return args -> {

			try {
				storageService.ensureBucketExists();
			} catch(Exception e) {
				log.error("Ensuring bucker exists failed: {}", e.getMessage(), e);
			}

			// No request at startup: seed tenant 1 as the system user (also when sign-in is on).
			executionContext.runAsSystem(DEV_TENANT_ID, () -> {
				for (SeedDocument seed : SEED_DOCUMENTS) {
					try {
						seedIfMissing(documentService, documentRepository, seed.topicId(), seed.title(),
								seed.originalFileName(), "devdocs/" + seed.resourceFileName());
					} catch (Exception e) {
						log.error("Dev seed failed for: {}", seed.resourceFileName(), e);
					}
				}
			});
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
			// Programming › Java and › System Design: tutorial series provided as PDFs (no Markdown source), the
			// lessons directly in the topic. Listed in lesson order, which is also the order they are shown in.
			new SeedDocument(2000L, "Java Language Fundamentals", "Java - 01 Java Language Fundamentals.pdf"),
			new SeedDocument(2000L, "Modern Java: Streams, Records, Optionals",
					"Java - 02 Modern Java Streams Records Optionals.pdf"),
			new SeedDocument(2000L, "JVM Memory and Garbage Collection",
					"Java - 03 JVM Memory and Garbage Collection.pdf"),
			new SeedDocument(2000L, "Processes, Threads, and Java Concurrency",
					"Java - 04 Processes Threads and Java Concurrency.pdf"),
			new SeedDocument(2000L, "Spring Framework Fundamentals", "Java - 05 Spring Framework Fundamentals.pdf"),
			new SeedDocument(2000L, "Spring Boot Practical Guide", "Java - 06 Spring Boot Practical Guide.pdf"),
			new SeedDocument(2000L, "Quarkus Fundamentals and Architecture",
					"Java - 07 Quarkus Fundamentals and Architecture.pdf"),

			new SeedDocument(3000L, "System Design Fundamentals", "System Design - 01 System Design Fundamentals.pdf"),
			new SeedDocument(3000L, "Scalability and Performance", "System Design - 02 Scalability and Performance.pdf"),
			new SeedDocument(3000L, "Data Storage and Consistency",
					"System Design - 03 Data Storage and Consistency.pdf"),
			new SeedDocument(3000L, "Distributed Systems and Messaging",
					"System Design - 04 Distributed Systems and Messaging.pdf"),
			new SeedDocument(3000L, "Reliability, Security, and Observability",
					"System Design - 05 Reliability Security and Observability.pdf"),

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

			// AI in Day-to-Day Work: nine tutorials provided as PDFs (no Markdown source), three per sub-topic, in order
			new SeedDocument(11010L, "AI at Work: Capabilities and Limits", "AI at Work - 01 What AI Can and Cannot Do.pdf"),
			new SeedDocument(11010L, "Prompting: From Vague Requests to Useful Outputs",
					"AI at Work - 02 Prompting for Useful Results.pdf"),
			new SeedDocument(11010L, "AI Tools, Context, and Human Oversight",
					"AI at Work - 03 AI Tools and Workflow Selection.pdf"),
			new SeedDocument(11020L, "AI for Emails, Meetings, and Status Updates",
					"AI at Work - 04 Writing Email and Meeting Notes.pdf"),
			new SeedDocument(11020L, "AI for Research, Learning, and Comparing Options",
					"AI at Work - 05 Research Learning and Synthesis.pdf"),
			new SeedDocument(11020L, "AI for Coding, Debugging, and Code Reviews",
					"AI at Work - 06 AI for Developer Productivity.pdf"),
			new SeedDocument(11030L, "Privacy, Security, and Safe AI Usage",
					"AI at Work - 07 Data Privacy and AI Safety.pdf"),
			new SeedDocument(11030L, "Grounded AI, Search, and Retrieval-Augmented Generation",
					"AI at Work - 08 Grounding and RAG in Workflows.pdf"),
			new SeedDocument(11030L, "Reliable AI Automation and Quality Evaluation",
					"AI at Work - 09 Automation Evaluation and Human Review.pdf"),

			// English Lessons (sources in demo-content/english-lessons): same four sections per lesson
			new SeedDocument(12100L, "Lesson 1 – Travel and Holidays", "English Lessons - Lesson 1 Travel.pdf"),
			new SeedDocument(12200L, "Lesson 2 – Work and Careers", "English Lessons - Lesson 2 Work.pdf"),
			new SeedDocument(12300L, "Lesson 3 – Health and Lifestyle", "English Lessons - Lesson 3 Health.pdf"),
			new SeedDocument(12400L, "Lesson 4 – Technology and the Internet",
					"English Lessons - Lesson 4 Technology.pdf"));

	private void seedIfMissing(DocumentService documentService, DocumentRepository documentRepository, Long topicId,
			String title, String originalFileName, String classpathLocation) throws Exception {

		// ---------- idempotency check ----------
		boolean exists = documentRepository.existsByTenantIdAndTopicIdAndOriginalFileName(DEV_TENANT_ID, topicId,
				originalFileName);

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
