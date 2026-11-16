package com.knowgauge.restapi.dev;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Focus suggestions for the dev-seeded demo topics (R__dev_seed_topics.sql), shown under the Focus field of the
 * generate form. Hard-coded here because they belong to the demo content only; the endpoint exists only when the
 * dev seed is enabled, so other environments have no suggestions. Each text was checked with the embedding model to
 * select the matching sections (see demo-content/README.md).
 */
@RestController
@ConditionalOnProperty(name = "kg.dev.seed.enabled", havingValue = "true")
@Profile("local")
@Tag(name = "Dev", description = "Development helpers for the seeded demo content")
public class DevFocusSuggestionsController {

	/** Topic id (from the dev seed) to its focus suggestions. */
	static final Map<Long, List<String>> SUGGESTIONS = Map.of(
			// Programming › System Design
			3000L, List.of("Reliability: message queues, retries, rate limiting and circuit breakers",
					"Distributed data: replication, sharding, consistent hashing and the CAP theorem",
					"Scaling a web system: load balancing, caching and stateless services",
					"Caching strategies, eviction and invalidation", "CAP theorem, consistency models and quorums"),
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
			11000L, List.of("Privacy and data security: personal data, GDPR and confidential information",
					"Risks and limitations: hallucinations, errors, deepfakes and copyright",
					"How AI tools work: language models, tokens, prompts and training",
					"Deepfakes, voice cloning scams and labelling AI-generated media"),
			// English Lessons (B1–B2)
			12000L, List.of("Common mistakes and their corrections", "Useful phrases for everyday situations",
					"Present perfect and past simple",
					"Modal verbs: must, have to, should, mustn't and don't have to",
					"First and second conditional with if", "The passive voice"));

	@GetMapping("/api/dev/focus-suggestions")
	@Operation(summary = "Focus suggestions for the demo topics", description = "Topic id to example focus texts for FOCUSED tests. The client offers the suggestions of the selected topics, their parents and their sub-topics. Only available when the dev seed is enabled.")
	public ResponseEntity<Map<Long, List<String>>> getFocusSuggestions() {
		return ResponseEntity.ok(SUGGESTIONS);
	}
}
