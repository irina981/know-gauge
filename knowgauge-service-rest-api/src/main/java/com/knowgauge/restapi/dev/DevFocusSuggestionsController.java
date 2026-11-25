package com.knowgauge.restapi.dev;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.DevFocusSuggestionsApi;

/**
 * Focus suggestions for the dev-seeded demo topics (R__dev_seed_topics.sql), shown under the Focus field of the
 * generate form. Hard-coded here because they belong to the demo content only; the endpoint exists only when the
 * dev seed is enabled, so other environments have no suggestions. Each text was checked with the embedding model to
 * select the matching sections (see demo-content/README.md).
 */
@RestController
@ConditionalOnProperty(name = "kg.dev.seed.enabled", havingValue = "true")
@Profile("local")
public class DevFocusSuggestionsController implements DevFocusSuggestionsApi {

	/** Topic id (from the dev seed) to its focus suggestions. */
	static final Map<Long, List<String>> SUGGESTIONS = Map.of(
			// Programming › Java: subjects a whole test is realistically about, each selecting 8-20 passages
			2000L, List.of("Threads, synchronization and concurrency in Java",
					"Memory management and garbage collection in the JVM",
					"Classes, objects, inheritance and interfaces in Java",
					"Lambdas, streams, records and Optional in modern Java", "Spring Framework and Spring Boot",
					"Dependency injection and beans in Spring", "Transactions and database access",
					"Web applications and REST endpoints",
					"Monitoring, health checks and observability of Java services", "Quarkus and cloud-native Java"),
			// Programming › System Design: the same, 11-22 passages each
			3000L, List.of("Scalability and performance", "Caching and load balancing",
					"Databases: storage, replication, sharding and consistency", "Data consistency and transactions",
					"Messaging and asynchronous communication between services", "Reliability and fault tolerance",
					"Security and observability of production systems", "Capacity estimation and requirements"),
			// Corvane Systems › Employee Onboarding & Compliance
			7000L, List.of("Passwords, passphrases and multi-factor authentication",
					"Recognising and reporting phishing emails",
					"Data classification levels: public, internal, confidential and restricted",
					"Gifts, hospitality and bribery limits", "Travel bookings and expense claims",
					"Annual leave, sick leave and parental leave"),
			// Corvane Systems › Office Guides
			9000L, List.of("Fire evacuation, alarms, assembly points and fire wardens",
					"Parking, electric car charging, bicycles and public transport",
					"Kitchen, coffee, fridge rules and team lunches",
					"Booking meeting rooms and registering visitors"),
			// Gradovi Srbije
			10000L, List.of("Hrana i piće, tradicionalna jela i rakija",
					"Festivali i manifestacije: koncerti, film i pozorište",
					"Istorija grada: osnivanje, vladari, ratovi i oslobođenje",
					"Znamenitosti za obilazak: tvrđave, spomenici, muzeji i parkovi"),
			// AI in Day-to-Day Work
			11000L, List.of("What AI can and cannot do: language models, tokens, hallucinations and verification",
					"Prompting: prompt components, few-shot examples and structured output",
					"Choosing AI tools and designing supervised AI workflows",
					"Meeting notes: decisions, action items, owners and status updates",
					"Code generation, debugging, unit tests and code review",
					"Sensitive data, privacy and prompt injection",
					"RAG and grounding: retrieval, chunking and evaluating retrieved evidence",
					"Evaluating AI quality: test datasets, metrics and monitoring"),
			// English Lessons (B1–B2)
			12000L, List.of("Common mistakes and their corrections", "Useful phrases for everyday situations",
					"Present perfect and past simple",
					"Modal verbs: must, have to, should, mustn't and don't have to",
					"First and second conditional with if", "The passive voice"));

	@Override
	public ResponseEntity<Map<Long, List<String>>> getFocusSuggestions() {
		return ResponseEntity.ok(SUGGESTIONS);
	}
}
